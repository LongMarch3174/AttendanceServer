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
import sun.rmi.runtime.Log;

@WebServlet("/fetchUserInfo")
public class FetchUserInfoServlet extends HttpServlet {

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

        if (username == null || username.isEmpty()) {
            jsonResponse.put("status", "failure");
            jsonResponse.put("message", "用户名为空或无效");
        } else {
            try (ResultSet resultSet = DatabaseHelper.getUserInfoByUsername(username)) {
                if (resultSet.next()) {
                    jsonResponse.put("status", "success");
                    jsonResponse.put("username", resultSet.getString("username"));
                    jsonResponse.put("email", resultSet.getString("profile")); // 假设 profile 包含 email 信息
                } else {
                    jsonResponse.put("status", "failure");
                    jsonResponse.put("message", "用户信息未找到");
                }
            } catch (Exception e) {
                e.printStackTrace();
                jsonResponse.put("status", "error");
                jsonResponse.put("message", "数据库错误: " + e.getMessage());
            }
        }

        try (PrintWriter out = response.getWriter()) {
            out.print(jsonResponse.toString());
        }
    }
}