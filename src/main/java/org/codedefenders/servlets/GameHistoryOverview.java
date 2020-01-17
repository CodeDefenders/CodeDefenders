package org.codedefenders.servlets;

import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.model.UserMultiplayerGameInfo;
import org.codedefenders.util.Constants;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This {@link HttpServlet} redirects to the games history page.
 *
 * <p>Serves on path: {@code /games/history}.
 *
 * @see org.codedefenders.util.Paths#GAMES_HISTORY
 */
@WebServlet("/games/history")
public class GameHistoryOverview extends HttpServlet {

    @Inject
    private LoginBean login;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        List<UserMultiplayerGameInfo> games = MultiplayerGameDAO.getFinishedMultiplayerGamesForUser(login.getUserId());
        request.setAttribute("finishedBattlegroundGames", games);

        request.getRequestDispatcher(Constants.GAMES_HISTORY_JSP).forward(request, response);
    }
}
