package com.xfrgq.attendanceserver;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import org.json.JSONObject;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

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
        String password = jsonRequest.optString("password");

        JSONObject jsonResponse = new JSONObject();

        try (ResultSet resultSet = DatabaseHelper.getUserByUsernameAndPassword(username, password)) {
            if (resultSet.next()) {
                jsonResponse.put("status", "success");
                jsonResponse.put("message", "Login successful");
                jsonResponse.put("username", resultSet.getString("username"));
                jsonResponse.put("profile", resultSet.getString("profile"));
            } else {
                jsonResponse.put("status", "failure");
                jsonResponse.put("message", "Invalid username or password");
            }
        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "Server error");
        }

        try (PrintWriter out = response.getWriter()) {
            out.print(jsonResponse.toString());
        }
    }
}

