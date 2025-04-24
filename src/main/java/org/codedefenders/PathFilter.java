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
package org.codedefenders;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Redirects requests that
 * <ul>
     * <li>have exactly one trailing slash and</li>
     * <li>would be redirected to the 404 page</li>
 * </ul>
 * by removing the trailing slash.
 */
public class PathFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String servletName = httpRequest.getHttpServletMapping().getServletName();

            if (servletName.equals("default")) {
                String uri = httpRequest.getRequestURI();
                String queryString = httpRequest.getQueryString();

                if (uri.endsWith("/") && !uri.endsWith("//")) {
                    uri = uri.substring(0, uri.length() - 1);
                    if (queryString != null) {
                        httpResponse.sendRedirect(uri + '?' + queryString);
                    } else {
                        httpResponse.sendRedirect(uri);
                    }
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }
}
