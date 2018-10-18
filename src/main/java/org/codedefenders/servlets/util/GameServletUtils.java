package org.codedefenders.servlets.util;

import org.codedefenders.servlets.games.GameManager;
import org.codedefenders.servlets.games.GameSelectionManager;
import org.codedefenders.servlets.games.MultiplayerGameManager;
import org.codedefenders.servlets.games.MultiplayerGameSelectionManager;
import org.codedefenders.servlets.games.puzzle.PuzzleGameManager;
import org.codedefenders.servlets.games.puzzle.PuzzleGameSelectionManager;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

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
        return ServletUtils.getIntParameter(request, "gameId");
    }
}
