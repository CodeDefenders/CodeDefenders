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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.cron.KillMapCronJob;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.execution.KillMap.KillMapJob;
import org.codedefenders.execution.KillMap.KillMapType;
import org.codedefenders.model.Classroom;
import org.codedefenders.service.ClassroomService;
import org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME;
import org.codedefenders.servlets.admin.AdminSystemSettings.SettingsDTO;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import static org.codedefenders.util.MessageUtils.pluralize;

/**
 * Handles toggling killmap processing, queueing killmaps for computation, deleting killmaps and cancelling queued
 * killmap jobs.
 *
 * <p></p>
 * The killmap computation page consists of three pages that are accessed via the GET parameter "page".
 * <ul>
 *      <li>manual: enter ids manually to queue or delete killmaps</li>
 *      <li>available: choose killmaps to queue or delete from a table of available killmaps</li>
 *      <li>queue: choose killmap jobs to cancel from a table of current killmap jobs</li>
 * </ul>
 *
 * <p></p>
 * The POST parameters for the servlet are
 * <ul>
 *      <li>page: the current page, used for redirection<br>
 *          "manual", "available", "queue"</li>
 *      <li>formType: the action of the submitted form<br>
 *          "toggleKillMapProcessing", "submitKillMapJobs", "cancelKillMapJobs", "deleteKillMaps"</li>
 *      <li>enable: enable or disable killmap processing<br>
 *          "true", "false"</li>
 *      <li>killmapType: type of killmaps to queue delete or cancel<br>
 *          "class", "game"</li>
 *      <li>ids: either a comma separated list or a JSON array of class/game ids</li>
 * </ul>
 */
