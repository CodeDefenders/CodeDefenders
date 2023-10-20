package org.codedefenders.servlets.games;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.database.GameDAO;
import org.codedefenders.game.GameMode;
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
        final GameMode gameType = GameDAO.getGameMode(gameId);

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
