package org.codedefenders.servlets.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.beans.message.Message;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;

import com.google.gson.Gson;

@WebServlet(Paths.API_MESSAGES)
public class MessagesAPI extends HttpServlet {
    Logger logger = org.slf4j.LoggerFactory.getLogger(MessagesAPI.class);

    @Inject
    MessagesBean messagesBean;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Handle GET request
        logger.debug("GET request to MessagesAPI");
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        String notificationMessages = getNotificationMessages();
        response.setStatus(HttpServletResponse.SC_OK);
        out.print(notificationMessages);
        out.flush();
    }

    private String getNotificationMessages() {
        List<Message> messagesList = messagesBean.getMessages();
        Gson gson = new Gson();
        List<Message> notificationMessages = messagesList.stream()
                .filter(message -> !message.isAlert())
                .toList();
        messagesBean.clear();
        return gson.toJson(notificationMessages);
    }
}
