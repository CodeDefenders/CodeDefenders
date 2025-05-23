/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
package org.codedefenders.beans;

import java.io.IOException;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.game.PreviousSubmissionBean;
import org.codedefenders.beans.page.PageInfoBean;
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
    private CodeDefendersAuth login;

    @Inject
    private PreviousSubmissionBean previousSubmission;

    @Inject
    private GameProducer gameProducer;

    @Inject
    private PageInfoBean pageInfo;

    @Override
    public void init(FilterConfig config) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setAttribute("login", login);
        request.setAttribute("previousSubmission", previousSubmission);
        request.setAttribute("pageInfo", pageInfo);

        // Configure the GameProducer with the game associated to this request if any
        // TODO This is a bit too generous, we should not consider /css/, /webjars/, and probably other requests
        if (request instanceof HttpServletRequest) {
            Optional<Integer> optGameId = ServletUtils.gameId((HttpServletRequest) request);
            optGameId.ifPresent(gameProducer::setGameId);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
