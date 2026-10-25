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
package org.codedefenders.servlets.api;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.game.TestAccordionBean;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;

/**
 * This {@link HttpServlet} offers an API for the test accordion game component.
 * It returns the data for categories and tests needed to render the test accordion.
 * If a {@code testId} is provided as a request parameter,
 * it returns only the data of that specific test and its categories.
 *
 * <p>Serves on path: {@code /api/test-accordion}.
 */
@WebServlet(Paths.API_TEST_ACCORDION)
public class TestAccordionAPI extends HttpServlet {

    @Inject
    private GameProducer gameProducer;

    @Inject
    private TestAccordionBean testAccordionBean;

    @Inject
    private CodeDefendersAuth login;

    @Override
    protected void doGet(final HttpServletRequest request,
                         final HttpServletResponse response) throws ServletException, IOException {
        final var gameId = ServletUtils.getIntParameter(request, "gameId");
        if (gameId.isEmpty()) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        gameProducer.setGameId(gameId.get());
        final var game = gameProducer.getGame();
        if (game == null) {
            response.setStatus(HttpStatus.SC_NOT_FOUND);
            return;
        }
        if (!game.isUserAuthorized(login.getUserId())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        var testId = ServletUtils.getIntParameter(request, "testId");
        var json = testId.map(this::getJsonForNewTest).orElseGet(this::getCompleteJson);

        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        out.print(json);
        out.flush();
    }

    private String getCompleteJson() {
        return "{\"categories\":" + testAccordionBean.jsonFromCategories() + "," +
                "\"tests\":" + testAccordionBean.jsonTests() + "}";
    }

    private String getJsonForNewTest(int testId) {
        return "{\"categories\":" + testAccordionBean.jsonCategoriesContainingTest(testId) + "," +
                "\"test\":" + testAccordionBean.jsonTest(testId) + "}";
    }
}
