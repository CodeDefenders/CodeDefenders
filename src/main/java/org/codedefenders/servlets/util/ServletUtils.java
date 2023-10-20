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

import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

/**
 * This class offers static methods, which offer functionality useful for {@link HttpServlet} implementations.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
public final class ServletUtils {

    private ServletUtils() {
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
        if (formType.isEmpty()) {
            throw new IllegalStateException("Could not retrieve 'formType' from request."
                    + "Aborting request. Making sure the request parameters are set correctly.");
        }
        return formType.get();
    }

    /**
     * Extracts the {@code gameId} URL parameter from a given request.
     *
     * <p>If {@code gameId} is no valid integer value, the method returns {@link Optional#empty()}.
     *
     * @param request the request, which {@code gameId} is extracted from.
     * @return a valid integer extracted from the {@code gameId} parameter of the given request wrapped in an
     *     {@link Optional}, or {@link Optional#empty()}.
     * @see ServletUtils#getIntParameter(HttpServletRequest, String)
     */
    public static Optional<Integer> gameId(HttpServletRequest request) {
        return getIntParameter(request, "gameId");
    }

    /**
     * Extracts a given integer URL parameter from a given request.
     *
     * <p>If the parameter is no valid integer value, the method returns an empty {@link Optional}.
     *
     * @param request   the request, which the parameter is extracted from.
     * @param parameter the given URL parameter.
     * @return a valid integer extracted for the parameter of the given request wrapped in an {@link Optional},
     *     or {@link Optional#empty()}.
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
     *
     * <p>If the parameter is no valid float value, the method returns an empty {@link Optional}.
     *
     * @param request   the request, which the parameter is extracted from.
     * @param parameter the given URL parameter.
     * @return a valid float extracted for the parameter of the given request wrapped in an {@link Optional},
     *     or {@link Optional#empty()}.
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
     *
     * <p>If the parameter is not a valid string value, the method returns an empty {@link Optional}.
     *
     * @param request   the request, which the parameter is extracted from.
     * @param parameter the given URL parameter.
     * @return a string extracted for the parameter of the given request wrapped in an {@link Optional},
     *     or {@link Optional#empty()}.
     */
    public static Optional<String> getStringParameter(HttpServletRequest request, String parameter) {
        return Optional.ofNullable(request.getParameter(parameter)).filter(s -> !s.isEmpty());
    }

    /**
     * Extracts a given enum URL parameter from a given request.
     *
     * <p>If the parameter is not a valid value for the given enum, the method returns an empty {@link Optional}.
     *
     * @param request   the request, which the parameter is extracted from.
     * @param enumClass the given enum type.
     * @param parameter the given URL parameter.
     * @return an enum value for the request parameter
     */
    public static <T extends Enum<T>> Optional<T> getEnumParameter(
            HttpServletRequest request, Class<T> enumClass, String parameter) {
        return Optional.ofNullable(request.getParameter(parameter)).map(s -> {
            try {
                return Enum.valueOf(enumClass, s);
            } catch (IllegalArgumentException e) {
                return null;
            }
        });
    }

    /**
     * Extracts a given UUID URL parameter from a given request.
     *
     * <p>If the parameter is no valid UUID value, the method returns an empty {@link Optional}.
     *
     * @param request   the request, which the parameter is extracted from.
     * @param parameter the given URL parameter.
     * @return a valid UUID extracted for the parameter of the given request wrapped in an {@link Optional},
     *     or {@link Optional#empty()}.
     */
    public static Optional<UUID> getUUIDParameter(HttpServletRequest request, String parameter) {
        return Optional.ofNullable(request.getParameter(parameter)).map(s -> {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                return null;
            }
        });
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
