package org.codedefenders;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
