package org.codedefenders.servlets;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.MeleeGameRepository;
import org.codedefenders.database.MultiplayerGameRepository;
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
    private CodeDefendersAuth login;

    @Inject
    private MeleeGameRepository meleeGameRepo;

    @Inject
    private MultiplayerGameRepository multiplayerGameRepo;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        List<UserMultiplayerGameInfo> games = multiplayerGameRepo.getFinishedMultiplayerGamesForUser(login.getUserId());
        request.setAttribute("finishedBattlegroundGames", games);
        List<UserMeleeGameInfo> meleeGames = meleeGameRepo.getFinishedMeleeGamesForUser(login.getUserId());
        request.setAttribute("finishedMeleeGames", meleeGames);

        request.getRequestDispatcher(Constants.GAMES_HISTORY_JSP).forward(request, response);
    }
}
