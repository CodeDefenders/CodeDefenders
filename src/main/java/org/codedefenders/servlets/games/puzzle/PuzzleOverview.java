package org.codedefenders.servlets.games.puzzle;

import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This {@link HttpServlet} handles to the overview page of {@link PuzzleGame}s.
 * <p>
 * {@code GET} requests redirect to the puzzle overview page.
 * <p>
 * Serves under {@code /puzzles}.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 * @see PuzzleGameSelectionManager
 * @see PuzzleGame
 */
public class PuzzleOverview extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(PuzzleOverview.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Extend this request handling for locked puzzles
        request.getRequestDispatcher(Constants.PUZZLE_OVERVIEW_VIEW_JSP).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.info("Unsupported POST request to /puzzles");
        // aborts request and sends 400
        super.doPost(req, resp);
    }
}
