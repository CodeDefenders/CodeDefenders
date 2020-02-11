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
package org.codedefenders.servlets.events;

import com.google.gson.Gson;

import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.UserDAO;
import org.codedefenders.game.Role;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This {@link HttpServlet} handles notification requests. Notifications are logged and
 * stored server side. Clients request their most recent notifications.
 *
 * <p>In this servlet, all {@link NotificationType NotificationTypes} are handled.
 *
 * <p>Serves on path: {@code /api/notifications}.
 *
 * @see org.codedefenders.util.Paths#API_NOTIFICATION
 */
@WebServlet("/api/notifications")
public class NotificationsHandler extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(NotificationsHandler.class);
    private static final Gson gson = new Gson();

    @Inject
    private LoginBean login;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!hasParameters(request)) {
            logger.debug("Missing required request parameters.");
            response.setStatus(400);
            return;
        }
        final HttpSession session = request.getSession();
        response.setContentType("application/json");

        final NotificationType type = NotificationType.valueOf(request.getParameter("type"));
        switch (type) {
            case PUSHEVENT:
                handlePushEventRequest(session, request, response);
                break;
            case GAMEEVENT:
                handleGameEventRequest(request, response);
                break;
            case USEREVENT:
                handleUserEventRequest(request, response);
                break;
            default:
                logger.error("Received request with wrong request type: " + type.name());
                response.setStatus(400);
                break;
        }
    }

    /**
     * Checks for a given request whether it holds the required parameters.
     *
     * @param request the given request as a {@link HttpServletRequest}.
     * @return {@code true} if request has required parameters, {@code false} otherwise.
     */
    private boolean hasParameters(HttpServletRequest request) {
        final String type = request.getParameter("type");
        return type != null;
    }

    /**
     * Handles a push event request, which requires the following URL parameters:
     * <ul>
     *     <li><code>gameId</code></li>
     * </ul>
     * If parameters are valid, responds with a JSON list of most recent {@link Event Events}.
     */
    @SuppressWarnings("Duplicates")
    private void handlePushEventRequest(HttpSession session, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        final String gameIdString = request.getParameter("gameId");
        if (gameIdString == null) {
            response.setStatus(400);
            logger.error("Push event: Missing parameter gameId.");
            return;
        }
        int gameId;
        try {
            gameId = Integer.parseInt(gameIdString);
        } catch (NumberFormatException e) {
            response.setStatus(400);
            logger.error("Push event: Error trying to format parameter gameId.", e);
            return;
        }

        final Object lastMsg1 = request.getSession().getAttribute("lastMsg");
        final int lastMessageId = lastMsg1 != null ? (Integer) lastMsg1 : 0;

        final ArrayList<Event> events =
                new ArrayList<>(DatabaseAccess.getNewEquivalenceDuelEventsForGame(gameId, lastMessageId));
        if (!events.isEmpty()) {
            int lastMsg = Collections.max(events, Event.MAX_ID_COMPARATOR).getId();
            session.setAttribute("lastMsg", lastMsg);
        }

        for (Event e : events) {
            e.setCurrentUserName(login.getUser().getUsername());
            e.parse(e.getEventStatus() == EventStatus.GAME);
        }

        PrintWriter out = response.getWriter();
        out.print(gson.toJson(events));
        out.flush();
    }

    /**
     * Handles a game event request, which requires the following URL parameters:
     * <ul>
     *     <li><code>gameId</code></li>
     *     <li><code>timestamp</code></li>
     * </ul>
     * If parameters are valid, responds with a JSON list of most recent game {@link Event Events}.
     */
    @SuppressWarnings("Duplicates")
    private void handleGameEventRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        final String timestampString = request.getParameter("timestamp");
        if (timestampString == null) {
            response.setStatus(400);
            logger.error("Game Event: Missing parameter timestamp.");
            return;
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampString);
        } catch (NumberFormatException e) {
            response.setStatus(400);
            logger.error("Game Event: Error trying to format timestamp.", e);
            return;
        }
        final String gameIdString = request.getParameter("gameId");
        if (gameIdString == null) {
            response.setStatus(400);
            logger.error("Game Event: Missing parameter gameId.");
            return;
        }
        int gameId;
        try {
            gameId = Integer.parseInt(gameIdString);
        } catch (NumberFormatException e) {
            response.setStatus(400);
            logger.error("Game Event: Error trying to format parameter gameId.", e);
            return;
        }

        final Role role = DatabaseAccess.getRole(login.getUserId(), gameId);
        final ArrayList<Event> events = new ArrayList<>(DatabaseAccess.getNewEventsForGame(gameId, timestamp, role));

        for (Event e : events) {
            e.setCurrentUserName(login.getUser().getUsername());
            e.parse(e.getEventStatus() == EventStatus.GAME);
        }

        PrintWriter out = response.getWriter();
        out.print(gson.toJson(events));
        out.flush();
    }

    /**
     * Handles a user event request, which requires the following URL parameters:
     * <ul>
     *     <li><code>timestamp</code></li>
     * </ul>
     * If parameters are valid, responds with a JSON list of most recent user {@link Event Events}.
     */
    @SuppressWarnings("Duplicates")
    private void handleUserEventRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        final String timestampString = request.getParameter("timestamp");
        if (timestampString == null) {
            response.setStatus(400);
            logger.error("User Event: Missing parameter timestamp.");
            return;
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampString);
        } catch (NumberFormatException e) {
            response.setStatus(400);
            logger.error("User Event: Error trying to format timestamp." + e );
            return;
        }

        try {
            // DatabaseAccess#getNewEventsForUser(int, long) never returns null, so no need
            // for extra check
            final List<Event> events = DatabaseAccess.getNewEventsForUser(login.getUserId(), timestamp).stream()
                    .peek(event -> event.setCurrentUserName(login.getUser().getUsername()))
                    .peek(event -> event.parse(event.getEventStatus() == EventStatus.GAME))
                    .collect(Collectors.toList());

            PrintWriter out = response.getWriter();
            out.print(gson.toJson(events));
            out.flush();
        } catch (NullPointerException e) {
            logger.info("" + DatabaseAccess.getNewEventsForUser(login.getUserId(), timestamp));
            logger.error("NPE: " + e);

        }

    }
}
