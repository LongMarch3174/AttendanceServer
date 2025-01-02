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
import java.sql.PreparedStatement;
import org.json.JSONObject;

@WebServlet("/updateUserInfo")
public class UpdateUserInfoServlet extends HttpServlet {

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
        String email = jsonRequest.optString("email");

        JSONObject jsonResponse = new JSONObject();

        if (username.isEmpty() || email.isEmpty()) {
            jsonResponse.put("status", "failure");
            jsonResponse.put("message", "用户名或邮箱不能为空");
        } else {
            try (Connection connection = DatabaseHelper.getConnection()) {
                int rowsUpdated = DatabaseHelper.updateUserInfo(connection, username, email);
                if (rowsUpdated > 0) {
                    jsonResponse.put("status", "success");
                    jsonResponse.put("message", "用户信息更新成功");
                } else {
                    jsonResponse.put("status", "failure");
                    jsonResponse.put("message", "用户信息更新失败");
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
