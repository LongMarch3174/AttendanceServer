package com.xfrgq.attendanceserver;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/getAttendanceRecords")
public class AttendanceRecordsServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        JSONObject jsonRequest = new JSONObject(sb.toString());
        String username = jsonRequest.optString("username");

        JSONObject jsonResponse = new JSONObject();

        try (Connection connection = DatabaseHelper.getConnection()) {
            // 获取 user_id
            int userId = DatabaseHelper.getUserIdByUsername(connection, username);
            if (userId == -1) {
                jsonResponse.put("status", "failure");
                jsonResponse.put("message", "用户不存在");
            } else {
                // 获取考勤记录
                JSONArray records = DatabaseHelper.getAttendanceRecords(connection, userId);
                jsonResponse.put("status", "success");
                jsonResponse.put("records", records);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "服务器错误");
        }

        try (PrintWriter out = response.getWriter()) {
            out.print(jsonResponse.toString());
        }
    }
}
