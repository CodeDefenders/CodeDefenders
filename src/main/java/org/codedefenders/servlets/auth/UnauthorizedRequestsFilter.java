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
package org.codedefenders.servlets.auth;

import java.io.IOException;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.shiro.authz.UnauthorizedException;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.client.Counter;

/**
 * Handles unauthorized requests after they have been denied.
 * <p>Logs the request and the exception and redirects to the error page.</p>
 */
@ApplicationScoped
public class UnauthorizedRequestsFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(UnauthorizedRequestsFilter.class);

    @Inject
    private CodeDefendersAuth login;

    private static final Counter unautorizedRequestsCounter = Counter.build()
            .name("unauthorized_requests")
            .help("The number of unauthorized requests handled by UnauthorizedRequestFilter.")
            .register();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        try {
            filterChain.doFilter(request, response);
        } catch (UnauthorizedException exception) {
            unautorizedRequestsCounter.inc();
            logUnauthorizedAccess(request, exception);
            handleDeniedRequest(request, response);
        }
    }

    /**
     * Logs the request details: user, HTTP method, path, action, exception message.
     */
    private void logUnauthorizedAccess(ServletRequest request, UnauthorizedException exception) {
        String userString = login.isLoggedIn()
                ? "user " + login.getUserId()
                : "unauthenticated user";

        if (request instanceof HttpServletRequest httpRequest) {
            String actionString = ServletUtils.getStringParameter(httpRequest, "action")
                    .map(a -> " with action [" + a + "]")
                    .orElse("");

            logger.info("Unauthorized {} request from {} for {}{}{}: {}",
                    httpRequest.getMethod(),
                    userString,
                    httpRequest.getServletPath(),
                    Objects.requireNonNullElse(httpRequest.getPathInfo(), ""),
                    actionString,
                    exception.getMessage());
        } else {
            logger.info("Unauthorized request from {}: {}",
                    userString,
                    exception.getMessage());
        }
    }

    /**
     * Sets the status code and serves the error page.
     * <p>If the response is already committed, no action is taken.</p>
     * <p>If the request is an API request, only the status code is set.</p>
     */
    private void handleDeniedRequest(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
        }

        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setStatus(403);
        }

        if (request instanceof HttpServletRequest httpRequest) {
            String path = httpRequest.getServletPath();
            if (!path.startsWith("/api/")) {
                request.getRequestDispatcher(Constants.ERROR_403_JSP).forward(request, response);
            }
        }
    }
}
