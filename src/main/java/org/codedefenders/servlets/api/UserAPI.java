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

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

@WebServlet("/api/user")
public class UserAPI extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(UserAPI.class);


    @Inject
    UserRepository userRepo;

    @Inject
    GameRepository gameRepo;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        List<String> userNames;
        Map<String, String[]> params = request.getParameterMap();
        if (params.containsKey("forGame") && Boolean.parseBoolean(request.getParameter("forGame"))) {
            if (!params.containsKey("gameId")) {
                logger.error("forGame is true, but no gameId given");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            userNames = gameRepo.getUsernamesForGame(Integer.parseInt(request.getParameter("gameId")));
        } else {
            userNames = userRepo.getUsers().stream()
                    .map(UserEntity::getUsername)
                    .toList();
        }

        Gson gson = new Gson();
        String json = gson.toJson(userNames);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            response.getWriter().write(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
