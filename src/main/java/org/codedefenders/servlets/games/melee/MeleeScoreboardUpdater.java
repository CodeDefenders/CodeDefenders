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
package org.codedefenders.servlets.games.melee;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.game.MeleeScoreboardBean;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.util.Constants.MELEE_SCOREBOARD_JSP;

/**
 * This {@link HttpServlet} handles asynchronous reloading of the scoreboard.
 *
 * @see Paths#MELEE_GAME_SCOREBOARD
 */
@WebServlet({Paths.MELEE_GAME_SCOREBOARD, Paths.MELEE_GAME_SCORE})
public class MeleeScoreboardUpdater extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(MeleeScoreboardUpdater.class);

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private GameProducer gameProducer;

    @Inject
    private PlayerRepository playerRepo;

    @Inject
    private MeleeScoreboardBean scoreboardBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final MeleeGame game = gameProducer.getMeleeGame();
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

        if (request.getServletPath().equals(Paths.MELEE_GAME_SCORE)) {
            int score = getScoreForUser(login.getUserId());
            response.setContentType("application/json");
            response.getWriter().write("{\"score\":" + score + "}");
            return;
        }

        request.getRequestDispatcher(MELEE_SCOREBOARD_JSP).forward(request, response);
    }

    private int getScoreForUser(int userId) {
        return scoreboardBean.getScoreItems().stream()
                .filter(item -> item.getUser().getId() == userId)
                .findFirst()
                .map(item -> item.getDuelScore().getTotalScore()
                        + item.getAttackScore().getTotalScore()
                        + item.getDefenseScore().getTotalScore())
                .orElse(0);
    }
}
