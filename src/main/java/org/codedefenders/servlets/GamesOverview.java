/*
 * Copyright (C) 2016-2019 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.servlets;

import java.io.IOException;
import java.util.List;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.UserMeleeGameInfo;
import org.codedefenders.model.UserMultiplayerGameInfo;
import org.codedefenders.persistence.database.MeleeGameRepository;
import org.codedefenders.persistence.database.MultiplayerGameRepository;
import org.codedefenders.servlets.games.puzzle.PuzzleOverview;
import org.codedefenders.util.Constants;

import jakarta.inject.Inject;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.GAME_CREATION;
import static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.GAME_JOINING;

/**
 * This {@link HttpServlet} handles to the overview page of
 * {@link MultiplayerGame} and {@link MeleeGame} games.
 *
 * <p>{@code GET} requests redirect to a game overview page.
 *
 * <p>Serves under {@code /games/overview}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 * @see PuzzleOverview
 */
@WebServlet(org.codedefenders.util.Paths.GAMES_OVERVIEW)
public class GamesOverview extends HttpServlet {

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private MeleeGameRepository meleeGameRepo;

    @Inject
    private MultiplayerGameRepository multiplayerGameRepo;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        List<UserMultiplayerGameInfo> activeGames =
                multiplayerGameRepo.getActiveMultiplayerGamesWithInfoForUser(login.getUserId());
        request.setAttribute("activeGames", activeGames);

        List<UserMeleeGameInfo> activeMeleeGames =
                meleeGameRepo.getActiveMeleeGamesWithInfoForUser(login.getUserId());
        request.setAttribute("activeMeleeGames", activeMeleeGames);

        List<UserMultiplayerGameInfo> openGames =
                multiplayerGameRepo.getOpenMultiplayerGamesWithInfoForUser(login.getUserId());
        request.setAttribute("openGames", openGames);

        List<UserMeleeGameInfo> openMeleeGames =
                meleeGameRepo.getOpenMeleeGamesWithInfoForUser(login.getUserId());
        request.setAttribute("openMeleeGames", openMeleeGames);

        boolean gamesJoinable = AdminDAO.getSystemSetting(GAME_JOINING).getBoolValue();
        request.setAttribute("gamesJoinable", gamesJoinable);

        boolean gamesCreatable = AdminDAO.getSystemSetting(GAME_CREATION).getBoolValue();
        request.setAttribute("gamesCreatable", gamesCreatable);

        RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.USER_GAMES_OVERVIEW_JSP);
        dispatcher.forward(request, response);
    }
}
