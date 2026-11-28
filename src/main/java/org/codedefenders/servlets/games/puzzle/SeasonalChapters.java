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
package org.codedefenders.servlets.games.puzzle;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.persistence.database.PuzzleRepository;
import org.codedefenders.service.SeasonalStylingService;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;

@WebServlet(Paths.SEASONAL_CHAPTERS)
public class SeasonalChapters extends HttpServlet {

    @Inject
    private PuzzleRepository puzzleRepo;

    @Inject
    private SeasonalStylingService seasonStyle;

    @Inject
    private URLUtils url;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        if (puzzleRepo.checkPuzzlesEnabled()
                && puzzleRepo.checkActivePuzzlesExist()
                && seasonStyle.getCurrentSeasonalChapter().isPresent()) {
            request.getRequestDispatcher(Constants.SEASONAL_CHAPTERS_VIEW_JSP)
                    .forward(request, response);
        } else {
            response.sendRedirect(url.forPath(Paths.LANDING_PAGE));
        }
    }
}
