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
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.MultiplayerGameRepository;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.service.UserService;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;

/**
 * This {@link HttpServlet} handles to the landing page under "/".
 *
 * <p>{@code GET} requests redirects to a landing page, depending whether
 * the requesting user is logged in or not.
 *
 * <p>Serves under {@code /}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet("")
public class LandingPage extends HttpServlet {

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private UserService userService;

    @Inject
    private URLUtils url;

    @Inject
    private MultiplayerGameRepository multiplayerGameRepo;

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        if (login.isLoggedIn()) {
            // User logged in? Send him to the games overview.
            response.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
        } else {
            // User logged not in? Show him the landing page.
            List<MultiplayerGame> availableMultiplayerGames = multiplayerGameRepo.getAvailableMultiplayerGames();
            Collections.shuffle(availableMultiplayerGames, new Random(LocalDate.now().getLong(ChronoField.EPOCH_DAY)));
            availableMultiplayerGames = availableMultiplayerGames
                    .stream()
                    .filter(game -> !game.getDefenderPlayers().isEmpty())
                    .filter(game -> !game.getAttackerPlayers().isEmpty())
                    .limit(15)
                    .collect(Collectors.toList());

            request.setAttribute("openMultiplayerGames", availableMultiplayerGames);

            Map<Integer, String> gameCreatorNames = availableMultiplayerGames.stream()
                    .collect(Collectors.toMap(AbstractGame::getId,
                            game -> userService.getSimpleUserById(game.getCreatorId())
                                    .map(SimpleUser::getName)
                                    .orElse("Unknown user")));

            request.setAttribute("gameCreatorNames", gameCreatorNames);

            request.getRequestDispatcher(Constants.INDEX_JSP).forward(request, response);
        }
    }
}
