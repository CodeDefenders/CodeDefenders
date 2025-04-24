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
package org.codedefenders.servlets.util;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
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
        URLUtils url = CDIUtil.getBeanFromCDI(URLUtils.class);

        String referer = request.getHeader("referer");

        if (referer == null) {
            logger.debug("Header does not specify a referer, redirecting back to " + Paths.LANDING_PAGE);
            response.sendRedirect(url.forPath(Paths.LANDING_PAGE));
        } else {
            logger.debug("Redirecting back to absolute URL " + referer);
            response.sendRedirect(referer);
        }
    }
}
