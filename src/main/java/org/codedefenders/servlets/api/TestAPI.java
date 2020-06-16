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
package org.codedefenders.servlets.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.commons.httpclient.HttpStatus;
import org.codedefenders.database.TestDAO;
import org.codedefenders.game.Test;
import org.codedefenders.servlets.util.ServletUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This {@link HttpServlet} offers an API for {@link Test tests}.
 *
 * <p>A {@code GET} request with the {@code testId} parameter results in a JSON string containing
 * test information, including the source code.
 *
 * <p>Serves on path: {@code /api/test}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet(org.codedefenders.util.Paths.API_TEST)
public class TestAPI extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        final Optional<Test> test = ServletUtils.getIntParameter(request, "testId")
                .map(TestDAO::getTestById);

        if (!test.isPresent()) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        PrintWriter out = response.getWriter();

        String json = generateJsonForTest(test.get());

        response.setContentType("application/json");
        out.print(json);
        out.flush();
    }

    private String generateJsonForTest(Test test) {
        Gson gson = new Gson();

        JsonObject root = new JsonObject();
        root.add("id", gson.toJsonTree(test.getId(), Integer.class));
        root.add("playerId", gson.toJsonTree(test.getPlayerId(), Integer.class));
        root.add("gameId", gson.toJsonTree(test.getGameId(), Integer.class));
        root.add("source", gson.toJsonTree(test.getAsString(), String.class));

        return gson.toJson(root);

    }

}
