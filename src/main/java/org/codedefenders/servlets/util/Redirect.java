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

        } else if (referer.startsWith("http")) {
            logger.debug("Redirecting back to absolute URL " + referer);
            response.sendRedirect(referer);

        } else {
            logger.debug("Redirecting back to relative URL " + contextPath + "/" + referer);
            redirectTo(request, response, referer);
        }
    }

    /**
     * Redirect to the provided target page.
     * The target page can have three possible formats:
     * <ul>
     *     <li>Relative path with leading slash: e.g.: <code>/games/overview</code></li>
     *     <li>Relative path without leading slash: e.g.: <code>games/overview</code></li>
     *     <li>
     *         Absolute path with leading slash: e.g.: <code>/codedefenders/games/overview</code>
     *         (with <code>/codedefenders</code> as the context path)
     *     </li>
     * </ul>
     */
    public static void redirectTo(HttpServletRequest request, HttpServletResponse response, String target) throws IOException {
        // TODO: Should we check this is indeed a valid URL from Paths, we cannot allow to redirect to any web page out there, can't we?
        String contextPath = request.getContextPath();

        if (target == null) {
            logger.debug("Target is null, redirecting back to " + Paths.LANDING_PAGE);
            response.sendRedirect(request.getContextPath() + Paths.LANDING_PAGE);

        // This could fail if the context path is also the name of a relative path within the context path.
        // E.g. if the context path is "/games".
        } else if (!contextPath.isEmpty() && target.startsWith(contextPath)) {
            logger.debug("Redirecting to absolute URL " + target);
            response.sendRedirect(target);

        } else {
            String redirectUrl = target.startsWith("/")
                    ? contextPath + target
                    : contextPath + "/" + target;
            logger.debug("Redirecting to relative URL " + redirectUrl);
            response.sendRedirect(redirectUrl);
        }
    }
}
