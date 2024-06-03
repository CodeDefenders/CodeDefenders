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
package org.codedefenders.notification.web;

import java.io.IOException;
import java.util.stream.Stream;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.notification.ITicketingService;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

@WebFilter(filterName = "TicketingFilter")
public class TicketingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(TicketingFilter.class);

    @Inject
    private CodeDefendersAuth login;

    public static final String TICKET_REQUEST_ATTRIBUTE_NAME = "notification-ticket";

    @Inject
    private ITicketingService ticketingService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;

        /*
         * Either this request is an HTTP request AND the session has a valid userId,
         * or its a WS request AND had a valid ticket
         */

        /*
         * This is a WS request, we validate its ticket inside PushSocket
         * since I have no idea how to properly close that here we an error
         * message without tampering with the output stream TODO Consider
         * using https://tomcat.apache.org/tomcat-8.0-doc/config/filter.html
         */
        if ("ws".equalsIgnoreCase(request.getProtocol())) {
            if (!isValidWsRequest(httpReq)) {
                chain.doFilter(request, response);
            }
        } else {
            if (requiresTicket(httpReq)) {
                if (login.isLoggedIn()) {
                    /*
                     * This is a valid HTTP request from LoginFilter, we decorate it with a new ticket.
                     */
                    String ticket = ticketingService.generateTicketForOwner(login.getUserId());
                    request.setAttribute(TICKET_REQUEST_ATTRIBUTE_NAME, ticket);
                    logger.debug("Registering ticket " + ticket + " for user " + login.getUserId()
                            + " from " + httpReq.getRequestURI());
                }
            }
            chain.doFilter(request, response);
        }
    }

    private boolean isValidWsRequest(HttpServletRequest request) {
        try {
            String[] tokens = request.getRequestURL().toString().split("/");
            String ticket = tokens[tokens.length - 2];
            int userId = Integer.parseInt(tokens[tokens.length - 1]);
            if (!ticketingService.validateTicket(ticket, userId)) {
                logger.warn("Ticket " + ticket + " for user " + userId + " is not valid.");
                return false;
            } else {
                return true;
            }
        } catch (IndexOutOfBoundsException | NumberFormatException | NullPointerException e) {
            logger.warn("Invalid WebSocket URL.", e);
            return false;
        }
    }

    private boolean requiresTicket(HttpServletRequest httpReq) {
        String servletPath = httpReq.getServletPath();
        /*
         * List here the pages which require the web socket!
         */
        return Stream.of(Paths.BATTLEGROUND_GAME, Paths.MELEE_GAME, Paths.PUZZLE_GAME,
                        Paths.BATTLEGROUND_HISTORY, Paths.MELEE_HISTORY)
                .anyMatch(servletPath::startsWith);
    }

    @Override
    public void destroy() {
    }

}
