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
package org.codedefenders.servlets.auth;

import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Checks if the user is logged in for pages that require login.
 * If the user accesses such a page and is not logged in, they are redirected to the login page.
 * If the user accesses such a page and is logged in, HTTP header fields are set to disable caching.
 */
@WebFilter(filterName = "LoginFilter")
public class LoginFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(LoginFilter.class);

    @Inject
    LoginBean login;

    @Override
    public void init(FilterConfig config) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!loginRequired(httpRequest)) {
            chain.doFilter(request, response);
        } else {
            HttpSession session = httpRequest.getSession();

            if (login.isLoggedIn()) {
                if (login.getUser().isActive()) {
                    /* Disable caching in the HTTP header.
                     * https://stackoverflow.com/questions/13640109/how-to-prevent-browser-cache-for-php-site */
                    httpResponse.setHeader("Pragma", "No-cache");
                    httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                    httpResponse.setDateHeader("Expires", -1);

                    chain.doFilter(request, response);
                } else {
                    session.invalidate();
                    redirectToLogin(httpRequest, response);
                }
            } else {
                redirectToLogin(httpRequest, response);
            }
        }
    }

    @Override
    public void destroy() {
    }

    private boolean loginRequired(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();

        /*
         * Do not authenticate the requests to the WebSocket since the
         * HttpSession is not visible from there. WebSocket authentication shall
         * be handled in a different Filter.
         *
         * TODO Maybe there's a way already to route Ws requests into WsFilters,
         * but for the moment we need to match their URL
         */
        if (path.matches("/notifications/.*/[0-9][0-9]*")) {
            logger.info("LoginFilter.loginRequired() " + request.getProtocol() + " " + path);
            return false;
        }

        if ((path.endsWith(context + "/"))
                || path.endsWith(context + "/favicon.ico")
                || path.endsWith(context + Paths.LOGIN)
                || path.endsWith(context + Paths.HELP_PAGE)
                || path.endsWith(context + "/video")
                || path.endsWith(context + "/video.mp4")
                || path.contains(context + "/papers")
                || path.endsWith(context + Paths.API_SEND_EMAIL)
                || path.endsWith(context + Paths.ABOUT_PAGE)
                || path.endsWith(context + Paths.CONTACT_PAGE)) {
            return false;
        }

        Pattern excludeUrls = Pattern.compile("^.*/(css|js|images|fonts|codemirror)/.*$", Pattern.CASE_INSENSITIVE);
        Matcher m = excludeUrls.matcher(path);
        return !m.matches();
    }

    private void redirectToLogin(HttpServletRequest httpReq, ServletResponse response) throws IOException {
        HttpServletResponse httpResp = (HttpServletResponse) response;
        login.redirectAfterLogin(httpReq.getRequestURI());
        httpResp.sendRedirect(httpReq.getContextPath() + Paths.LOGIN);
    }
}
