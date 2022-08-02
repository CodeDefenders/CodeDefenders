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

package org.codedefenders.servlets.auth;

import java.io.IOException;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.beans.user.LoginBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prevents caching of private data and user specific HTML header elements by disabling cache
 * for all requests - except resources - if a user is logged in.
 */
@WebFilter(urlPatterns = "/*")
public class CacheControlFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(CacheControlFilter.class);

    @Inject
    LoginBean login;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest httpReq = (HttpServletRequest) request;
        final HttpServletResponse httpRes = (HttpServletResponse) response;

        /* All resources are public and can be always cached, independent of the login status. */
        final boolean isResource = Stream
                .of("images", "webjars", "css", "js")
                .anyMatch(uri -> httpReq.getServletPath().startsWith("/" + uri));

        final boolean isLoggedIn = login.isLoggedIn() && login.getUser().isActive();

        if (!isResource && isLoggedIn) {
            disableCache(httpRes);
            logger.debug("Disabled cache for: " + httpReq.getRequestURI());
        }

        chain.doFilter(request, response);
    }

    private void disableCache(HttpServletResponse httpResponse) {
        /*
         * Disable caching in the HTTP header.
         * (https://stackoverflow.com/questions/13640109/how-to-prevent-browser-cache-for-php-site)
         */
        httpResponse.setHeader("Pragma", "No-cache");
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        httpResponse.setDateHeader("Expires", -1);
    }
}
