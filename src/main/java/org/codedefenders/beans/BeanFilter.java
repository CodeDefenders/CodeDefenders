package org.codedefenders.beans;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.codedefenders.beans.game.PreviousSubmissionBean;
import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.servlets.util.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds various beans to the request, so they can be shared between the
 * application and the JSPs.
 */
/*
 * This filter should only be required until we change our JSPs to use the JSP
 * tag libraries, since the tags can access the session beans directly, without
 * this extra step of adding them to the request.
 */
@WebFilter(filterName = "BeanFilter")
public class BeanFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(BeanFilter.class);

    @Inject
    private LoginBean login;

    @Inject
    private PreviousSubmissionBean previousSubmission;

    @Inject
    private GameProducer gameProducer;

    @Override
    public void init(FilterConfig config) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setAttribute("login", login);
        request.setAttribute("previousSubmission", previousSubmission);

        // Configure the GameProducer with the game associated to this request if any
        // TODO This is a bit too generous, we should not consider /css/, /webjars/, and probably other 
        // requests
        if (request instanceof HttpServletRequest) {

            Optional<Integer> possiblyGameId = ServletUtils.gameId((HttpServletRequest) request);
            if( possiblyGameId.isPresent() ) {
                possiblyGameId.ifPresent(integer -> gameProducer.setTheGame(integer));
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
