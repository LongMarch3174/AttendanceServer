package com.xfrgq.attendanceserver;

import java.sql.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class DatabaseHelper {

    private static final String DB_URL = "jdbc:mysql://*.*.*.*:****/attendance_db";
    private static final String DB_USER = "attendance";
    private static final String DB_PASSWORD = "********";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static boolean registerUser(String username, String password) throws SQLException {
        try (Connection connection = getConnection()) {
            // 检查用户名是否已存在
            String checkQuery = "SELECT id FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        return false; // 用户名已存在
                    }
                }
            }

            // 插入新用户
            String insertUserQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertUserQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                int affectedRows = insertStmt.executeUpdate();

                if (affectedRows > 0) {
                    // 获取生成的 user_id
                    try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int userId = generatedKeys.getInt(1);

                            // 插入用户统计信息
                            return initializeStatistics(connection, userId);
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean initializeStatistics(Connection connection, int userId) throws SQLException {
        String initStatsQuery = "INSERT INTO attendance_statistics (user_id, present_count, absent_count) VALUES (?, 0, 0)";
        try (PreparedStatement initStmt = connection.prepareStatement(initStatsQuery)) {
            initStmt.setInt(1, userId);
            return initStmt.executeUpdate() > 0;
        }
    }

    public static ResultSet getUserByUsernameAndPassword(String username, String password) throws SQLException {
        Connection connection = getConnection();
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, username);
        statement.setString(2, password);
        return statement.executeQuery();
    }

    public static ResultSet getUserInfoByUsername(String username) throws SQLException {
        Connection connection = getConnection();
        String query = "SELECT username, profile FROM users WHERE username = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, username);
        return statement.executeQuery();
    }

    public static int updateUserInfo(Connection connection, String username, String email) throws SQLException {
        String query = "UPDATE users SET profile = ? WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email); // 假设 profile 字段存储邮箱信息
            statement.setString(2, username);
            return statement.executeUpdate();
        }
    }

    public static ResultSet getCheckInItems(Connection connection, String username) throws SQLException {
        // SQL 查询
        String query = "SELECT c.name, c.description, c.deadline " +
                "FROM check_in_items c " +
                "WHERE c.deadline > NOW() " +
                "AND NOT EXISTS ( " +
                "    SELECT 1 FROM attendance_records ar " +
                "    JOIN users u ON ar.user_id = u.id " +
                "    WHERE ar.item_id = c.item_id AND u.username = ? " +
                ")";

        // 预编译语句
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, username); // 设置用户名参数
        return statement.executeQuery();
    }

    public static boolean recordCheckIn(Connection connection, String itemName, String location, String clientTime, String username) throws SQLException {
        // Step 1: Retrieve user ID based on username
        String userQuery = "SELECT id FROM users WHERE username = ?";
        int userId = -1;
        try (PreparedStatement userStatement = connection.prepareStatement(userQuery)) {
            userStatement.setString(1, username);
            ResultSet userResultSet = userStatement.executeQuery();
            if (userResultSet.next()) {
                userId = userResultSet.getInt("id");
            } else {
                // User not found, return false
                return false;
            }
        }

        // Step 2: Retrieve item details (item_id and deadline)
        String checkQuery = "SELECT item_id, deadline FROM check_in_items WHERE name = ?";
        try (PreparedStatement checkStatement = connection.prepareStatement(checkQuery)) {
            checkStatement.setString(1, itemName);
            ResultSet rs = checkStatement.executeQuery();

            if (rs.next()) {
                int itemId = rs.getInt("item_id");
                String deadline = rs.getString("deadline");

                // Step 3: Determine attendance status
                String attendanceStatus = clientTime.compareTo(deadline) <= 0 ? "On-Time" : "Late";

                // Step 4: Record check-in in attendance_records
                String insertQuery = "INSERT INTO attendance_records (user_id, item_id, clock_in_time, attendance_status, location) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                    insertStatement.setInt(1, userId);
                    insertStatement.setInt(2, itemId);
                    insertStatement.setString(3, clientTime);
                    insertStatement.setString(4, attendanceStatus);
                    insertStatement.setString(5, location);

                    int rowsInserted = insertStatement.executeUpdate();

                    // Step 5: Update attendance_statistics
                    if (rowsInserted > 0) {
                        updateAttendanceStatistics(connection, userId, attendanceStatus);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void updateAttendanceStatistics(Connection connection, int userId, String attendanceStatus) throws SQLException {
        String updateQuery = null;
        if ("On-Time".equals(attendanceStatus)) {
            // Increment present count
            updateQuery = "UPDATE attendance_statistics SET present_count = present_count + 1 WHERE user_id = ?";
        } else if("Late".equals(attendanceStatus)) {
            // Increment absent count
            updateQuery = "UPDATE attendance_statistics SET absent_count = absent_count + 1 WHERE user_id = ?";
        }
        try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
            updateStatement.setInt(1, userId);
            updateStatement.executeUpdate();
        }
    }

    // 根据用户名获取 user_id
    public static int getUserIdByUsername(Connection connection, String username) throws SQLException {
        String query = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            return -1;  // 用户名不存在
        }
    }

    // 获取考勤统计
    public static JSONObject getAttendanceStatistics(Connection connection, int userId) throws SQLException {
        String query = "SELECT present_count, absent_count FROM attendance_statistics WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            JSONObject stats = new JSONObject();
            if (rs.next()) {
                stats.put("present_count", rs.getInt("present_count"));
                stats.put("absent_count", rs.getInt("absent_count"));
            }
            return stats;
        }
    }

    // 获取考勤记录
    public static JSONArray getAttendanceRecords(Connection connection, int userId) throws SQLException {
        String query = "SELECT ar.clock_in_time, ar.location, c.name, c.description, c.created_at, c.deadline " +
                "FROM attendance_records ar " +
                "JOIN check_in_items c ON ar.item_id = c.item_id " +
                "WHERE ar.user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            JSONArray records = new JSONArray();
            while (rs.next()) {
                JSONObject record = new JSONObject();
                record.put("time", rs.getString("clock_in_time"));
                record.put("location", rs.getString("location"));
                record.put("name", rs.getString("name"));
                record.put("description", rs.getString("description"));
                record.put("created_at", rs.getString("created_at"));
                record.put("deadline", rs.getString("deadline"));
                records.put(record);
            }
            return records;
        }
    }

    public static void closeResources(Connection connection, PreparedStatement statement, ResultSet resultSet) {
        try {
            if (resultSet != null) resultSet.close();
            if (statement != null) statement.close();
            if (connection != null) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
