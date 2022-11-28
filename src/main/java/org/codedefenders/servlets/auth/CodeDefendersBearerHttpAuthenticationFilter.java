/*
 * Copyright (C) 2022 Code Defenders contributors
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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.BearerHttpAuthenticationFilter;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.codedefenders.auth.CodeDefendersRealm;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.service.UserService;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;

/**
 * This filter performs the login with the form data submitted via a HTTP POST request to the {@code /login} url.
 *
 * <p>The whole authentication logic is handled silently by the parent class {@link FormAuthenticationFilter} which
 * performs a login against the {@link CodeDefendersRealm} with the credentials
 * found in the {@code username} and {@code password} HTML parameters of the POST request.
 */
@Singleton
public class CodeDefendersBearerHttpAuthenticationFilter extends BearerHttpAuthenticationFilter {
    private static final Logger logger = LoggerFactory.getLogger(CodeDefendersBearerHttpAuthenticationFilter.class);

    private final MessagesBean messages;
    private final UserService userService;

    @Inject
    public CodeDefendersBearerHttpAuthenticationFilter(MessagesBean messages, UserService userService) {
        super();

        this.messages = messages;
        this.userService = userService;

        // org.codedefenders.util.Paths.LOGIN = "/login";
        this.setLoginUrl(Paths.LOGIN);
        // Go to game overview page after successful login
        this.setSuccessUrl(Paths.GAMES_OVERVIEW);
    }

    @Override
    protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request,
                                     ServletResponse response) throws Exception {
        // Make sure that the session and the like are correctly configured

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        final int userId = subject.getPrincipals().oneByType(CodeDefendersRealm.LocalUserId.class).getUserId();
        final String ipAddress = getClientIpAddress(httpRequest);

        // Log user activity including the timestamp
        userService.recordSession(userId, ipAddress);
        logger.info("Successful login for username '{}' from ip {}", token.getPrincipal(), ipAddress);

        // Call the super method, as this is the one doing the redirect after a successful login.
        return super.onLoginSuccess(token, subject, request, response);
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request,
                                     ServletResponse response) {

        messages.add("Username not found or password incorrect.");

        if (request instanceof HttpServletRequest
                && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            final String ipAddress = getClientIpAddress(httpRequest);

            if (e instanceof IncorrectCredentialsException) {
                logger.warn("Failed login with wrong password for username '{}' from ip {}", token.getPrincipal(), ipAddress);
            } else if (e instanceof UnknownAccountException) {
                logger.warn("Failed login for non-existing username '{}' from ip {}", token.getPrincipal(), ipAddress);
            } else {
                logger.warn("Failed login for username '{}' from ip {}", token.getPrincipal(), ipAddress);
            }
        }

        return false;
    }

    @Override
    protected void saveRequest(ServletRequest request) {
        if (request instanceof HttpServletRequest) {
            String httpRequestURI = ((HttpServletRequest) request).getRequestURI();
            // Don't save request in this case because otherwise user is redirected to an API url on successful login
            if (httpRequestURI.startsWith("/api/")) {
                return;
            }
        }
        super.saveRequest(request);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (invalidIP(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (invalidIP(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (invalidIP(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (invalidIP(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (invalidIP(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private boolean invalidIP(String ip) {
        //noinspection UnstableApiUsage
        return (ip == null)
                || (ip.length() == 0)
                || ("unknown".equalsIgnoreCase(ip))
                || ("0:0:0:0:0:0:0:1".equals(ip))
                || !InetAddresses.isInetAddress(ip);
    }
}
