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
package org.codedefenders.util;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;


public class JspWorkaround {
    /**
     * This is intended as a workaround until we get scriptlets out of the last pages.
     *
     * <p>Sets the title (title) and JSP page (jspPage) in the request, and redirects to a wrapper page.
     * This allows us to include pages with scriptlets without having to create a wrapper for each one.
     */
    public static void forwardInWrapper(ServletRequest request, ServletResponse response,
                                        String title, String jspPage) throws ServletException, IOException {
        request.setAttribute("title", title);
        request.setAttribute("jspPage", jspPage);
        RequestDispatcher dispatcher = request.getRequestDispatcher("/jsp/main_page_wrapper.jsp");
        dispatcher.forward(request, response);
    }
}
