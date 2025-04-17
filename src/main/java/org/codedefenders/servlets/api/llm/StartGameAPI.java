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
package org.codedefenders.servlets.api.llm;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.servlets.util.ServletUtils;

@WebServlet("/llm-api/battleground/start")
public class StartGameAPI extends APIServlet {
    @Inject
    protected GameService gameService;

    @Inject
    protected GameRepository gameRepo;

    @Inject
    protected GameClassRepository gameClassRepo;

    @Inject
    protected GameProducer gameProducer;

    @Inject
    protected CodeDefendersAuth login;

    @Override
    protected void doPost(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        final var gameId = ServletUtils.getIntParameter(request, "gameId");

        if (gameId.isEmpty()) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    new Common.ErrorResponseDTO("Parameter 'gameId' missing."));
            return;
        }

        gameProducer.setGameId(gameId.get());
        final MultiplayerGame game = gameProducer.getMultiplayerGame();

        if (game == null) {
            writeResponse(response, HttpServletResponse.SC_NOT_FOUND,
                    new Common.ErrorResponseDTO("Game not found."));
            return;
        }

        if (game.getCreatorId() != login.getUserId() && game.getRole(login.getUserId()) != Role.OBSERVER) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            writeResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    new Common.ErrorResponseDTO("Only the game's creator or an observer can start the game."));
            return;
        }

        if (game.getState() == GameState.CREATED) {
            gameService.startGame(game);
            writeResponse(response, HttpServletResponse.SC_OK, new StartGameResponseDTO(true, "Game started."));
        } else {
            writeResponse(response, HttpServletResponse.SC_EXPECTATION_FAILED, new StartGameResponseDTO(false,
                    "Game has to be in CREATED state to be started."));
        }

    }

    public record StartGameResponseDTO(boolean success, String message) {}
}
