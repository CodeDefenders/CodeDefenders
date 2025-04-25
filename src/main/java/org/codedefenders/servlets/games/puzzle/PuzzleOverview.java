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
package org.codedefenders.servlets.games.puzzle;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.persistence.database.PuzzleRepository;
import org.codedefenders.util.Constants;
import org.codedefenders.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link HttpServlet} handles to the overview page of {@link PuzzleGame}s.
 *
 * <p>{@code GET} requests redirect to the puzzle overview page.
 *
 * <p>Serves under {@code /puzzles}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 * @see PuzzleGame
 */
@WebServlet(org.codedefenders.util.Paths.PUZZLE_OVERVIEW)
public class PuzzleOverview extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(PuzzleOverview.class);

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private URLUtils url;

    @Inject
    private PuzzleRepository puzzleRepo;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        if (!puzzleRepo.checkPuzzlesEnabled() || !puzzleRepo.checkActivePuzzlesExist()) {
            // Send users to the home page
            response.sendRedirect(url.forPath("/"));
            return;
        }

        request.getRequestDispatcher(Constants.PUZZLE_OVERVIEW_VIEW_JSP).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.info("Unsupported POST request to /puzzles");
        // aborts request and sends 400
        super.doPost(req, resp);
    }
}
