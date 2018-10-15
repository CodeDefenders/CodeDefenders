package org.codedefenders.servlets.util;

import org.codedefenders.database.PuzzleDAO;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.servlets.games.GameManager;
import org.codedefenders.servlets.games.GameSelectionManager;
import org.codedefenders.servlets.games.MultiplayerGameManager;
import org.codedefenders.servlets.games.MultiplayerGameSelectionManager;
import org.codedefenders.servlets.games.puzzle.PuzzleGameManager;
import org.codedefenders.servlets.games.puzzle.PuzzleGameSelectionManager;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.codedefenders.util.Constants.SESSION_ATTRIBUTE_PUZZLE_GAME;

/**
 * This class offers static methods, which offer functionality useful for {@link HttpServlet}
 * implementations which manage game
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 * @see GameManager
 * @see GameSelectionManager
 * @see MultiplayerGameManager
 * @see MultiplayerGameSelectionManager
 * @see PuzzleGameManager
 * @see PuzzleGameSelectionManager
 */
public final class GameServletUtils {
    private GameServletUtils() {
    }

    /**
     * Extracts the {@code gameId} URL parameter from a given request.
     * <p>
     * If {@code gameId} is no valid integer value, the method returns {@code null}.
     *
     * @param request the request, which {@code gameId} is extracted from.
     * @return a valid integer extracted from the {@code gameId} parameter of the given request, or {@code null}.
     * @see ServletUtils#getIntParameter(HttpServletRequest, String)
     */
    public static Integer getGameId(HttpServletRequest request) {
        final Integer gameId;

        final String gameIdParameter = request.getParameter("gameId");
        if (gameIdParameter == null) {
            return null;
        }
        try {
            gameId = Integer.parseInt(gameIdParameter);
        } catch (NumberFormatException e) {
            return null;
        }
        return gameId;
    }

    public static final class Puzzle {
        private Puzzle() {
        }

        /**
         * For a given identifier retrieves a {@link PuzzleGame} from the session (if available and identifier of
         * existing session puzzle game matches) or the database.
         * <p>
         * Returns {@code null} if no game could be found for identifier.
         *
         * @param session the session the puzzle game may be retrieved from (if available and ID matches).
         * @param gameId  the given identifier of the puzzle game.
         * @return a puzzle game object, or {@code null} if no puzzle game found.
         */
        public static PuzzleGame getPuzzleGame(HttpSession session, int gameId) {
            final PuzzleGame game;
            final PuzzleGame sessionGame = (PuzzleGame) session.getAttribute(SESSION_ATTRIBUTE_PUZZLE_GAME);
            if (sessionGame != null && sessionGame.getId() == gameId) {
                game = sessionGame;
            } else {
                game = PuzzleDAO.getPuzzleGameForId(gameId);
            }
            return game;
        }

        /**
         * Removes the active puzzle game attribute for a given session if a the game
         * puzzle identifier matches the given identifier.
         *
         * @param session      the given session the attribute is removed from.
         * @param puzzleGameId the identifier of the puzzle game to be removed.
         */
        public static void invalidateSessionPuzzleGameIfIdMatches(HttpSession session, int puzzleGameId) {
            final PuzzleGame game = (PuzzleGame) session.getAttribute(SESSION_ATTRIBUTE_PUZZLE_GAME);
            if (game.getId() == puzzleGameId) {
                session.removeAttribute(SESSION_ATTRIBUTE_PUZZLE_GAME);
            }
        }
    }
}
