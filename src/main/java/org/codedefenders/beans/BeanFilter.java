package org.codedefenders.beans;

import org.codedefenders.beans.game.PreviousSubmissionBean;
import org.codedefenders.beans.user.LoginBean;
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
import java.io.IOException;

/**
 * Adds various beans to the request, so they can be shared between the application and ths JSPs.
 */
@WebFilter(filterName = "BeanFilter")
public class BeanFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(BeanFilter.class);

    @Inject
    private LoginBean login;

    @Inject
    private PreviousSubmissionBean previousSubmission;


    @Override
    public void init(FilterConfig config) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setAttribute("login", login);
        request.setAttribute("previousSubmission", previousSubmission);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
