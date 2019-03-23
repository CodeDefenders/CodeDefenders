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
package org.codedefenders.servlets.games;

import com.google.gson.Gson;

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

 // TODO Phil 02/01/19: this seems to be never used so we may remove it?
public class MutantManager extends HttpServlet {

    private static final Logger logger =
            LoggerFactory.getLogger(MutantManager.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String contextPath = request.getContextPath();
        int gameId = Integer.parseInt(request.getParameter("gameId"));

        AbstractGame game = MultiplayerGameDAO.getMultiplayerGame(gameId);

        int userId = (int) request.getSession().getAttribute("uid");

        Gson gson = new Gson();
        PrintWriter out = response.getWriter();

        try {
            if (!canAccess(request)) {
                ArrayList<Mutant> ms = new ArrayList<Mutant>();

                out.print(gson.toJson(ms));
                out.flush();

            } else {

                response.setContentType("text/json");

                List<Mutant> mutants = game.getMutants();

                boolean showDiff = !DatabaseAccess.getRole(userId, gameId)
                        .equals(Role.DEFENDER) || game.getLevel().equals(
                        GameLevel.EASY);

                for (Mutant m : mutants){
                    m.prepareForSerialise(showDiff);
                }

                out.print(gson.toJson(mutants));
                out.flush();
            }
        } catch (Exception e) {
            response.sendRedirect(contextPath + Paths.GAMES_OVERVIEW);
        }
    }

    private boolean canAccess(HttpServletRequest request) {
        //TODO: Implement heavy load/DDOS handling
        if (request.getParameter("gameId") != null) {
            int pId = DatabaseAccess.getPlayerIdForMultiplayerGame(
                    (int)request.getSession().getAttribute("uid"),
                    Integer.parseInt(request.getParameter("gameId"))
            );
            return pId >= 0;
        }
        return false;
    }

}