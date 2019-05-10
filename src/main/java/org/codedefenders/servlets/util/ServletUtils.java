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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

/**
 * This class offers static methods, which offer functionality useful for {@link HttpServlet} implementations.
 *
 * @author <a href=https://github.com/werli>Phil Werli</a>
 */
public final class ServletUtils {
    private static final Logger logger = LoggerFactory.getLogger(ServletUtils.class);
    private ServletUtils() {
    }

    /**
     * Returns the context path a of a given {@link HttpServletRequest}.
     * <p>
     * {@code ctx(request)} can be used as a shorter version of {@code request.getContextPath()}
     *
     * @param request the request the context is retrieved from.
     * @return the application context.
     */
    public static String ctx(HttpServletRequest request) {
        return request.getContextPath();
    }

    /**
     * Returns the base URL from a given request.
     *
     * @param request the request the URL is retrieved from
     * @return the base URL as a {@link String} or {@code null} if no base URL could be retrieved.
     */
    public static String getBaseURL(HttpServletRequest request) {
        String baseURL = null;
        try {
            baseURL = new URL(request.getScheme(),
                    request.getServerName(),
                    request.getServerPort(),
                    request.getContextPath()).toString();
        } catch (MalformedURLException ignored) {
            logger.error("Could not retrieve base URL from request.");
        }
        return baseURL;
    }

    /**
     * Extracts the user from the current {@link javax.servlet.http.HttpSession} of the given request.
     *
     * @param request the request, which the session is extracted from.
     * @return a valid integer extracted from the session of the request.
     * @throws IllegalStateException if no user can be extracted from the session.
     */
    public static int userId(HttpServletRequest request) {
        final Integer userId = (Integer) request.getSession().getAttribute("uid");
        if (userId == null) {
            throw new IllegalStateException("Could not retrieve userId from session.");
        }
        return userId;
    }

    /**
     * Extracts the formType action from the given request.
     *
     * @param request the request, which the formType action string is extracted from.
     * @return a valid string extracted from the request, never {@link null} or empty.
     * @throws IllegalStateException if no action can be extracted from the request.
     */
    public static String formType(HttpServletRequest request) {
        final Optional<String> formType = getStringParameter(request, "formType");
        if (!formType.isPresent()) {
            throw new IllegalStateException("Could not retrieve 'formType' from request. Aborting request. Making sure the request parameters are set correctly.");
        }
        return formType.get();
    }

    /**
     * Extracts the {@code gameId} URL parameter from a given request.
     * <p>
     * If {@code gameId} is no valid integer value, the method returns {@code null}.
     *
     * @param request the request, which {@code gameId} is extracted from.
     * @return a valid integer extracted from the {@code gameId} parameter of the given request wrapped in an {@link Optional}, or {@link Optional#empty()}.
     * @see ServletUtils#getIntParameter(HttpServletRequest, String)
     */
    public static Optional<Integer> gameId(HttpServletRequest request) {
        return getIntParameter(request, "gameId");
    }

    /**
     * Extracts a given integer URL parameter from a given request.
     * <p>
     * If the parameter is no valid integer value, the method returns an empty {@link Optional}.
     *
     * @param request   the request, which the parameter is extracted from.
     * @param parameter the given URL parameter.
     * @return a valid integer extracted for the parameter of the given request wrapped in an {@link Optional}, or {@link Optional#empty()}.
     */
    public static Optional<Integer> getIntParameter(HttpServletRequest request, String parameter) {
        return Optional.ofNullable(request.getParameter(parameter)).map(s -> {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return null;
            }
        });
    }

    /**
     * Extracts a given float URL parameter from a given request.
     * <p>
     * If the parameter is no valid float value, the method returns an empty {@link Optional}.
     *
     * @param request   the request, which the parameter is extracted from.
     * @param parameter the given URL parameter.
     * @return a valid float extracted for the parameter of the given request wrapped in an {@link Optional}, or {@link Optional#empty()}.
     */
    public static Optional<Float> getFloatParameter(HttpServletRequest request, String parameter) {
        return Optional.ofNullable(request.getParameter(parameter)).map(s -> {
            try {
                return Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return null;
            }
        });
    }

    /**
     * Extracts a given URL parameter from a given request.
     * <p>
     * If the parameter is not a valid string value, the method returns an empty {@link Optional}.
     *
     * @param request   the request, which the parameter is extracted from.
     * @param parameter the given URL parameter.
     * @return a string extracted for the parameter of the given request wrapped in an {@link Optional}, or {@link Optional#empty()}.
     */
    public static Optional<String> getStringParameter(HttpServletRequest request, String parameter) {
        return Optional.ofNullable(request.getParameter(parameter)).filter(s -> !s.isEmpty());
    }

    /**
     * Checks whether a given URL parameter can be extracted from a given request.
     * If the parameter can be extracted, returns a given {@code then} value.
     * Otherwise return {@code other}.
     *
     * @param request   the request, which the parameter is extracted from.
     * @param parameter the given URL parameter.
     * @param then      the value that is returned when the request does contain the given parameter.
     * @param other     the value that is returned when the request does not contain the given parameter.
     * @param <T>       the type of {@code then} and {@code other} parameters and the return type.
     * @return {@code then} if the given parameter can be extracted from the request, {@code other} otherwise.
     */
    public static <T> T parameterThenOrOther(HttpServletRequest request, String parameter, T then, T other) {
        return Optional.ofNullable(request.getParameter(parameter)).map(s -> then).orElse(other);
    }
}
