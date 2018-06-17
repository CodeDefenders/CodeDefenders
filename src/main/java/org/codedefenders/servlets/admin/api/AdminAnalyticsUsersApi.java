package org.codedefenders.servlets.admin.api;

import com.google.gson.Gson;
import org.codedefenders.database.AdminDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

public class AdminAnalyticsUsersApi extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminAnalyticsUsersApi.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        List<UserDataDto> users = new LinkedList<>();
        List<List<String>> unassignedUsersInfo = AdminDAO.getAllUsersInfo();

        for (List<String> userInfo : unassignedUsersInfo) {
            UserDataDto user = new UserDataDto();

            user.uid = Integer.valueOf(userInfo.get(0));
            user.username = userInfo.get(1);
            user.email = userInfo.get(2);
            user.lastLogin = userInfo.get(3);
            user.totalScore = userInfo.get(5);

            users.add(user);
        }

        Gson gson = new Gson();
        out.print(gson.toJson(users));
        out.flush();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException { }

    private static class UserDataDto {
        private int uid;
        private String username;
        private String email;
        private String lastLogin;
        private String totalScore;
    }

}