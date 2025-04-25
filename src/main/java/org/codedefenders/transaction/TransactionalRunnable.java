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
package org.codedefenders.transaction;

import java.sql.SQLException;

/**
 * This is a functional interface for executing code, which can throw exceptions, within a transaction.
 *
 * @author degenhart
 */
@FunctionalInterface
public interface TransactionalRunnable {

    // TODO: Should we limit the thrown exceptions?!

    /**
     * Execute this function within a {@link Transaction}.
     *
     * @param transaction The transaction active for the code execution.
     * @throws SQLException if a database access error occurs
     */
    void executeInTransaction(Transaction transaction) throws Exception;
}
