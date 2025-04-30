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
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.codedefenders.game.GameClass;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.servlets.util.ServletUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * This {@link HttpServlet} offers an API for {@link GameClass game classes}.
 *
 * <p>A {@code GET} request with the {@code classId} parameter results in a JSON string containing
 * class information, including the source code.
 *
 * <p>Serves on path: {@code /api/class}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet(org.codedefenders.util.Paths.API_CLASS)
public class GameClassAPI extends HttpServlet {

    private boolean withDependencies;

    @Inject
    private GameClassRepository gameClassRepo;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        final Optional<GameClass> classId = ServletUtils.getIntParameter(request, "classId")
                .flatMap(gameClassRepo::getClassForId);

        int withDependenciesInt = ServletUtils.getIntParameter(request, "withDependencies").orElse(0);
        switch (withDependenciesInt) {
            case 0:
                withDependencies = false;
                break;
            case 1:
                withDependencies = true;
                break;
            default:
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
                return;
        }

        if (classId.isEmpty()) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        PrintWriter out = response.getWriter();

        String json = generateJsonForClass(classId.get());

        response.setContentType("application/json");
        out.print(json);
        out.flush();
    }

    private String generateJsonForClass(GameClass gameClass) {
        Gson gson = new Gson();

        JsonObject root = new JsonObject();
        root.add("id", gson.toJsonTree(gameClass.getId(), Integer.class));
        root.add("name", gson.toJsonTree(gameClass.getName(), String.class));
        root.add("alias", gson.toJsonTree(gameClass.getAlias(), String.class));
        root.add("source", gson.toJsonTree(gameClass.getSourceCode(), String.class));

        if (withDependencies) {
            root.add("dependency_names",
                    gson.toJsonTree(gameClass.getDependencyNames().toArray(), String[].class));
            root.add("dependency_code",
                    gson.toJsonTree(gameClass.getDependencyCode().toArray(), String[].class));
        }

        return gson.toJson(root);

    }

}
