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
