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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.execution.KillMap.KillMapJob;
import org.codedefenders.execution.KillMap.KillMapJob.Type;
import org.codedefenders.execution.KillMapProcessor;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AdminKillMaps extends HttpServlet {
    private static final long serialVersionUID = 4237104046949774958L;

    private static final Logger logger = LoggerFactory.getLogger(AdminKillMaps.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.getRequestDispatcher(Constants.ADMIN_KILLMAPS_JSP).forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // TODO This shall be a call done by an admin user.
        // But I guess there's no way to check this...

        String formType = request.getParameter("formType");
        if (formType == null) {
            request.getRequestDispatcher(Constants.ADMIN_KILLMAPS_JSP).forward(request, response);
            return;
        }

        switch (formType) {

            case "toggleExecution":
                String enableString = request.getParameter("enable");
                if (enableString == null) {
                    addMessage(request.getSession(), "Invalid request. Missing parameter.");
                    break;
                }
                if (enableString.equals("true")) { // TODO: case sensitive or case insensitive
                    toggleExecution(request, true);
                } else if (enableString.equals("false")) {
                    toggleExecution(request, false);
                } else {
                    addMessage(request.getSession(), "Invalid request. Invalid parameter.");
                    break;
                }

                break;

            case "submitKillMapJob":
            case "cancelKillMapJob":
                /* Get the type of killmap: game or class. */
                String jobTypeString = request.getParameter("jobType");
                Type jobType;
                if (jobTypeString == null) {
                    addMessage(request.getSession(), "Invalid request. Missing job type.");
                    break;
                }
                try {
                    jobType = Type.valueOf(jobTypeString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    addMessage(request.getSession(), "Invalid request. Invalid job type.");
                    break;
                }

                /* Get the (class or game) ids. */
                String idsString = request.getParameter("ids");
                List<Integer> ids;
                if (idsString == null) {
                    addMessage(request.getSession(), "Invalid request. Missing IDs.");
                    break;
                }
                try {
                    Gson gson = new Gson();
                    ids = gson.fromJson(idsString, new TypeToken<List<Integer>>(){}.getType());
                } catch (JsonSyntaxException e) {
                    addMessage(request.getSession(), "Invalid request. Invalid IDs.");
                    break;
                }

                if (formType.equals("submitKillMapJob")) {
                    submitKillMapJobs(request, jobType, ids);
                } else {
                    cancelKillMapJobs(request, jobType, ids);
                }

                break;

            default:
                addMessage(request.getSession(), "Invalid request. Invalid form type.");
                break;
        }

        request.getRequestDispatcher(Constants.ADMIN_KILLMAPS_JSP).forward(request, response);
    }

    private void submitKillMapJobs(HttpServletRequest request, Type jobType, List<Integer> ids) {
        /* Check if classes or games exist for the given ids. */
        List<Integer> existingIds = null;
        if (jobType == Type.CLASS) {
            existingIds = GameClassDAO.filterExistingClassIDs(ids);
        } else if (jobType == Type.GAME) {
            existingIds = GameDAO.filterExistingGameIDs(ids);
        }
        if (existingIds.size() != ids.size()) {

            Set<Integer> existingIdsSet = new TreeSet<>(existingIds);
            String missingIds = ids.stream()
                    .filter(id -> !existingIdsSet.contains(id))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            addMessage(request.getSession(), "Invalid request. No games / classes for ID(s) " + missingIds + " exist.");
            return;
        }

        /* Enqueue the jobs. */
        for (int id : ids) {
            KillmapDAO.enqueueJob(new KillMapJob(jobType, id));
        }
    }

    private void cancelKillMapJobs(HttpServletRequest request, Type jobType, List<Integer> ids) {
        // TODO cancel current killmap computation if necessary
        KillmapDAO.removeJobsByIds(jobType, ids);
    }

    private void toggleExecution(HttpServletRequest request, boolean enable) {
        int currentUserID = (Integer) request.getSession().getAttribute("uid");
        ServletContext context = getServletContext();
        KillMapProcessor killMapProcessor = (KillMapProcessor) context.getAttribute(KillMapProcessor.NAME);

        List<String> messages = (List<String>) request.getSession().getAttribute("messages");
        if (messages == null) messages = new ArrayList<>();
        request.getSession().setAttribute("messages", messages);

        if (enable) {
            logger.info("User {} enabled Killmap Processor", currentUserID);
            killMapProcessor.setEnabled(true);
            (new AdminSystemSettings()).updateSystemSettings(request, messages);
        } else {
            logger.info("User {} disabled Killmap Processor", currentUserID);
            killMapProcessor.setEnabled(false);
            (new AdminSystemSettings()).updateSystemSettings(request, messages);
        }
    }

    // TODO: Provide this method for other classes -> dependency injection
    private void addMessage(HttpSession session, String message) {
        List<String> messages = (List<String>) session.getAttribute("messages");
        if (messages == null) {
            messages = new ArrayList<>();
            session.setAttribute("messages", messages);
        }
        messages.add(message);
    }
}