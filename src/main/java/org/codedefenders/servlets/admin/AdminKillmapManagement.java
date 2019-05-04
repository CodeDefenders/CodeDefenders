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
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.execution.KillMap.KillMapJob;
import org.codedefenders.execution.KillMap.KillMapJob.Type;
import org.codedefenders.execution.KillMapProcessor;
import org.codedefenders.util.Constants;
import org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME;
import org.codedefenders.servlets.admin.AdminSystemSettings.SettingsDTO;
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

public class AdminKillmapManagement extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminKillmapManagement.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.getRequestDispatcher(Constants.ADMIN_KILLMAPS_JSP).forward(request, response);
    }

    // TODO: show number of queued / canceled / deleted killmaps in messages

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // TODO This shall be a call done by an admin user.
        // But I guess there's no way to check this...

        /* Handle parameter "formType" */
        String formType = request.getParameter("formType");
        if (formType == null) {
            addMessage(request.getSession(), "Invalid request. Missing form type.");
            request.getRequestDispatcher(Constants.ADMIN_KILLMAPS_JSP).forward(request, response);
            return;
        }

        switch (formType) {

            case "toggleKillMapProcessing":
                /* Handle parameter "enable" */
                String enableString = request.getParameter("enable");
                if (enableString == null) {
                    addMessage(request.getSession(), "Invalid request. Missing parameter \"enable\".");
                    break;
                }
                if (enableString.equals("true")) {
                    toggleExecution(request, true);
                } else if (enableString.equals("false")) {
                    toggleExecution(request, false);
                } else {
                    addMessage(request.getSession(), "Invalid request. Invalid parameter \"enable\".");
                }
                break;

            case "submitKillMapJobs":
            case "cancelKillMapJobs":
            case "deleteKillMaps":
                /* Handle parameter "killmapType" */
                String killmapTypeString = request.getParameter("killmapType");
                Type killmapType;
                if (killmapTypeString == null) {
                    addMessage(request.getSession(), "Invalid request. Missing job type.");
                    break;
                }
                try {
                    killmapType = Type.valueOf(killmapTypeString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    addMessage(request.getSession(), "Invalid request. Invalid job type.");
                    break;
                }

                /* Handle parameter "ids" */
                String idsString = request.getParameter("ids");
                List<Integer> ids;
                if (idsString == null) {
                    addMessage(request.getSession(), "Invalid request. Missing IDs.");
                    break;
                }
                try {
                    /* Convert comma-separated list into JSON array if necessary. */
                    idsString = idsString.trim();
                    if (idsString.length() == 0 || idsString.charAt(0) != '[') {
                        idsString = "[" + idsString + "]";
                    }
                    Gson gson = new Gson();
                    ids = gson.fromJson(idsString, new TypeToken<List<Integer>>(){}.getType());
                } catch (JsonSyntaxException e) {
                    addMessage(request.getSession(), "Invalid request. Invalid IDs.");
                    break;
                }

                switch (formType) {
                    case "submitKillMapJobs":
                        submitKillMapJobs(request, killmapType, ids);
                        break;
                    case "cancelKillMapJobs":
                        cancelKillMapJobs(request, killmapType, ids);
                        break;
                    case "deleteKillMaps":
                        deleteKillMaps(request, killmapType, ids);
                        break;
                }
                break;

            default:
                addMessage(request.getSession(), "Invalid request. Invalid form type.");
        }

        request.getRequestDispatcher(Constants.ADMIN_KILLMAPS_JSP).forward(request, response);
    }

    private void submitKillMapJobs(HttpServletRequest request, Type killmapType, List<Integer> ids) {
        /* Check if classes or games exist for the given ids. */
        List<Integer> existingIds;
        if (killmapType == Type.CLASS) {
            existingIds = GameClassDAO.filterExistingClassIDs(ids);
        } else {
            existingIds = GameDAO.filterExistingGameIDs(ids);
        }

        /* If all classes / games exist, queue the jobs. */
        if (existingIds.size() == ids.size()) {
            for (int id : ids) {
                if (!KillmapDAO.enqueueJob(new KillMapJob(killmapType, id))) {
                    addMessage(request.getSession(), "Error while queueing selected killmap.");
                    return;
                }
            }

            addMessage(request.getSession(), "Successfully queued "
                    + ids.size() + " " + pluralize(ids.size(), "killmap", "s") + ".");

        /* Otherwise, construct an error message with the missing ids. */
        } else {
            Set<Integer> existingIdsSet = new TreeSet<>(existingIds);
            String missingIds = ids.stream()
                    .filter(id -> !existingIdsSet.contains(id))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));

            int count = ids.size() - existingIds.size();
            boolean plural = count == 0 || count > 1;
            String typeString;
            if (killmapType == Type.CLASS) {
                typeString = plural ? "classes" : "class";
            } else {
                typeString = plural ? "games" : "game";
            }
            String idString = plural ? "IDs" : "ID";

            addMessage(request.getSession(),
                    "Invalid request. No " + typeString + " for " + idString + " " + missingIds + " exist.");
        }
    }

    private void cancelKillMapJobs(HttpServletRequest request, Type killmapType, List<Integer> ids) {
        // TODO cancel current killmap computation if necessary
        if (KillmapDAO.removeKillmapJobsByIds(killmapType, ids)) {
            addMessage(request.getSession(), "Successfully canceled "
                    + ids.size() + " " + pluralize(ids.size(), "job", "s") + ".");
        } else {
            addMessage(request.getSession(), "Error while canceling selected jobs.");
        }
    }

    private void deleteKillMaps(HttpServletRequest request, Type killmapType, List<Integer> ids) {
        if (ids.isEmpty() || KillmapDAO.removeKillmapsByIds(killmapType, ids)) {
            addMessage(request.getSession(), "Successfully deleted "
                    + ids.size() + " " + pluralize(ids.size(), "killmap", "s") + ".");
        } else {
            addMessage(request.getSession(), "Error while deleting selected killmaps.");
        }
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
            if (AdminDAO.updateSystemSetting(new SettingsDTO(SETTING_NAME.AUTOMATIC_KILLMAP_COMPUTATION, true))) {
                addMessage(request.getSession(), "Successfully enabled killmap processing.");
            } else {
                addMessage(request.getSession(), "Error while enabling killmap processing.");
            }
        } else {
            logger.info("User {} disabled Killmap Processor", currentUserID);
            killMapProcessor.setEnabled(false);
            if (AdminDAO.updateSystemSetting(new SettingsDTO(SETTING_NAME.AUTOMATIC_KILLMAP_COMPUTATION, false))) {
                addMessage(request.getSession(), "Successfully disabled killmap processing.");
            } else {
                addMessage(request.getSession(), "Error while disabling killmap processing.");
            }
        }
    }

    // TODO: Provide this method for other classes -> dependency injection
    private void addMessage(HttpSession session, String message) {
        @SuppressWarnings("unchecked")
        List<String> messages = (List<String>) session.getAttribute("messages");
        if (messages == null) {
            messages = new ArrayList<>();
            session.setAttribute("messages", messages);
        }
        messages.add(message);
    }

    private String pluralize(int amount, String word, String suffix) {
        if (amount == 0 || amount > 1) {
            return word + suffix;
        } else {
            return word;
        }
    }
}