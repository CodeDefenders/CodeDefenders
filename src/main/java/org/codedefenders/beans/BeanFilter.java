package org.codedefenders.beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

/**
 * Adds various request-scoped beans to the request, so they can be shared between the application and ths JSPs.
 */
@WebFilter(filterName = "BeanFilter")
public class BeanFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(BeanFilter.class);

    @Inject
    MessageBean messages;

    @Override
    public void init(FilterConfig config) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        request.setAttribute("messages", messages);
    }

    @Override
    public void destroy() {

    }
}
