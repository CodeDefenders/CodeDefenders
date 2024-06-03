package org.codedefenders;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Redirects requests that
 * <ul>
     * <li>have exactly one trailing slash and</li>
     * <li>would be redirected to the 404 page</li>
 * </ul>
 * by removing the trailing slash.
 */
public class PathFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String servletName = httpRequest.getHttpServletMapping().getServletName();

            if (servletName.equals("default")) {
                String uri = httpRequest.getRequestURI();
                String queryString = httpRequest.getQueryString();

                if (uri.endsWith("/") && !uri.endsWith("//")) {
                    uri = uri.substring(0, uri.length() - 1);
                    if (queryString != null) {
                        httpResponse.sendRedirect(uri + '?' + queryString);
                    } else {
                        httpResponse.sendRedirect(uri);
                    }
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }
}
