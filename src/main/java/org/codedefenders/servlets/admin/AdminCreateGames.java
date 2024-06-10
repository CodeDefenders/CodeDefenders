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
package org.codedefenders.servlets.admin;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.creategames.AdminCreateGamesBean;
import org.codedefenders.beans.creategames.CreateGamesBean;
import org.codedefenders.service.CreateGamesService;
import org.codedefenders.servlets.creategames.CreateGamesServlet;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;


@WebServlet(urlPatterns = {Paths.ADMIN_PAGE, Paths.ADMIN_GAMES})
public class AdminCreateGames extends CreateGamesServlet {
    private AdminCreateGamesBean createGamesBean;

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private CreateGamesService createGamesService;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        createGamesBean = createGamesService.getContextForAdmin(login.getUserId());
        synchronized (createGamesBean.getSynchronizer()) {
            request.setAttribute("createGamesBean", createGamesBean);
            request.getRequestDispatcher(Constants.ADMIN_GAMES_JSP).forward(request, response);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        createGamesBean = createGamesService.getContextForAdmin(login.getUserId());
        super.doPost(request, response);

    }

    @Override
    protected CreateGamesBean getContext() {
        return createGamesBean;
    }
}
