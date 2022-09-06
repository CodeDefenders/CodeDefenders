package org.codedefenders.transaction;

import java.sql.SQLException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
 * {@link #startTransaction()} and {@link #executeInTransaction(TransactionalExecution)}
 *
 * <p>In most cases {@link #executeInTransaction(TransactionalExecution)} should be used instead of
 * {@link #startTransaction()}.<br>
 * The only advantage the later one provides, is the ability to spread the start and the end (either 'commit' or
 * 'rollback') over multiple methods. This is strongly discouraged, because it will be very hard to ensure every started
 * {@link Transaction} (and the underlying {@link java.sql.Connection}) is closed.
 *
 * @author degenhart
 * @see Transaction
 * @see TransactionalExecution
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
     * {@link TransactionalExecution} {@code execution} with the current {@link Transaction} as parameter.
     *
     * <p>The transaction will automatically be aborted before this method call ends and if it was not committed
     * already. E.g. if {@code execution} throws any {@link Exception}.
     *
     * @param execution The code to execute within the current transaction.
     * @throws Exception if a {@code Exception} occurred in the {@code execution}
     */
    <T> T executeInTransaction(@Nonnull TransactionalExecution<T> execution) throws Exception;

    <T> T executeInTransaction(@Nonnull TransactionalExecution<T> execution,
            @Nullable Integer transactionIsolation) throws Exception;
}
