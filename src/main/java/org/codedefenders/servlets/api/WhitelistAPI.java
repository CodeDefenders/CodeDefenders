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
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.model.WhitelistType;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.persistence.database.WhitelistRepository;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Used to add, remove and query to and from whitelists of players in a game.
 */
@WebServlet(Paths.WHITELIST_API)
public class WhitelistAPI extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(WhitelistAPI.class);
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

            String[] addIdParams = params.get("addIds");
            String[] addNameParams = params.get("addNames");
            int[] addIds;
            try {
                addIds = userIds(addIdParams, addNameParams);
            } catch (NumberFormatException e) {
                logger.error("Failed to parse addIds", e);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid player ID: " + e.getMessage());
                return;
            }
            String[] removeIdParams = params.get("removeIds");
            String[] removeNameParams = params.get("removeNames");
            int[] removeIds;
            try {
                removeIds = userIds(removeIdParams, removeNameParams);
            } catch (NumberFormatException e) {
                logger.error("Failed to parse removeIds", e);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid player ID: " + e.getMessage());
                return;
            }
            if (!params.containsKey("type")) {
                for (int userId : addIds) {
                    whitelistRepo.addToWhitelist(gameId, userId);
                }
                for (int userId : removeIds) {
                    whitelistRepo.removeFromWhitelist(gameId, userId);
                }
            } else {
                WhitelistType type = WhitelistType.fromString(params.get("type")[0]);
                for (int userId : addIds) {
                    whitelistRepo.addToWhitelist(gameId, userId, type);
                }
                for (int userId : removeIds) {
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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String[]> params = req.getParameterMap();
        if (!params.containsKey("gameId")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing gameId parameter");
            return;
        }
        int gameId = Integer.parseInt(params.get("gameId")[0]);
        List<String> userNames;
        if (!params.containsKey("type")) {
            userNames = whitelistRepo.getWhiteListedPlayerNames(gameId);
        } else {
            userNames = whitelistRepo.getWhiteListedPlayerNames(gameId,
                    WhitelistType.fromString(params.get("type")[0]));
        }
        Gson gson = new Gson();
        String json = gson.toJson(userNames);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(json);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().flush();
        resp.getWriter().close(); //TODO Notwendig? In GameClassAPI wird es nicht gemacht

    }

    /**
     * Combines id and name parameter string arrays into a single int array of user ids.
     */
    private int[] userIds(String[] userIdParams, String[] userNameParams) {
        int[] userIds = new int[(userIdParams != null ? userIdParams.length : 0)
                + (userNameParams != null ? userNameParams.length : 0)];
        if (userIdParams != null) {
            for (int i = 0; i < userIdParams.length; i++) {
                userIds[i] = Integer.parseInt(userIdParams[i]);
            }
        }
        int offset = userIdParams != null ? userIdParams.length : 0;
        if (userNameParams != null) {
            for (int i = 0; i < userNameParams.length; i++) {
                int userId = userRepo.getUserByName(userNameParams[i]).orElseThrow().getId();
                userIds[offset + i] = userId;
            }
        }
        return userIds;
    }
}
