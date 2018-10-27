/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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
package org.codedefenders.game.singleplayer;

import org.codedefenders.execution.AntRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AiPreparer extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AntRunner.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect(request.getContextPath()+"/games/upload");
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession();
        ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        switch (request.getParameter("formType")) {
            case "runPrepareAi":
                int cutId = Integer.parseInt(request.getParameter("cutID"));
                System.out.println("Running PrepareAI on class " + cutId);
                if(!PrepareAI.createTestsAndMutants(cutId)) {
                    messages.add("Preparation of AI for the class failed, please prepare the class again, or try a different class.");
                }
                break;
            default:
                break;
        }

        response.sendRedirect(request.getContextPath()+"/games/upload");
    }
}
