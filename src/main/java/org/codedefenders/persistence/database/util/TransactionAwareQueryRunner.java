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

package org.codedefenders.persistence.database.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.dbutils.ResultSetHandler;
import org.codedefenders.database.ConnectionFactory;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.transaction.Transaction;
import org.codedefenders.transaction.TransactionManager;
import org.codedefenders.transaction.TransactionalRunnable;
import org.codedefenders.transaction.TransactionalSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


/**
 * Implements the {@link QueryRunner} and {@link TransactionManager} interfaces, so it can execute database operations
 * within a transaction that is running in the current thread.
 *
 * @author degenhart
 */
// TODO This requires `connectionFactory` to be ThreadSafe. `queryRunner` is ThreadSafe and `transaction`
//  is thread-confined (also ThreadSafe).
@ThreadSafe
@ApplicationScoped
public class TransactionAwareQueryRunner implements TransactionManager, QueryRunner {
    private static final Logger logger = LoggerFactory.getLogger(TransactionAwareQueryRunner.class);

    private final ConnectionFactory connectionFactory;
    private final org.apache.commons.dbutils.QueryRunner queryRunner = new org.apache.commons.dbutils.QueryRunner();
    private final ThreadLocal<ManagedTransaction> transaction = new ThreadLocal<>();

    /**
     * Creates e new TransactionAwareQueryRunner which will retrieve the connections used for transactions and to run
     * execute the SQL statements from the given {@code connectionFactory}.
     *
     * @param connectionFactory The object from which needed connections are acquired
     */
    @Inject
    public TransactionAwareQueryRunner(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public Transaction startTransaction() {
        return startTransaction(null);
    }

    @Override
    @Nonnull
    public Transaction startTransaction(@Nullable Integer transactionIsolation) {
        ManagedTransaction currentTransaction = transaction.get();

        ManagedTransaction tx = null;

        if (currentTransaction != null) {
            try {
                if (transactionIsolation != null
                        && transactionIsolation > currentTransaction.getConnection().getTransactionIsolation()) {
                    throw new IllegalArgumentException("Requested higher transaction isolation as the current running"
                            + "transaction can provide");
                }
            } catch (SQLException e) {
                throw new UncheckedSQLException("Could not query transaction isolation level", e);
            }

            tx = new InnerManagedTransaction(currentTransaction);
        } else {
            tx = new OuterManagedTransaction(getInternalConnection(), transactionIsolation);
        }
        transaction.set(tx);
        MDC.put("tx", tx.getTransactionIdentifier());
        return tx;
    }

    @Override
    public void terminateTransaction() throws SQLException { // TODO(Alex): Handle the SQL Exception?!
        ManagedTransaction currentTransaction = transaction.get();
        if (currentTransaction != null) {
            String txId = currentTransaction.getTransactionIdentifier();
            while (currentTransaction != null) {
                currentTransaction.close();
                currentTransaction = transaction.get();
            }
            throw new IllegalStateException("Forcefully terminated Transaction " + txId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T executeInTransaction(@Nonnull TransactionalSupplier<T> execution) throws Exception {
        return executeInTransaction(execution, null);
    }

    @Override
    public <T> T executeInTransaction(@Nonnull TransactionalSupplier<T> execution,
            @Nullable Integer transactionIsolation) throws Exception {
        try (Transaction transaction = startTransaction(transactionIsolation)) {
            return execution.executeInTransaction(transaction);
        }
    }

    @Override
    public void executeInTransaction(@Nonnull TransactionalRunnable execution) throws Exception {
        executeInTransaction(execution, null);
    }

    @Override
    public void executeInTransaction(@Nonnull TransactionalRunnable execution,
            @Nullable Integer transactionIsolation) throws Exception {
        try (Transaction transaction = startTransaction(transactionIsolation)) {
            execution.executeInTransaction(transaction);
        }
    }

    /**
     * Returns either the connection of the current transaction or a new Connection with {@code AutoCommit} set to true.
     *
     * @return The connection of the transaction or a connection in autocommit mode
     */
    @Nonnull
    private Connection getInternalConnection() {
        ManagedTransaction currentTransaction = transaction.get();
        if (currentTransaction != null) {
            return currentTransaction.getConnection();
        } else {
            try {
                Connection result = connectionFactory.getConnection();
                result.setAutoCommit(true);
                return result;
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            }
        }
    }

    @FunctionalInterface
    private interface SQLAction<T> {
        @Nullable
        T execute(@Nonnull Connection connection) throws SQLException;
    }

    @FunctionalInterface
    private interface SQLActionWithConnection<T> {
        @Nullable
        T execute() throws SQLException;
    }

    @Nullable
    private <T> T withConnection(@Nonnull SQLAction<T> function) throws SQLException {
        T result;

        ManagedTransaction currentTransaction = transaction.get();
        if (currentTransaction != null && currentTransaction.committed) {
            throw new IllegalStateException("The transaction used to execute the SQL statement is already committed.");
        }

        Connection conn = null;
        try {
            conn = getInternalConnection();
            result = function.execute(conn);
        } finally {
            // Only close the connection if we are outside a transaction (aka transaction.get() == null)
            if (conn != null && transaction.get() == null) {
                conn.close();
            }
        }
        return result;
    }

    @Nullable
    private <T> T handleException(@Nonnull SQLActionWithConnection<T> function) throws UncheckedSQLException {
        try {
            return function.execute();
        } catch (SQLException e) {
            logger.error("Rethrowing SQLException", e);
            throw new UncheckedSQLException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T query(@Nonnull String sql, @Nonnull ResultSetHandler<T> mapper) throws UncheckedSQLException {
        return handleException(() -> withConnection(conn -> queryRunner.query(conn, sql, mapper)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T query(@Nonnull String sql, @Nonnull ResultSetHandler<T> mapper,
            @Nonnull Object... params) throws UncheckedSQLException {
        return handleException(() -> withConnection(conn -> queryRunner.query(conn, sql, mapper, params)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T insert(@Nonnull String sql, @Nonnull ResultSetHandler<T> mapper) throws UncheckedSQLException {
        return handleException(() -> withConnection(conn -> queryRunner.insert(conn, sql, mapper)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T insert(@Nonnull String sql, @Nonnull ResultSetHandler<T> mapper,
            @Nonnull Object... params) throws UncheckedSQLException {
        return handleException(() -> withConnection(conn -> queryRunner.insert(conn, sql, mapper, params)));
    }

    @Override
    public <T> T insertBatch(@Nonnull String sql, @Nonnull ResultSetHandler<T> mapper,
            @Nonnull Object[][] params) throws UncheckedSQLException {
        return handleException(() -> withConnection(conn -> queryRunner.insertBatch(conn, sql, mapper, params)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int update(@Nonnull String sql) throws UncheckedSQLException {
        Integer result = handleException(() -> withConnection(conn -> queryRunner.update(conn, sql)));
        return result != null ? result : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int update(@Nonnull String sql, @Nonnull Object... params) throws UncheckedSQLException {
        Integer result = handleException(() -> withConnection(conn -> queryRunner.update(conn, sql, params)));
        return result != null ? result : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int execute(@Nonnull String sql, @Nonnull Object... params) throws UncheckedSQLException {
        Integer result = handleException(() -> withConnection(conn -> queryRunner.execute(conn, sql, params)));
        return result != null ? result : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> execute(@Nonnull String sql, @Nonnull ResultSetHandler<T> mapper,
            @Nonnull Object... params) throws UncheckedSQLException {
        return handleException(() -> withConnection(conn -> queryRunner.execute(conn, sql, mapper, params)));
    }

    @Override
    public int[] batch(@Nonnull String sql, @Nonnull Object[][] params) throws UncheckedSQLException {
        return handleException(() -> withConnection(conn -> queryRunner.batch(conn, sql, params)));
    }


    private void removeCurrentTransaction() {
        transaction.remove();
    }

    private void replaceCurrentTransaction(@Nonnull ManagedTransaction outerTransaction) {
        transaction.set(outerTransaction);
    }


    private abstract class ManagedTransaction implements Transaction {
        final UUID uuid = UUID.randomUUID();
        boolean committed = false;

        abstract Connection getConnection();

        /**
         * Checks whether this {@link Transaction} is the currently active one.
         *
         * @throws IllegalStateException if this is not the currently active transaction.
         */
        protected void requireCurrentlyActive() {
            ManagedTransaction tx = TransactionAwareQueryRunner.this.transaction.get();
            if (tx == null) {
                throw new IllegalStateException("There is no currently active Transaction!");
            } else {
                if (tx != this) {
                    throw new IllegalStateException("This is not the currently active Transaction!");
                }
            }
        }
    }

    private class InnerManagedTransaction extends ManagedTransaction {
        final ManagedTransaction transaction;
        final Savepoint savepoint;

        private boolean closed = false;

        InnerManagedTransaction(@Nonnull ManagedTransaction transaction) {
            this.transaction = transaction;
            // Set this already here, so log statements in the constructor already have the MDC set.
            MDC.put("tx", this.getTransactionIdentifier());
            try {
                this.savepoint = getConnection().setSavepoint();
                logger.debug("Successfully started {}", this);
            } catch (SQLException e) {
                throw new UncheckedSQLException("Could not create savepoint on connection, when creating transaction", e);
            }
        }

        @Override
        public String getTransactionIdentifier() {
            return transaction.getTransactionIdentifier() + ":" + uuid;
        }

        @Override
        @Nonnull
        Connection getConnection() {
            return transaction.getConnection();
        }

        @Override
        public void commit() throws SQLException {
            if (!committed) {
                requireCurrentlyActive();

                committed = true;
            }
        }

        @Override
        public void close() throws SQLException {
            if (!closed) {
                requireCurrentlyActive();

                try {
                    if (!committed) {
                        getConnection().rollback(savepoint);
                        logger.debug("Successfully rolled back {} to savepoint", this);
                        closed = true;
                    }
                } finally {
                    TransactionAwareQueryRunner.this.replaceCurrentTransaction(transaction);
                    logger.debug("Successfully closed {}", this);
                    MDC.put("tx", transaction.getTransactionIdentifier());
                }
            }
        }

        @Override
        public String toString() {
            return transaction + ":" + uuid.toString().substring(0, 8);
        }
    }

    private class OuterManagedTransaction extends ManagedTransaction {
        @Nonnull
        final Connection connection;

        OuterManagedTransaction(@Nonnull Connection connection, @Nullable Integer transactionIsolation) {
            this.connection = connection;
            // Set this already here, so log statements in the constructor already have the MDC set.
            MDC.put("tx", this.getTransactionIdentifier());
            try {
                connection.setAutoCommit(false);
                if (transactionIsolation != null) {
                    connection.setTransactionIsolation(transactionIsolation);
                }
                logger.debug("Successfully started {}", this);
            } catch (SQLException e) {
                throw new UncheckedSQLException("Could not set autocommit on connection, when creating transaction", e);
            }
        }

        @Override
        public String getTransactionIdentifier() {
            return uuid.toString();
        }

        @Override
        @Nonnull
        Connection getConnection() {
            return connection;
        }

        @Override
        public void commit() throws SQLException {
            if (!committed) {
                requireCurrentlyActive();

                connection.commit();
                committed = true;
                logger.debug("Successfully committed {}", this);
            }
        }

        @Override
        public void close() throws SQLException {
            if (!connection.isClosed()) {
                requireCurrentlyActive();

                try {
                    if (!committed) {
                        connection.rollback();
                        logger.debug("Successfully rolled back {}", this);
                    }
                } finally {
                    try {
                        connection.close();
                        logger.debug("Successfully closed underlying connection {} of {}", connection, this);
                    } finally {
                        TransactionAwareQueryRunner.this.removeCurrentTransaction();
                        logger.debug("Successfully closed {}", this);
                        MDC.remove("tx");
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "Transaction " + uuid.toString().substring(0, 8);
        }
    }
}
