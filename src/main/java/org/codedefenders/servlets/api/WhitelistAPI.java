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
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.persistence.database.WhitelistRepository;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.util.Paths;

/**
 * Used to add, remove and query to and from whitelists of players in a game.
 */
@WebServlet(Paths.WHITELIST_API)
public class WhitelistAPI extends HttpServlet {
    @Inject
    private MessagesBean messages;

    @Inject
    private WhitelistRepository whitelistRepo;

    @Inject
    private UserRepository userRepo;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Map<String, String[]> params = req.getParameterMap();
            if (!params.containsKey("gameId")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing gameId parameter");
                return;
            }
            int gameId = Integer.parseInt(params.get("gameId")[0]);

            String[] userIdParams = params.get("user-id");
            String[] userNameParams = params.get("user-name");

            //Combines ids and names into one array
            int[] userIds = new int[(userIdParams != null ? userIdParams.length : 0)
                    + (userNameParams != null ? userNameParams.length : 0)];
            if (userIdParams != null) {
                for (int i = 0; i < userIdParams.length; i++) {
                    userIds[i] = Integer.parseInt(userIdParams[i]);
                }
            }
            int offset = userIdParams != null ? userIdParams.length : 0;
            if (userNameParams != null) {
                for (int i = 0;  i < userNameParams.length; i++) {
                    int userId = userRepo.getUserByName(userNameParams[i]).orElseThrow().getId();
                    userIds[offset + i] = userId;
                }
            }
            if (params.containsKey("add")) {
                for (int userId : userIds) {
                    whitelistRepo.addToWhitelist(gameId, userId);
                }
                //messages.add("Added " + userIds.length + " players to the whitelist!");
                //TODO Feedback
            }
            if (params.containsKey("remove")) {
                for (int userId : userIds) {
                    whitelistRepo.removeFromWhitelist(gameId, userId);
                }
            }
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid player ID");
        } finally {
            Redirect.redirectBack(req, resp);
        }
    }
}
