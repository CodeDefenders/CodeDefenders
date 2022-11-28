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
package org.codedefenders.servlets.admin.api;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.admin.AdminCreateGamesBean;
import org.codedefenders.database.GameDAO;
import org.codedefenders.dto.api.GameID;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Test;
import org.codedefenders.persistence.database.SettingsRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.util.APIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParseException;

/**
 * This {@link HttpServlet} offers an API for {@link Test tests}.
 *
 * <p>A {@code GET} request with the {@code testId} parameter results in a JSON string containing
 * test information, including the source code.
 *
 * <p>Serves on path: {@code /api/test}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet("/admin/api/game/start")
public class StartGameAPI extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(StartGameAPI.class);
    @Inject
    CodeDefendersAuth login;
    @Inject
    GameService gameService;
    @Inject
    SettingsRepository settingsRepository;
    @Inject
    UserRepository userRepository;
    @Inject
    UserService userService;
    @Inject
    AdminCreateGamesBean adminCreateGamesBean;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final GameID gameId;
        try {
            gameId = (GameID) APIUtils.parsePostOrRespondJsonError(request, response, GameID.class);
        } catch (JsonParseException e) {
            return;
        }
        AbstractGame game = GameDAO.getGame(gameId.getGameId());
        if (game == null) {
            APIUtils.respondJsonError(response, "Game with ID " + gameId.getGameId() + " not found", HttpServletResponse.SC_NOT_FOUND);
        } else if (login.getUserId() != game.getCreatorId()) {
            APIUtils.respondJsonError(response, "Only the game's creator can start the game", HttpServletResponse.SC_BAD_REQUEST);
        } else if (game.getState() != GameState.CREATED) {
            APIUtils.respondJsonError(response, "Game cannot be started since it has state " + game.getState(), HttpServletResponse.SC_BAD_REQUEST);
        } else {
            logger.info("Starting game {} (Setting state to ACTIVE)", gameId);
            game.setState(GameState.ACTIVE);
            game.update();
        }
/*
            Optional<UserEntity> user = userRepository.getUserById(userId);
            PrintWriter out = response.getWriter();
            if (user.isPresent()) {
                response.setContentType("application/json");
                Gson gson = new Gson();
                JsonObject root = new JsonObject();
                root.add("token", gson.toJsonTree(user.get().getToken(), String.class));
                out.print(new Gson().toJson(root));
                out.flush();
            } else {
                ServletUtils.respondJsonError(response, "User " + userId + " not found", HttpStatus.SC_NOT_FOUND);
            }
*/
    }
}
