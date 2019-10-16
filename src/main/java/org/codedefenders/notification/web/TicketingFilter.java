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

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.codedefenders.notification.ITicketingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TicketingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(TicketingFilter.class);

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
        HttpSession session = httpReq.getSession();

        /*
         * Either this request is an HTTP request AND has a valid userId, or its
         * a WS request AND had a valid ticket
         */
        if (!"ws".equalsIgnoreCase(request.getProtocol())) {
            Integer userId = (Integer) session.getAttribute("uid");
            if (userId != null && requiresTicket(httpReq)) {
                /*
                 * This is a valid HTTP request from LoginFilter, we decorate it
                 * with a new ticket
                 */
                String ticket = ticketingService.generateTicketForOwner(userId);
                request.setAttribute(TICKET_REQUEST_ATTRIBUTE_NAME, ticket);

                logger.debug("Registering toket {} for {} from {}.", ticket, userId, httpReq.getRequestURI());

                chain.doFilter(request, response);
            } else {
                /*
                 * This might require some love. All the HTTP requests for
                 * resources which does not require validation pass by this
                 * statement
                 */
                chain.doFilter(request, response);
            }
        } else {
            /*
             * This is a WS request, we validate its ticket inside PushSocket
             * since I have no idea how to properly close that here we an error
             * message without tampering with the output stream TODO Consider
             * using https://tomcat.apache.org/tomcat-8.0-doc/config/filter.html
             */
            // if (isValidWsRequest(httpReq)) {
            chain.doFilter(request, response);
            // } else {
            /*
             * DO not propagate WS/HTTP Code (see [1]) but close the connection
             * with a custom code instead
             */
            // request.setAttribute(name, o);
            // }
        }

    }

    // private boolean isValidWsRequest(HttpServletRequest request) {
    // try {
    // //
    // ws://localhost:8080/notifications/3febb2d3-f1a5-45ff-8cf4-106f49e5e2d2/100
    // String[] tokens = request.getRequestURL().toString().split("/");
    // System.out.println("TicketingFilter.isValidWsRequest() " +
    // Arrays.toString( tokens ));
    //
    // String ticket = tokens[tokens.length-2];
    // Integer owner = Integer.valueOf( tokens[tokens.length-1] );
    // System.out.println("TicketingFilter.isValidWsRequest() " + ticket );
    // System.out.println("TicketingFilter.isValidWsRequest() " + owner);
    //
    // return ticketingService.validateTicket(ticket, owner);
    // } catch (Throwable t) {
    // // Request is invalid.
    // return false;
    // }
    //
    // }

    private boolean requiresTicket(HttpServletRequest httpReq) {
        String path = httpReq.getRequestURI();
        String context = httpReq.getContextPath();
        /*
         * List here the pages which require the web socket !
         */
        if (path.startsWith(context + "/multiplayergame")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void destroy() {
    }

}