@WebServlet(Paths.ADMIN_KILLMAPS + "/*")
public class AdminKillmapManagement extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminKillmapManagement.class);

    @Inject
    private MessagesBean messages;

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private KillMapCronJob killMapCronJob;

    @Inject
    private URLUtils url;

    @Inject
    private ClassroomService classroomService;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (setPage(request) == null) {
            response.sendRedirect(url.forPath(Paths.ADMIN_KILLMAPS));
            return;
        }

        request.getRequestDispatcher(Constants.ADMIN_KILLMAPS_JSP).forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // TODO: This shall be a call done by an admin user but I guess there's no way to check this...

        KillmapPage page = setPage(request);
        if (page == null || page == KillmapPage.NONE) {
            messages.add("Invalid request. Invalid URL.");
            response.sendRedirect(url.forPath(Paths.ADMIN_KILLMAPS));
            return;
        }

        /* Handle parameter "formType" */
        String formType = request.getParameter("formType");
        if (formType == null) {
            messages.add("Invalid request. Missing form type.");
            request.getRequestDispatcher(Constants.ADMIN_KILLMAPS_JSP).forward(request, response);
            return;
        }

        switch (formType) {

            case "toggleKillMapProcessing":
                /* Handle parameter "enable" */
                String enableString = request.getParameter("enable");
                if (enableString == null) {
                    messages.add("Invalid request. Missing parameter \"enable\".");
                    break;
                }
                if (enableString.equals("true")) {
                    toggleProcessing(true);
                } else if (enableString.equals("false")) {
                    toggleProcessing(false);
                } else {
                    messages.add("Invalid request. Invalid parameter \"enable\".");
                }
                break;

            case "submitKillMapJobs":
            case "cancelKillMapJobs":
            case "deleteKillMaps":
                /* Handle parameter "killmapType" */
                String killmapTypeString = request.getParameter("killmapType");
                KillMapType killmapType;
                if (killmapTypeString == null) {
                    messages.add("Invalid request. Missing job type.");
                    break;
                }
                try {
                    killmapType = KillMapType.valueOf(killmapTypeString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    messages.add("Invalid request. Invalid job type.");
                    break;
                }

                /* Handle parameter "ids" */
                String idsString = request.getParameter("ids");
                List<Integer> ids;
                if (idsString == null) {
                    messages.add("Invalid request. Missing IDs.");
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
                    messages.add("Invalid request. Invalid IDs.");
                    break;
                }

                switch (formType) {
                    case "submitKillMapJobs":
                        submitKillMapJobs(killmapType, ids);
                        break;
                    case "cancelKillMapJobs":
                        cancelKillMapJobs(killmapType, ids);
                        break;
                    case "deleteKillMaps":
                        deleteKillMaps(killmapType, ids);
                        break;
                    default:
                        // ignored
                }
                break;

            default:
                messages.add("Invalid request. Invalid form type.");
        }

        /* Use PRG (post redirect get) to prevent erroneous killmap job submissions. */
        response.sendRedirect(request.getRequestURI());
    }

    private void submitKillMapJobs(KillMapType killmapType, Collection<Integer> ids) {
        ids = new HashSet<>(ids);

        /* Check if classes or games exist for the given ids. */
        List<Integer> existingIds;
        switch (killmapType) {
            case CLASS:
                existingIds = GameClassDAO.filterExistingClassIDs(ids);
                break;
            case GAME:
                existingIds = GameDAO.filterExistingGameIDs(ids);
                break;
            case CLASSROOM:
                existingIds = ids.stream()
                        .map(classroomService::getClassroomById)
                        .flatMap(Optional::stream)
                        .map(Classroom::getId)
                        .collect(Collectors.toList());
                break;
            default:
                logger.error("Unknown killmap type: " + killmapType);
                return;
        }

        /* If all classes / games exist, queue the jobs. */
        if (existingIds.size() == ids.size()) {
            List<Integer> successfulIds = new LinkedList<>();
            for (int id : ids) {
                if (!KillmapDAO.enqueueJob(new KillMapJob(killmapType, id))) {
                    logger.warn("Failed to queue killmap for {}: {}", killmapType, StringUtils.join(ids));
                    messages.add(String.format("Failed to queue selected %s.",
                            pluralize(ids.size(), "killmap", "killmaps")));
                } else {
                    successfulIds.add(id);
                }
            }

            logger.info("User {} queued killmaps for {}: {}",
                    login.getUserId(), killmapType, StringUtils.join(successfulIds));
            messages.add(String.format("Successfully queued %d %s.",
                    successfulIds.size(), pluralize(successfulIds.size(), "killmap", "killmaps")));

        /* Otherwise, construct an error message with the missing ids. */
        } else {
            Set<Integer> existingIdsSet = new TreeSet<>(existingIds);
            String missingIds = ids.stream()
                    .filter(id -> !existingIdsSet.contains(id))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));

            int count = ids.size() - existingIds.size();
            String type;
            switch (killmapType) {
                case CLASS:
                    type = pluralize(count, "class", "classes");
                    break;
                case GAME:
                    type = pluralize(count, "game", "games");
                    break;
                case CLASSROOM:
                    type = pluralize(count, "classroom", "classrooms");
                    break;
                default:
                    logger.error("Unknown killmap type: " + killmapType);
                    return;
            }

            messages.add(String.format("Invalid request. No %s for %s %s exist. No killmaps were queued.",
                    type, pluralize(count, "ID", "IDs"), missingIds));
        }
    }

    // TODO: cancel current killmap computation if necessary?
    private void cancelKillMapJobs(KillMapType killmapType, List<Integer> ids) {
        if (KillmapDAO.removeKillmapJobsByIds(killmapType, ids)) {
            logger.info("User {} canceled killmap jobs for {}: {}",
                    login.getUserId(), killmapType, StringUtils.join(ids, ", "));
            messages.add(String.format("Successfully canceled %d %s.",
                    ids.size(), pluralize(ids.size(), "job", "jobs")));
        } else {
            logger.warn("Failed to cancel killmap jobs: " + StringUtils.join(ids, ", "));
            messages.add("Failed to cancel selected jobs.");
        }
    }

    private void deleteKillMaps(KillMapType killmapType, List<Integer> ids) {
        /* Don't check the return value of removeKillmapsByIds,
         * because zero rows could be deleted which is not an error. */
        KillmapDAO.removeKillmapsByIds(killmapType, ids);
        logger.info("User {} deleted killmaps for {}: {}",
                login.getUserId(), killmapType, StringUtils.join(ids, ", "));
        messages.add(String.format("Successfully deleted %d %s.",
                ids.size(), pluralize(ids.size(), "killmap", "killmaps")));

        /* logger.warn("Failed to delete killmaps: {}", StringUtils.join(ids, ", "));
        addMessage(request.getSession(), "Failed to delete selected killmaps."); */
    }

    private void toggleProcessing(boolean enable) {
        if (enable) {
            killMapCronJob.setEnabled(true);
            if (AdminDAO.updateSystemSetting(new SettingsDTO(SETTING_NAME.AUTOMATIC_KILLMAP_COMPUTATION, true))) {
                logger.info("User {} enabled killmap processing", login.getUserId());
                messages.add("Successfully enabled killmap processing.");
            } else {
                logger.warn("Failed to enable killmap processing");
                messages.add("Failed to enable killmap processing");
            }
        } else {
            killMapCronJob.setEnabled(false);
            if (AdminDAO.updateSystemSetting(new SettingsDTO(SETTING_NAME.AUTOMATIC_KILLMAP_COMPUTATION, false))) {
                logger.info("User {} disabled killmap processing", login.getUserId());
                messages.add("Successfully disabled killmap processing.");
            } else {
                logger.warn("Failed to disable killmap processing");
                messages.add("Failed to disable killmap processing");
            }
        }
    }

    /**
     * Sets the page attribute in the request according to the path info in the URL.
     * @return The page according to the path info.
     */
    private KillmapPage setPage(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            request.setAttribute("page", null);
            return KillmapPage.NONE;
        }

        pathInfo = pathInfo.substring(1).toUpperCase();

        try {
            KillmapPage page = KillmapPage.valueOf(pathInfo);
            request.setAttribute("page", page);
            return page;

        } catch (IllegalArgumentException e) {
            request.setAttribute("page", null);
            return null;
        }
    }

    public enum KillmapPage {
        NONE,
        MANUAL,
        AVAILABLE,
        QUEUE
    }
}
