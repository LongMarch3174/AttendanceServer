package com.xfrgq.attendanceserver;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import org.json.JSONObject;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

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

        try {
            // 注册用户并更新统计表
            boolean isRegistered = DatabaseHelper.registerUser(username, password);

            if (isRegistered) {
                jsonResponse.put("status", "success");
                jsonResponse.put("message", "注册成功");
            } else {
                jsonResponse.put("status", "failure");
                jsonResponse.put("message", "用户名已存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "服务器错误: " + e.getMessage());
        }

        try (PrintWriter out = response.getWriter()) {
            out.print(jsonResponse.toString());
        }
    }
}
