package com.xfrgq.attendanceserver;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/getCheckInItems")
public class GetCheckInItemsServlet extends HttpServlet {

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
            ResultSet resultSet = DatabaseHelper.getCheckInItems(connection, username);

            JSONArray itemsArray = new JSONArray();
            while (resultSet.next()) {
                JSONObject item = new JSONObject();
                item.put("name", resultSet.getString("name"));
                item.put("description", resultSet.getString("description"));
                item.put("deadline", resultSet.getString("deadline"));
                itemsArray.put(item);
            }

            jsonResponse.put("status", "success");
            jsonResponse.put("items", itemsArray);
        } catch (SQLException e) {
            e.printStackTrace();
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "Database error: " + e.getMessage());
        }

        try (PrintWriter out = response.getWriter()) {
            out.print(jsonResponse.toString());
        }
    }
}