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
package org.codedefenders.servlets.games.battleground;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.game.ScoreboardBean;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.util.Constants.BATTLEGROUND_SCOREBOARD_JSP;

/**
 * This {@link HttpServlet} handles asynchronous reloading of the scoreboard.
 *
 * @see Paths#BATTLEGROUND_GAME_SCOREBOARD
 */
@WebServlet({Paths.BATTLEGROUND_GAME_SCOREBOARD})
public class MultiplayerScoreboardUpdater extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(MultiplayerScoreboardUpdater.class);

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private GameProducer gameProducer;

    @Inject
    private PlayerRepository playerRepo;

    @Inject
    private ScoreboardBean scoreboard;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final MultiplayerGame game = gameProducer.getMultiplayerGame();
        if (game == null) {
            logger.error("No game found. Aborting request.");
            response.sendError(404, "Game not found.");
            return;
        }

        final int gameId = game.getId();
        final int playerId = playerRepo.getPlayerIdForUserAndGame(login.getUserId(), gameId);
        if (playerId == -1) {
            logger.info("User {} not part of game {}. Aborting request.", login.getUserId(), gameId);
            response.sendError(403, "You are not a player in this game.");
            return;
        }

        scoreboard.setGameId(game.getId());
        scoreboard.setScores(game.getMutantScores(), game.getTestScores());
        scoreboard.setPlayers(game.getAttackerPlayers(), game.getDefenderPlayers());
        request.setAttribute("scoreboard", scoreboard);

        request.getRequestDispatcher(BATTLEGROUND_SCOREBOARD_JSP).forward(request, response);
    }

}
