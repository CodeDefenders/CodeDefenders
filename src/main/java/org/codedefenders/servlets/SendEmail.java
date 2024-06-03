/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.servlets;

import java.io.IOException;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.util.EmailUtils;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This {@link HttpServlet} handles requests to send mails. Mails are sent
 * using {@link EmailUtils#sendEmailToSelf(String, String, String)}.
 *
 * <p>Serves on path: {@code /api/sendmail}.
 */
@WebServlet(Paths.API_SEND_EMAIL)
public class SendEmail extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(SendEmail.class);

    @Inject
    private MessagesBean messages;

    @Inject
    private URLUtils url;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String name = request.getParameter("name");
        final String email = request.getParameter("email");
        final String subject = request.getParameter("subject");
        if (name == null || email == null || subject == null) {
            logger.warn("Request parameters are missing. Aborting sending mail.");
            response.setStatus(400);
            return;
        }

        final String message = String.format("From: %s <%s>\n\n%s", name, email, request.getParameter("message"));

        if (EmailUtils.sendEmailToSelf(subject, message, email)) {
            logger.debug("Successfully sent email to {}", email);
            messages.add("Thanks for your message, we'll get back to you soon! --The Code Defenders Team");
        } else {
            logger.warn("Sending email to {} failed.", email);
            messages.add("Sorry! There was an error when trying to send the message.");
        }

        response.sendRedirect(url.forPath(Paths.CONTACT_PAGE));
    }
}
