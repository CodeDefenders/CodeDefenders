/*
 * Copyright (C) 2020 Code Defenders contributors
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

import org.codedefenders.beans.game.MeleeScoreboardBean;
import org.codedefenders.database.MeleeGameDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameState;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.scoring.ScoreCalculator;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;

import javax.inject.Inject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@WebServlet(Paths.MELEE_HISTORY)
public class MeleeGameHistoryManager extends HttpServlet {

    @Inject
    private ScoreCalculator scoreCalculator;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {


        MeleeGame game;

        final Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (!gameIdOpt.isPresent()) {
            response.sendRedirect(request.getContextPath() + Paths.GAMES_HISTORY);
            return;
        }
        game = MeleeGameDAO.getMeleeGame(gameIdOpt.get());

        if (game == null || game.getState() != GameState.FINISHED) {
            response.sendRedirect(request.getContextPath() + Paths.GAMES_OVERVIEW);
            return;
        }

        request.setAttribute("game", game);


        // Compute the score and pass along the ScoreBoard bean?
        MeleeScoreboardBean meleeScoreboardBean = new MeleeScoreboardBean();
        // Why ID is necessary here?
        meleeScoreboardBean.setGameId(game.getId());
        meleeScoreboardBean.setScores(scoreCalculator.getMutantScores(), scoreCalculator.getTestScores(),
                scoreCalculator.getDuelScores());
        meleeScoreboardBean.setPlayers(game.getPlayers());
        // Set the preconditions for the score board
        request.setAttribute("meleeScoreboardBean", meleeScoreboardBean);

        RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.MELEE_GAME_HISTORY_VIEW_JSP);
        dispatcher.forward(request, response);
    }
}
