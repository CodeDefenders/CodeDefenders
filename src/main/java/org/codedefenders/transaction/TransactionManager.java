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

package org.codedefenders.transaction;

import java.sql.SQLException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A {@link TransactionManager} is a class which provides a standard way to run multiple database query executed in a
 * Thread in the same transaction and decide if the changes should actually be saved to the database (via a 'commit')
 * or discarded (via a 'rollback').
 *
 * <p>SQL statements will be—if no other transaction isolation level is given—executed with the default isolation level
 * of the database/JDBC driver.
 *
 * <p>Nested transactions are realized via savepoint.
 *
 * <p>This interface provides two methods:
 * {@link #startTransaction()} and {@link #executeInTransaction(TransactionalSupplier)}
 *
 * <p>In most cases {@link #executeInTransaction(TransactionalSupplier)} should be used instead of
 * {@link #startTransaction()}.<br>
 * The only advantage the later one provides, is the ability to spread the start and the end (either 'commit' or
 * 'rollback') over multiple methods. This is strongly discouraged, because it will be very hard to ensure every started
 * {@link Transaction} (and the underlying {@link java.sql.Connection}) is closed.
 *
 * @author degenhart
 * @see Transaction
 * @see TransactionalSupplier
 */
public interface TransactionManager {

    /**
     * Begin a new database-{@link Transaction}, specific to the current thread.
     *
     * <p>The returned {@link Transaction} has to be either committed (via {@link Transaction#commit()}) or aborted (via
     * {@link Transaction#close()}).
     *
     * @return An object representing the started transaction.
     * @throws IllegalStateException If there is already a running database-Transaction for the current thread.
     */
    @Nonnull
    Transaction startTransaction();

    @Nonnull
    Transaction startTransaction(@Nullable Integer transactionIsolation);

    /**
     * Forcefully closes a–in the current thread–running transaction.
     *
     * <p>If no transaction is running this is a no-op.
     *
     * <p>This method should only be used to ensure no transaction remains running if a thread will be reused.
     * E.g. in the case of thread pools like the one handling the http requests of the tomcat server.
     *
     * @throws IllegalStateException If a transaction had to be closed.
     * @see org.codedefenders.servlets.util.TransactionCleanupFilter
     */
    void terminateTransaction() throws IllegalStateException, SQLException;

    // TODO: Should this do an automatic commit if no exception was thrown within {@code execution} ?!

    /**
     * This will begin a new database-{@link Transaction} specific to the current thread, and execute the provided
     * {@link TransactionalSupplier} {@code execution} with the current {@link Transaction} as parameter.
     *
     * <p>The transaction will automatically be aborted before this method call ends and if it was not committed
     * already. E.g. if {@code execution} throws any {@link Exception}.
     *
     * @param execution The code to execute within the current transaction.
     * @throws Exception if a {@code Exception} occurred in the {@code execution}
     */
    <T> T executeInTransaction(@Nonnull TransactionalSupplier<T> execution) throws Exception;

    <T> T executeInTransaction(@Nonnull TransactionalSupplier<T> execution,
            @Nullable Integer transactionIsolation) throws Exception;

    void executeInTransaction(@Nonnull TransactionalRunnable execution) throws Exception;

    void executeInTransaction(@Nonnull TransactionalRunnable execution,
            @Nullable Integer transactionIsolation) throws Exception;
}
