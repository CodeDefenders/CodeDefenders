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
package org.codedefenders.servlets.api;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.game.Role;
import org.codedefenders.notification.events.server.chat.ServerGameChatEvent;
import org.codedefenders.persistence.database.GameChatRepository;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This {@link HttpServlet} offers an API for chat messages.
 * <br/>
 * <br/>
 * Chat messages are sent in the same way they are retrieved from {@link GameChatRepository#getChatMessages(int, Role, int)}.
 * <br/>
 * <br/>
 * Parameters:
 * <ul>
 *     <li>gameId</li>
 *     <li>limit (optional)</li>
 * </ul>
 */
@WebServlet(Paths.API_GAME_CHAT)
public class GameChatAPI extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(GameChatAPI.class);
    private static final int DEFAULT_LIMIT = 1000;

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private GameChatRepository gameChatRepo;

    @Inject
    private PlayerRepository playerRepo;

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        final Optional<Integer> gameIdOpt = ServletUtils.getIntParameter(request, "gameId");
        if (gameIdOpt.isEmpty()) {
            logger.warn("Missing parameter: gameId.");
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        final int limit = ServletUtils.getIntParameter(request, "limit").orElse(DEFAULT_LIMIT);
        final int gameId = gameIdOpt.get();

        final Role role = playerRepo.getRole(login.getUserId(), gameId);
        if (role == Role.NONE || role == null) {
            logger.warn("Requesting user is not part of game.");
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        List<ServerGameChatEvent> messages = gameChatRepo.getChatMessages(gameId, role, limit);

        response.setContentType("application/json");
        gson.toJson(messages, response.getWriter());
    }
}
