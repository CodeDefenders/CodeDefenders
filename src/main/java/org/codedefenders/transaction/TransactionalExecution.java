package org.codedefenders.transaction;

import java.sql.SQLException;

/**
 * This is a functional interface for executing code, which can throw exceptions, within a transaction.
 *
 * @author degenhart
 */
@FunctionalInterface
public interface TransactionalExecution<T> {

    // TODO: Should we limit the thrown exceptions?!

    /**
     * Execute this function within a {@link Transaction}.
     *
     * @param transaction The transaction active for the code execution.
     * @throws SQLException if a database access error occurs
     */
    T executeInTransaction(Transaction transaction) throws Exception;
}
