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
package org.codedefenders.servlets.util;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Redirect {
    private static final Logger logger = LoggerFactory.getLogger(Redirect.class);

    private Redirect() {
    }

    /**
     * Redirect back to the referer, or to the start page if the header does not specify a referer.
     */
    public static void redirectBack(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String referer = request.getHeader("referer");
        String contextPath = request.getContextPath();

        if (referer == null) {
            logger.debug("Header does not specify a referer, redirecting back to " + Paths.LANDING_PAGE);
            redirectTo(request, response, Paths.LANDING_PAGE);

        } else if (referer.contains(contextPath)) {
            logger.debug("Redirecting back to absolute URL " + referer);
            redirectTo(request, response, referer);

        } else {
            logger.debug("Redirecting back to relative URL " + contextPath + "/" + referer);
            redirectTo(request, response, contextPath + "/" + referer);
        }
    }
    
    /**
     * Redirect to the provided target page
     */
    public static void redirectTo(HttpServletRequest request, HttpServletResponse response, String target) throws IOException {
        // TODO: Should we check this is indeed a valid URL from Paths, we cannot allow to redirect to any web page out there, can't we?
        String contextPath = request.getContextPath();

        if (target == null) {
            logger.debug("Header does not specify a target, redirecting back to " + Paths.LANDING_PAGE);
            response.sendRedirect(Paths.LANDING_PAGE);

        } else if (target.startsWith(contextPath)) {
            logger.debug("Redirecting back to absolute URL " + target);
            response.sendRedirect(target);

        } else {
            logger.debug("Redirecting back to relative URL " + contextPath + "/" + target);
            response.sendRedirect(contextPath + "/" + target);
        }
    }
}
