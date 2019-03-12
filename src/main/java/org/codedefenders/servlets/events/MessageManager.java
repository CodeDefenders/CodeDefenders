/**
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

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.UserDAO;
import org.codedefenders.game.Role;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This {@link HttpServlet} handles uploading and storing new chat messages. Chat
 * messages are stored as {@link Event Events}.
 * <p>
 * Serves on path: {@code /api/messages}.
 * @see org.codedefenders.util.Paths#API_MESSAGES
 */
public class MessageManager extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(MessageManager.class);

    /**
     * Checks for a given request whether it holds the required parameters.
     * Required parameters are {@code gameId}, {@code message} and {@code target}.
     *
     * @param request the given request as a {@link HttpServletRequest}.
     * @return {@code true} if request has required parameters, {@code false} if not.
     */
    private boolean hasParameters(HttpServletRequest request) {
        final String gameId = request.getParameter("gameId");
        final String message = request.getParameter("message");
        final String target = request.getParameter("target");

        return gameId != null && message != null && target != null;
    }

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
        if (!hasParameters(request)) {
            logger.debug("Missing required request parameters.");
            response.setStatus(400);
            out.print("{'status':'Error'}");
            out.flush();
            return;
        }
        int gameId = Integer.parseInt(request.getParameter("gameId"));
        int userId = (int) request.getSession().getAttribute("uid");

        final Role role = DatabaseAccess.getRole(userId, gameId);

        EventType eventType = EventType.valueOf(request.getParameter("target"));

        if (eventType == EventType.GAME_MESSAGE
                || (eventType == EventType.ATTACKER_MESSAGE && role == Role.ATTACKER)
                || (eventType == EventType.DEFENDER_MESSAGE && role == Role.DEFENDER)) {
            if (eventType == EventType.GAME_MESSAGE) {
                if (role == Role.DEFENDER) {
                    eventType = EventType.GAME_MESSAGE_DEFENDER;
                } else if (role == Role.ATTACKER) {
                    eventType = EventType.GAME_MESSAGE_ATTACKER;
                }
            }
            final EventStatus status = EventStatus.GAME;
            final User user = UserDAO.getUserById(userId);
            final String chatMessage = request.getParameter("message");
            final String message = user.getUsername() + ": " + chatMessage;
            final Timestamp timestamp = new Timestamp(System.currentTimeMillis());

            final Event e = new Event(0, gameId, userId, message, eventType, status, timestamp);
            e.setChatMessage(DatabaseAccess.sanitise(chatMessage));

            if (e.insert()) {
                logger.debug("Event {} saved in game {}.", eventType, gameId);
                out.print("{'status':'Success'}");
                out.flush();
            } else {
                logger.error("Error saving event {} in game {}", eventType, gameId);
                out.print("{'status':'Error'}");
                out.flush();
            }
        }
	}
}