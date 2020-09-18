package org.codedefenders.servlets;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.database.MeleeGameDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.model.UserMeleeGameInfo;
import org.codedefenders.model.UserMultiplayerGameInfo;
import org.codedefenders.util.Constants;

/**
 * This {@link HttpServlet} redirects to the games history page.
 *
 * <p>Serves on path: {@code /games/history}.
 */
@WebServlet(org.codedefenders.util.Paths.GAMES_HISTORY)
public class GameHistoryOverview extends HttpServlet {

    @Inject
    private LoginBean login;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        List<UserMultiplayerGameInfo> games = MultiplayerGameDAO.getFinishedMultiplayerGamesForUser(login.getUserId());
        request.setAttribute("finishedBattlegroundGames", games);
        List<UserMeleeGameInfo> meleeGames = MeleeGameDAO.getFinishedMeleeGamesForUser(login.getUserId());
        request.setAttribute("finishedMeleeGames", meleeGames);

        request.getRequestDispatcher(Constants.GAMES_HISTORY_JSP).forward(request, response);
    }
}
