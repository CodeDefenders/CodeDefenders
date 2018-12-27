package org.codedefenders.servlets.util;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

/**
 * This class offers static methods, which offer functionality useful for {@link HttpServlet} implementations.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 */
public final class ServletUtils {
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
     * Extracts a given URL parameter from a given request.
     * <p>
     * If the parameter is no valid integer value, the method returns {@code null}.
     *
     * @param request   the request, which the parameter is extracted from.
     * @param parameter the given URL parameter.
     * @return a valid integer extracted for the parameter of the given request, or {@code null}.
     */
    public static Integer getIntParameter(HttpServletRequest request, String parameter) {
        final Integer number;

        final String parameterString = request.getParameter(parameter);
        if (parameterString == null) {
            return null;
        }
        try {
            number = Integer.parseInt(parameterString);
        } catch (NumberFormatException e) {
            return null;
        }
        return number;
    }

    /**
     * Extracts a given URL parameter from a given request.
     * <p>
     * If the parameter is not a valid string value, the method returns {@code null}.
     *
     * @param request   the request, which the parameter is extracted from.
     * @param parameter the given URL parameter.
     * @return a string extracted for the parameter of the given request, or {@code null}.
     */
    public static String getStringParameter(HttpServletRequest request, String parameter) {
        final String string = request.getParameter(parameter);
        if (string == null || string.isEmpty()) {
            return null;
        }
        return string;
    }
}
