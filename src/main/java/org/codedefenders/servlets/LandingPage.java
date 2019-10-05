package org.codedefenders.servlets;

import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.User;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.codedefenders.servlets.util.ServletUtils.ctx;

/**
 * This {@link HttpServlet} handles to the landing page under "/".
 * <p>
 * {@code GET} requests redirects to a landing page, depending whether
 * the requesting user is logged in or not.
 * <p>
 * Serves under {@code /}.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 * @see org.codedefenders.util.Paths#LANDING_PAGE
 */
@WebServlet("")
public class LandingPage extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (isUserLoggedIn(request)) {
            // User logged in? Send him to the games overview.
            response.sendRedirect(ctx(request) + Paths.GAMES_OVERVIEW);
        } else {
            // User logged not in? Show him the landing page.
            final List<MultiplayerGame> availableMultiplayerGames = MultiplayerGameDAO.getAvailableMultiplayerGames();
            request.setAttribute("openMultiplayerGames", availableMultiplayerGames);

            request.setAttribute("gameCreatorNames", UserDAO.getGamesCreatorNames(availableMultiplayerGames));

            request.getRequestDispatcher(Constants.INDEX_JSP).forward(request, response);
        }
    }

    private boolean isUserLoggedIn(HttpServletRequest request) {
        final Integer userId = (Integer) request.getSession().getAttribute("uid");
        if (userId != null) {
            final User user = UserDAO.getUserById(userId);
            if (user != null && user.isActive()) {
                return true;
            }
        }
        return false;
    }
}
