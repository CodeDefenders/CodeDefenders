package org.codedefenders.servlets.games.puzzle;

import org.codedefenders.database.PuzzleDAO;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.codedefenders.servlets.util.ServletUtils.gameId;
import static org.codedefenders.servlets.util.ServletUtils.ctx;
import static org.codedefenders.servlets.util.ServletUtils.getIntParameter;
import static org.codedefenders.util.Constants.REQUEST_ATTRIBUTE_PUZZLE_GAME;

/**
 * This {@link HttpServlet} handles management of {@link PuzzleGame}.
 * <p>
 * Offers {@code POST} request handling for creating and ending a given game.
 * <p>
 * Serves under {@code /puzzle/games}.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 * @see PuzzleGameManager
 * @see PuzzleGame
 */
public class PuzzleGameSelectionManager extends HttpServlet {
    private static Logger logger = LoggerFactory.getLogger(PuzzleGameSelectionManager.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect(ctx(request) + Paths.PUZZLE_OVERVIEW);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String action = ServletUtils.formType(request);
        switch (action) {
            case "createGame":
                createGame(request, response);
                break;
            case "endGame":
                endGame(request, response);
                break;
            default:
                logger.info("Action not recognised: {}", action);
                Redirect.redirectBack(request, response);
                break;
        }
    }

    /**
     * Creates a puzzle game for a given request for the required parameter {@code puzzleId}.
     * <p>
     * If the provided parameter is not valid, the request will abort and return a {@code 400} status code.
     * <p>
     * If a puzzle game can be created, the game is started and the user is redirected to the game page.
     *
     * @param request  the request to create a test.
     * @param response the response to the request.
     * @throws IOException when redirecting fails.
     */
    static void createGame(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final HttpSession session = request.getSession();
        final int userId = ((Integer) session.getAttribute("uid"));

        final Optional<Integer> puzzleId = getIntParameter(request, "puzzleId");
        if (!puzzleId.isPresent()) {
            logger.error("Failed to retrieve puzzleId from request.");
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }

        final Puzzle puzzle = PuzzleDAO.getPuzzleForId(puzzleId.get());
        if (puzzle == null) {
            logger.error("Failed to retrieve puzzle from database for puzzleId: {}.", puzzleId);
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }

        final PuzzleGame game = PuzzleGame.createPuzzleGame(puzzle, userId);
        if (game == null) {
            logger.error("Failed to create puzzle game for puzzleId: {} and userId: {}.", puzzleId, userId);
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }

        request.setAttribute(REQUEST_ATTRIBUTE_PUZZLE_GAME, game);

        String path = ctx(request) + Paths.PUZZLE_GAME + "?gameId=" + game.getId();
        response.sendRedirect(path);
    }

    /**
     * Ends a puzzle game for a given request for the required parameter {@code gameId} before the game is solved.
     * <p>
     * If the provided parameter is not valid, the request will abort and return a {@code 400} status code.
     * <p>
     * If found, the game is shut down and the user is redirected to the puzzle overview page.
     *
     * @param request  the request to create a mutant.
     * @param response the response to the request.
     * @throws IOException when redirecting fails.
     */
    private static void endGame(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final HttpSession session = request.getSession();
        final int userId = ((Integer) session.getAttribute("uid"));

        final Optional<Integer> gameIdOpt = gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("Failed to retrieve gameId from request.");
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }
        final int gameId = gameIdOpt.get();

        final PuzzleGame game = PuzzleDAO.getPuzzleGameForId(gameId);
        if (game == null) {
            logger.error("Failed to retrieve puzzle game from database for gameId: {}.", gameId);
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }

        if (game.getMode() != GameMode.PUZZLE) {
            logger.error("Trying to end non-puzzle game {}.", gameId);
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }

        if (game.getState() != GameState.ACTIVE) {
            logger.error("Trying to end non-active game {}", gameId);
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }

        logger.info("User {} ended puzzle {} manually.", userId, game.getPuzzleId());
        game.setState(GameState.FAILED);
        game.update();

        response.sendRedirect(ctx(request) + Paths.PUZZLE_OVERVIEW);
    }
}
