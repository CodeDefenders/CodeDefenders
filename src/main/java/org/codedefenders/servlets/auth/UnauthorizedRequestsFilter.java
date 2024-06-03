package org.codedefenders.servlets.auth;

import java.io.IOException;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
