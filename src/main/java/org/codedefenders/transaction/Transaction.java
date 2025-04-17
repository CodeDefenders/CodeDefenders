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
 * Interface which provides methods for committing and closing and thereby aborting a transaction.
 *
 * @author degenhart
 */
public interface Transaction extends AutoCloseable {

    String getTransactionIdentifier();

    /**
     * Commit any changes made in this transaction.
     *
     * @throws SQLException if a database access error occurs
     */
    void commit() throws SQLException;

    /**
     * Closes the underlying connection and aborts the transaction (which will roll back any changes made) if it was not
     * already committed.
     *
     * <p>This method is idempotent. Calling {@code close()} on an already closed connection is a no-op.
     *
     * @throws SQLException if a database access error occurs
     */
    void close() throws SQLException;
}
