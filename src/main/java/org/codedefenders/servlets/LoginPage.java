/*
 * Copyright (C) 2020 Code Defenders contributors
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

package org.codedefenders.servlets;

import java.io.IOException;
import java.util.Objects;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.page.PageInfoBean;
import org.codedefenders.util.Paths;

@WebServlet("/login")
public class LoginPage extends HttpServlet {

    @Inject
    CodeDefendersAuth login;

    @Inject
    PageInfoBean pageInfo;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (login.isLoggedIn()) {
            resp.sendRedirect(!Objects.isNull(req.getParameter("nextUrl")) ? req.getParameter("nextUrl") :
                    req.getContextPath() + Paths.GAMES_OVERVIEW);
        } else {
            pageInfo.setPageTitle("Login");

            req.getRequestDispatcher("/jsp/login_view.jsp").forward(req, resp);
        }
    }
}
