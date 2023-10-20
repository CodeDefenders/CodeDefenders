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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.game.Mutant;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.util.ServletUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * This {@link HttpServlet} offers an API for {@link Mutant mutants}.
 *
 * <p>A {@code GET} request with the {@code mutantId} parameter results in a JSON string containing
 * mutant information, including the source code diff.
 *
 * <p>Serves on path: {@code /api/mutant}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet(org.codedefenders.util.Paths.API_MUTANT)
public class MutantAPI extends HttpServlet {

    @Inject
    GameService gameService;

    @Inject
    CodeDefendersAuth login;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        final Optional<MutantDTO> mutant = ServletUtils.getIntParameter(request, "mutantId")
                .map(id -> gameService.getMutant(login.getUserId(), id));

        if (mutant.isEmpty()) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        PrintWriter out = response.getWriter();

        String json = generateJsonForMutant(mutant.get());

        response.setContentType("application/json");
        out.print(json);
        out.flush();
    }

    private String generateJsonForMutant(MutantDTO mutant) {
        Gson gson = new Gson();

        JsonObject root = new JsonObject();
        root.add("id", gson.toJsonTree(mutant.getId(), Integer.class));
        root.add("playerId", gson.toJsonTree(mutant.getPlayerId(), Integer.class));
        root.add("gameId", gson.toJsonTree(mutant.getGameId(), Integer.class));
        root.add("diff", gson.toJsonTree(mutant.getPatchString(), String.class));

        return gson.toJson(root);

    }

}
