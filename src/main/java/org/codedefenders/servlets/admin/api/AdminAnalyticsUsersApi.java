package org.codedefenders.servlets.admin.api;

import com.google.gson.Gson;
import org.codedefenders.api.analytics.UserDataDTO;
import org.codedefenders.database.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class AdminAnalyticsUsersApi extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminAnalyticsUsersApi.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        List<UserDataDTO> users = DatabaseAccess.getAnalyticsUserData();

        Gson gson = new Gson();
        out.print(gson.toJson(users));
        out.flush();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException { }
}