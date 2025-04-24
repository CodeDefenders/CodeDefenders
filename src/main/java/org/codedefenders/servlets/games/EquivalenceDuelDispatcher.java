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
package org.codedefenders.servlets.games;

import java.io.IOException;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.game.GameMode;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receive requests for Equivalence Duels and dispatch them to the "right"
 * servlet. In the future, we can centralize in this servlet all the common
 * logic for handling equivalence duels
 *
 * @author gambi
 */
@WebServlet("/equivalence-duels")
public class EquivalenceDuelDispatcher extends HttpServlet {

    // TODO Having "Injectable" game controllers would be a nice solution to
    // decouple request dispatching and game-state-logic

    private static final Logger logger = LoggerFactory.getLogger(EquivalenceDuelDispatcher.class);

    @Inject
    private GameRepository gameRepo;


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (gameIdOpt.isEmpty()) {
            logger.warn("No gameId parameter. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }

        final int gameId = gameIdOpt.get();

        // TODO This might be a bit too much, but maybe we can be sure we got the right
        // requests?
        final String action = ServletUtils.formType(request);

        // TODO We can also perform additional checks here? Like is the game active or
        // the player exists or what's not.
        switch (action) {
            case "claimEquivalent":
            case "resolveEquivalence":
                forwardEquivalenceAction(request, response, gameId);
                return;
            default:
                logger.info("Action not recognised: {}", action);
                Redirect.redirectBack(request, response);
        }
    }

    private void forwardEquivalenceAction(HttpServletRequest request, HttpServletResponse response, int gameId)
            throws IOException, ServletException {
        final GameMode gameType = gameRepo.getGameMode(gameId).orElseThrow();

        ServletContext context = this.getServletContext();

        switch (gameType) {
            case PARTY: {
                RequestDispatcher dispatcher = context.getRequestDispatcher(Paths.BATTLEGROUND_GAME);
                dispatcher.forward(request, response);
                break;
            }
            case MELEE: {
                RequestDispatcher dispatcher = context.getRequestDispatcher(Paths.MELEE_GAME);
                dispatcher.forward(request, response);
                break;
            }
            default:
                logger.info("Cannot handle Equivalence Claim for Game Type: {}", gameType);
                Redirect.redirectBack(request, response);
        }
    }
}
