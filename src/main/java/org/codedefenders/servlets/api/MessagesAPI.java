/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
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
import org.codedefenders.service.I18nService;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.xnap.commons.i18n.I18n;

import com.google.gson.Gson;

@WebServlet(Paths.API_MESSAGES)
public class MessagesAPI extends HttpServlet {
    Logger logger = org.slf4j.LoggerFactory.getLogger(MessagesAPI.class);

    @Inject
    MessagesBean messagesBean;

    @Inject
    I18nService i18nService;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Handle GET request
        logger.debug("GET request to MessagesAPI");
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        String notificationMessages = getNotificationMessages(request);
        response.setStatus(HttpServletResponse.SC_OK);
        out.print(notificationMessages);
        out.flush();
    }

    /**
     * Retrieves the notification messages from the MessagesBean,
     * filters out alert messages,
     * translates the messages,
     * converts them to JSON,
     * and clears the messages from the bean.
     *
     * @param request the servlet request, used to retrieve the locale for translation
     * @return a JSON string representing the list of notification messages
     */
    private String getNotificationMessages(HttpServletRequest request) {
        I18n i18n = i18nService.getI18n(request);
        List<Message> messagesList = messagesBean.getMessages();
        Gson gson = new Gson();
        List<Message> notificationMessages = messagesList.stream()
                .filter(message -> !message.isAlert())
                .map(message -> translate(message, i18n))
                .toList();
        messagesBean.clear();
        return gson.toJson(notificationMessages);
    }

    private Message translate(Message message, I18n i18n) {
        return message
                .setTitle(I18nService.safeTr(i18n, message.getTitle()))
                .setSecondary(I18nService.safeTr(i18n, message.getSecondary()))
                .setText(I18nService.safeTr(i18n, message.getText(), message.getArgs()));
    }

}
