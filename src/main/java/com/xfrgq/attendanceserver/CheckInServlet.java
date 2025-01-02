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
import org.json.JSONObject;

@WebServlet("/checkIn")
public class CheckInServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        JSONObject jsonRequest = new JSONObject(sb.toString());
        String itemName = jsonRequest.optString("item_name");
        String location = jsonRequest.optString("location");
        String time = jsonRequest.optString("time");
        String username = jsonRequest.optString("username");

        JSONObject jsonResponse = new JSONObject();

        try (Connection connection = DatabaseHelper.getConnection()) {
            boolean isCheckedIn = DatabaseHelper.recordCheckIn(connection, itemName, location, time, username);

            if (isCheckedIn) {
                jsonResponse.put("status", "success");
                jsonResponse.put("message", "Check-in successful.");
            } else {
                jsonResponse.put("status", "failure");
                jsonResponse.put("message", "Check-in failed. Item not found or already checked in.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "Database error: " + e.getMessage());
        }

        try (PrintWriter out = response.getWriter()) {
            out.print(jsonResponse.toString());
        }
    }
}