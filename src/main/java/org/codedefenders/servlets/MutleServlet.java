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

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.arcade.mutle.service.MutleService;
import org.codedefenders.arcade.mutle.service.MutleSettings;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.util.Paths;

@WebServlet({Paths.MUTLE, Paths.MUTLE_PLAY})
public class MutleServlet extends HttpServlet {
    @Inject
    private MutleService mutleService;

    @Inject
    private MutleSettings settings;

    @Inject
    private CodeDefendersAuth login;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!settings.enabled() || !mutleService.isReady()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (Paths.MUTLE.equals(request.getServletPath())) {
            request.setAttribute("currentCase", mutleService.current(login.getUserId()).orElse(null));
            request.setAttribute("currentFixture", mutleService.currentFixture());
            request.setAttribute("history", mutleService.history(login.getUserId()));
            if (login.isAdmin()) {
                request.setAttribute("fixtures", mutleService.fixtures());
            }
            request.getRequestDispatcher("/jsp/mutle/overview.jsp").forward(request, response);
            return;
        }
        String gameId = request.getParameter("gameId");
        if (gameId != null && !gameId.matches("[1-9][0-9]*")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String fixtureKey = request.getParameter("fixture");
        if (fixtureKey != null && (gameId != null || !login.isAdmin()
                || mutleService.fixtures().stream().noneMatch(fixture -> fixture.fixtureKey().equals(fixtureKey)))) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        request.setAttribute("mutleGameId", gameId == null ? "" : gameId);
        request.setAttribute("mutleFixtureKey", fixtureKey == null ? "" : fixtureKey);
        request.getRequestDispatcher("/jsp/mutle/play.jsp").forward(request, response);
    }
}
