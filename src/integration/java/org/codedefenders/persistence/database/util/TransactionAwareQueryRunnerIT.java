/*
 * Copyright (C) 2021,2022 Code Defenders contributors
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

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.codedefenders.database.ConnectionFactory;
import org.codedefenders.transaction.Transaction;
import org.codedefenders.transaction.TransactionManager;
import org.codedefenders.util.DatabaseExtension;
import org.codedefenders.util.tags.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.codedefenders.persistence.database.util.ResultSetUtils.oneFromRS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DatabaseTest
@ExtendWith(DatabaseExtension.class)
public class TransactionAwareQueryRunnerIT {

    private TransactionManager transactionManager;
    private QueryRunner queryRunner;

    private Connection connection;


    @BeforeEach
    public void setup(ConnectionFactory connectionFactory) throws Exception {
        // Expose the requested connection from ConnectionFactory
        ConnectionFactory mockedConnectionFactory = mock(ConnectionFactory.class);
        when(mockedConnectionFactory.getConnection())
                .thenAnswer(invocation -> {
                    connection = connectionFactory.getConnection();
                    return connection;
                });

        // Construct the query runner
        TransactionAwareQueryRunner transactionAwareQueryRunner = new TransactionAwareQueryRunner(
                mockedConnectionFactory);
        queryRunner = transactionAwareQueryRunner;
        transactionManager = transactionAwareQueryRunner;

        // Setup database table for testing.
        queryRunner.execute("""
             CREATE TABLE test (
                id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(255) NOT NULL,
                number INT UNIQUE NULL DEFAULT NULL
             );
        """);
    }

    @Test
    public void nestedTransactions() throws Exception {
        Transaction outerTx = transactionManager.startTransaction();


        Transaction innerTx1 = transactionManager.startTransaction();
        // Do Stuff
        innerTx1.commit();
        innerTx1.close();


        Transaction innerTx2 = transactionManager.startTransaction();
        // Do some other stuff
        innerTx2.close();


        outerTx.commit();
        outerTx.close();

        assertTrue(connection.isClosed());
    }

    @Test
    public void multipleLevelOfNestedTransactions() throws Exception {
        Transaction outerTx = transactionManager.startTransaction();


        Transaction innerTx1 = transactionManager.startTransaction();
        // Do stuff 1

        Transaction innerTx2 = transactionManager.startTransaction();
        // Do some other stuff
        innerTx2.close();

        // Do stuff 2
        innerTx1.commit();
        innerTx1.close();


        outerTx.commit();
        outerTx.close();

        assertTrue(connection.isClosed());
    }

    @Test
    public void executeQueryAfterTransactionCommitThrows() throws Exception {
        Transaction tx = transactionManager.startTransaction();
        //noinspection SqlResolve
        queryRunner.execute("SELECT COUNT(*) FROM test;");

        tx.commit();

        assertThrows(IllegalStateException.class, () -> {
            //noinspection SqlResolve
            queryRunner.execute("SELECT COUNT(*) FROM test;");
        });

        tx.close();
        assertTrue(connection.isClosed());
    }

    @Test
    public void wrongTransactionIsolation() throws Exception {
        Transaction outerTx = null;
        AtomicReference<Transaction> innerTx = new AtomicReference<>();
        try {
            outerTx = transactionManager.startTransaction(Connection.TRANSACTION_READ_COMMITTED);

            assertThrows(RuntimeException.class,
                    () -> innerTx.set(transactionManager.startTransaction(Connection.TRANSACTION_REPEATABLE_READ)));

        } finally {
            try {
                if (innerTx.get() != null) {
                    innerTx.get().close();
                }
            } finally {
                if (outerTx != null) {
                    outerTx.close();
                }
            }
        }
    }

    @Test
    public void wrongClosingOrderThrows() throws Exception {
        Transaction outerTx = transactionManager.startTransaction();

        Transaction innerTx1 = transactionManager.startTransaction();

        assertThrows(IllegalStateException.class, outerTx::close);

        innerTx1.close();
        outerTx.close();
    }

    @Test
    public void commitWithoutCurrentlyActiveTransactionThrows() throws Exception {
        TransactionAwareQueryRunner txAwareQueryRunner = (TransactionAwareQueryRunner) transactionManager;

        Transaction outerTx = transactionManager.startTransaction();

        Field field = TransactionAwareQueryRunner.class.getDeclaredField("transaction");
        field.setAccessible(true);
        ThreadLocal<Transaction> transactionThreadLocal = (ThreadLocal<Transaction>) field.get(txAwareQueryRunner);

        Transaction tx = transactionThreadLocal.get();

        transactionThreadLocal.remove();

        assertThrows(IllegalStateException.class, outerTx::commit);

        transactionThreadLocal.set(tx);

        outerTx.close();
    }

    @Test
    public void terminateTransactionDoesNotThrow() throws Exception {
        try (Transaction outerTx = transactionManager.startTransaction()) {
            try (Transaction innerTx1 = transactionManager.startTransaction()) {

                innerTx1.commit();
            }
            outerTx.commit();
        }
        assertDoesNotThrow(() -> transactionManager.terminateTransaction());
        assertTrue(connection.isClosed());
    }

    @Test
    public void terminateTransactionDoesThrow() throws Exception {
        @SuppressWarnings("unused") Transaction outerTx = transactionManager.startTransaction();

        @SuppressWarnings("unused") Transaction innerTx1 = transactionManager.startTransaction();

        @SuppressWarnings("unused") Transaction innerTx2 = transactionManager.startTransaction();

        assertThrows(IllegalStateException.class, () -> transactionManager.terminateTransaction());
        assertTrue(connection.isClosed());
    }

    @Test
    public void rollbackWithoutExplicitCommit() throws Exception {

        transactionManager.executeInTransaction(tx -> {
            //noinspection SqlResolve
            queryRunner.insert("INSERT INTO test (name) VALUES ('test0')",
                    resultSet -> oneFromRS(resultSet, rs -> rs.getInt(1)));
            return true;
        });
        assertTrue(connection.isClosed());

        //noinspection SqlResolve
        Optional<Integer> actual = queryRunner.query("SELECT COUNT(*) FROM test;",
                resultSet -> oneFromRS(resultSet, rs -> rs.getInt(1)));
        assertTrue(actual.isPresent());
        assertEquals(0, actual.get());
    }

    @Test
    public void successfulCommit() throws Exception {

        transactionManager.executeInTransaction(tx -> {
            //noinspection SqlResolve
            queryRunner.insert("INSERT INTO test (name, number) VALUES (?, ?)",
                    resultSet -> oneFromRS(resultSet, rs -> rs.getInt(1)),
                    "test1", 1);
            tx.commit();
            return true;
        });
        assertTrue(connection.isClosed());

        //noinspection SqlResolve
        Optional<Integer> actual = queryRunner.query("SELECT COUNT(*) FROM test;",
                resultSet -> oneFromRS(resultSet, rs -> rs.getInt(1)));
        assertTrue(actual.isPresent());
        assertEquals(1, actual.get());
    }

    @Test
    public void rollbackOnException() throws Exception {
        Optional<Integer> actual;


        transactionManager.executeInTransaction(tx -> {
            //noinspection SqlResolve
            queryRunner.insert("INSERT INTO test (name, number) VALUES (?, ?)",
                    resultSet -> oneFromRS(resultSet, rs -> rs.getInt(1)),
                    "test1", 1);
            tx.commit();
            return true;
        });

        //noinspection SqlResolve
        actual = queryRunner.query("SELECT COUNT(*) FROM test;",
                resultSet -> oneFromRS(resultSet, rs -> rs.getInt(1)));
        assertTrue(actual.isPresent());
        assertEquals(1, actual.get());

        assertThrows(SQLException.class, () -> transactionManager.executeInTransaction(tx -> {
            //noinspection SqlResolve
            queryRunner.insert("INSERT INTO test (name, number) VALUES (?, ?)",
                    resultSet -> oneFromRS(resultSet, rs -> rs.getInt(1)),
                    "test2", 1);
            tx.commit();
            return true;
        }));
        assertTrue(connection.isClosed());

        //noinspection SqlResolve
        actual = queryRunner.query("SELECT COUNT(*) FROM test;",
                resultSet -> oneFromRS(resultSet, rs -> rs.getInt(1)));
        assertTrue(actual.isPresent());
        assertEquals(1, actual.get());
    }

    @Test
    public void rollbackOnExceptionNested() throws Exception {

        int id = transactionManager.executeInTransaction(tx -> {
            //noinspection SqlResolve
            int result = queryRunner.insert("INSERT INTO test (name, number) VALUES (?, ?)",
                    resultSet -> oneFromRS(resultSet, rs -> rs.getInt(1)),
                    "test1", 1).get();
            tx.commit();
            return result;
        });
        assertTrue(connection.isClosed());

        //noinspection SqlResolve
        Optional<Integer> actualInt = queryRunner.query("SELECT COUNT(*) FROM test;",
                resultSet -> oneFromRS(resultSet, rs -> rs.getInt(1)));
        assertTrue(actualInt.isPresent());
        assertEquals(1, actualInt.get());

        assertThrows(SQLException.class, () -> transactionManager.executeInTransaction(tx1 -> {
            transactionManager.executeInTransaction(tx -> {
                //noinspection SqlResolve
                queryRunner.execute("UPDATE test SET name = ? WHERE id = ?;", "test2", id);

                //noinspection SqlResolve
                queryRunner.update("UPDATE test SET name = NULL WHERE id = " + id);
                tx.commit();
                return true;
            });
            tx1.commit();
            return true;
        }));
        assertTrue(connection.isClosed());

        //noinspection SqlResolve
        Optional<String> actualString = queryRunner.query("SELECT name FROM test WHERE id = ?;",
                resultSet -> oneFromRS(resultSet, rs -> rs.getString("name")), id);
        assertTrue(actualString.isPresent());
        assertEquals("test1", actualString.get());
    }
}
