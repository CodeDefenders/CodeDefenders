package org.codedefenders.servlets;

import org.codedefenders.game.duel.DuelGame;
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
 * This {@link HttpServlet} handles to the overview page of {@link DuelGame Duel} and
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
