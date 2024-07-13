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

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;

/**
 * This {@link HttpServlet} handles admin management of puzzles.
 *
 * <p>{@code GET} requests redirect to the admin puzzle management page.
 * and {@code POST} requests handle puzzle related management.
 *
 * <p>Serves under {@code /admin/puzzles} and {@code /admin/puzzles/management}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet({Paths.ADMIN_PUZZLE_OVERVIEW, Paths.ADMIN_PUZZLE_MANAGEMENT})
public class AdminPuzzleManagement extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher(Constants.ADMIN_PUZZLE_MANAGEMENT_JSP).forward(request, response);
    }
}
