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
package org.codedefenders.servlets.registration;

import java.io.IOException;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.service.UserService;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;


@WebServlet(Paths.USER)
public class UserServlet extends HttpServlet {

    @Inject
    private MessagesBean messages;

    @Inject
    private UserService userService;

    @Inject
    private URLUtils url;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("create".equals(request.getParameter("formType"))) {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            String confirm = request.getParameter("confirm");
            String email = request.getParameter("email");

            if (!password.equals(confirm)) {
                // This check should be performed in the user interface too.
                messages.add("Could not create user. Password entries did not match.");
            } else {

                Optional<String> result = userService.registerUser(username, password, email);
                if (result.isEmpty()) {
                    messages.add("Your user has been created. You can login now.");
                } else {
                    messages.add(result.get());
                }
            }
            response.sendRedirect(url.forPath(Paths.LOGIN));
        } else { // Anything different from "create" is an error, so we not allow it
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
