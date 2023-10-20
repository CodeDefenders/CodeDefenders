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
package org.codedefenders.servlets.admin;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ValidationException;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.service.ClassroomService;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(Paths.ADMIN_CLASSROOMS)
public class AdminClassrooms extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminClassrooms.class);

    @Inject
    private ClassroomService classroomService;

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private MessagesBean messages;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.getRequestDispatcher("/jsp/admin_classrooms.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Optional<String> action = ServletUtils.getStringParameter(request, "action");
        if (action.isEmpty()) {
            messages.add("Missing required parameter: action.");
            Redirect.redirectBack(request, response);
            return;
        }

        try {
            switch (action.get()) {
                case "create-classroom":
                    createClassroom(request, response);
                    break;
                default:
                    messages.add("Invalid action: " + action);
                    Redirect.redirectBack(request, response);
            }
        } catch (ValidationException e) {
            messages.add("Validation failed: " + e.getMessage());
            Redirect.redirectBack(request, response);
        } catch (NoSuchElementException e) {
            messages.add("Missing or invalid parameter.");
            Redirect.redirectBack(request, response);
        }
    }

    private void createClassroom(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String name = ServletUtils.getStringParameter(request, "name").get();
        classroomService.addClassroom(name);
        Redirect.redirectBack(request, response);
    }
}
