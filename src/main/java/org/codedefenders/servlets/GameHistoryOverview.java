/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.model.UserMeleeGameInfo;
import org.codedefenders.model.UserMultiplayerGameInfo;
import org.codedefenders.persistence.database.MeleeGameRepository;
import org.codedefenders.persistence.database.MultiplayerGameRepository;
import org.codedefenders.util.Constants;
import org.codedefenders.util.JspWorkaround;

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

        JspWorkaround.forwardInWrapper(request, response, "Game History", Constants.GAMES_HISTORY_JSP);
    }
}
