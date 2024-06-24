/*
 * Copyright (C) 2022 Code Defenders contributors
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

import java.io.IOException;
import java.sql.SQLException;

import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.transaction.TransactionManager;

@WebFilter(filterName = "TransactionCleanupFilter")
public class TransactionCleanupFilter implements Filter {

    @Inject
    TransactionManager transactionManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        chain.doFilter(request, response);
        // At the end of the request terminate any still running transactions.
        try {
            transactionManager.terminateTransaction();
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }
    }
}
