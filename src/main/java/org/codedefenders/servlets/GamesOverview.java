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
package org.codedefenders.servlets;

import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.servlets.games.puzzle.PuzzleOverview;
import org.codedefenders.util.Constants;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This {@link HttpServlet} handles to the overview page of
 * {@link MultiplayerGame Battleground} games.
 * <p>
 * {@code GET} requests redirect to a game overview page.
 * <p>
 * Serves under {@code /games/overview}.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 * @see PuzzleOverview
 * @see org.codedefenders.util.Paths#GAMES_OVERVIEW
 */
public class GamesOverview extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Phil 02/01/19: extract information retrieval logic from JSP file and set information as request parameters.

        RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.USER_GAMES_OVERVIEW_JSP);
        dispatcher.forward(request, response);
    }
}
