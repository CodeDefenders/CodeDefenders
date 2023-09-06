/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.codedefenders.util.CDIUtil;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @deprecated Please use {@link org.codedefenders.persistence.database.util.QueryRunner} instead, you can obtain it
 * via injection.
 * For an example which already uses this take a look at {@link org.codedefenders.persistence.database.UserRepository}.
 * For the {@link org.codedefenders.persistence.database.util.QueryRunner} obtained via injection you don't need to
 * provide a connection, it will handle the connection transparently for you.
 * Another thing to watch out for is the need to call {@link ResultSet#next()}, to simplify this you can use
 * {@link org.codedefenders.persistence.database.util.ResultSetUtils#nextFromRS(ResultSet, org.apache.commons.dbutils.ResultSetHandler)}
 * which will wrap the {@link ResultSet#next()} check for you.
 */
@Deprecated
public class DB {
    private static final Logger logger = LoggerFactory.getLogger(DB.class);

    public static synchronized Connection getConnection() {
        try {
            return CDIUtil.getBeanFromCDI(ConnectionFactory.class).getConnection();
        } catch (SQLException e) {
            logger.error("Unable to acquire SQL connection", e);
            return null;
        }
    }

    public static void cleanup(Connection conn, PreparedStatement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException se) {
            logger.error("SQL exception while closing statement!", se);
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("Unable to close SQL connection", e);
            }
        }
    }

    public static PreparedStatement createPreparedStatement(Connection conn,@Language("SQL") String query,
                                                            DatabaseValue<?>... values) {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            int count = 1;
            for (DatabaseValue<?> value : values) {
                assignDatabaseValue(stmt, value, count);
                count++;
            }
        } catch (SQLException se) {
            logger.error("SQLException while creating prepared statement. Query was:\n\t" + query, se);
            DB.cleanup(conn, stmt);
            throw new UncheckedSQLException(se);
        }
        return stmt;
    }

    private static void assignDatabaseValue(PreparedStatement stmt, DatabaseValue<?> value, int position)
            throws SQLException {
        final DatabaseValue.Type type = value.getType();
        switch (type) {
            case NULL:
                stmt.setNull(position, type.typeValue);
                break;
            case BOOLEAN:
            case INT:
            case STRING:
            case LONG:
            case FLOAT:
            case TIMESTAMP:
                stmt.setObject(position, value.getValue(), type.typeValue);
                break;
            default:
                final IllegalArgumentException e =
                        new IllegalArgumentException("Unknown database value type: " + type);
                logger.error("Failed to create prepared statement due to unknown database value type.", e);
                throw e;
        }
    }

    public static boolean executeUpdate(PreparedStatement stmt, Connection conn) {
        try {
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("SQLException while executing Update for statement\n\t" + stmt, e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return false;
    }

    static boolean executeUpdateQuery(@Language("SQL") String query, DatabaseValue<?>... params) {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, params);

        return executeUpdate(stmt, conn);
    }

    public static int executeUpdateGetKeys(PreparedStatement stmt, Connection conn) {
        try {
            if (stmt.executeUpdate() > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("SQLException while executing Update and getting generated Keys for statement\n\t" + stmt, e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return -1;
    }

    static int executeUpdateQueryGetKeys(@Language("SQL") String query, DatabaseValue<?>... params) {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, params);

        return executeUpdateGetKeys(stmt, conn);
    }

    /**
     * Provides a way to extract the query result from a {@link ResultSet} entry.
     * The implementation must not advance the {@link ResultSet}. It can return {@code null} to skip an entry.
     * If it throws any {@link Exception}, the query will fail with a {@link SQLMappingException}.
     * If something is wrong with the query result, and the result can not properly be extracted from it,
     * the implementation should throw a {@link SQLMappingException}.
     *
     * @param <T> The class to convert {@link ResultSet} entries to.
     */
    @FunctionalInterface
    interface RSMapper<T> {
        T extractResultFrom(ResultSet rs) throws SQLException, Exception;
    }

    /**
     * Executes a database query, then uses a mapper function to extract the first value from the query result.
     * Cleans up the database connection and statement afterwards.
     *
     * @param query  The query.
     * @param mapper The mapper function.
     * @param params The parameters for the query.
     * @param <T>    The type of value to be queried.
     * @return The first result of the query, or {@code null} if the query had no result.
     * @throws UncheckedSQLException If a {@link SQLException} is thrown while executing the query
     *                               or advancing the {@link ResultSet}.
     * @throws SQLMappingException   If there is something wrong with the query result, and the result can
     *                               not properly be extracted from it.
     * @see RSMapper
     */
    static <T> T executeQueryReturnValue(@Language("SQL") String query, RSMapper<T> mapper, DatabaseValue<?>... params)
            throws UncheckedSQLException, SQLMappingException {

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, params);

        return executeQueryReturnValue(conn, stmt, mapper);
    }

    /**
     * Executes a database query, then uses a mapper function to extract the first value from the query result.
     * Cleans up the database connection and statement afterwards.
     *
     * @param conn   The database connection.
     * @param stmt   THe prepared database statement.
     * @param mapper The mapper function.
     * @param <T>    The type of value to be queried.
     * @return The first result of the query, or {@code null} if the query had no result.
     * @throws UncheckedSQLException If a {@link SQLException} is thrown while executing the query
     *                               or advancing the {@link ResultSet}.
     * @throws SQLMappingException   If there is something wrong with the query result, and the result can
     *                               not properly be extracted from it.
     * @see RSMapper
     */
    private static <T> T executeQueryReturnValue(Connection conn, PreparedStatement stmt, RSMapper<T> mapper)
            throws UncheckedSQLException, SQLMappingException {

        try {
            ResultSet resultSet = stmt.executeQuery();

            if (resultSet.next()) {
                try {
                    return mapper.extractResultFrom(resultSet);
                } catch (Exception e) {
                    logger.error("Exception while handling result set.", e);
                    throw new SQLMappingException("Exception while handling result set.", e);
                }
            }

            return null;

        } catch (SQLException e) {
            logger.error("SQL exception while executing query.", e);
            throw new UncheckedSQLException("SQL exception while executing query.", e);

        } finally {
            DB.cleanup(conn, stmt);
        }
    }

    /**
     * Executes a database query, then uses a mapper function to extract the values from the query result.
     * Cleans up the database connection and statement afterwards.
     *
     * @param query  The query.
     * @param mapper The mapper function.
     * @param params The parameters for the query.
     * @param <T>    The type of value to be queried.
     * @return The results of the query, as a {@link List}. Will never be null.
     * @throws UncheckedSQLException If a {@link SQLException} is thrown while executing the query
     *                               or advancing the {@link ResultSet}.
     * @throws SQLMappingException   If there is something wrong with the query result, and the result can
     *                               not properly be extracted from it.
     * @see RSMapper
     */
    static <T> List<T> executeQueryReturnList(@Language("SQL") String query, RSMapper<T> mapper,
                                              DatabaseValue<?>... params)
            throws UncheckedSQLException, SQLMappingException {

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, params);

        return executeQueryReturnList(conn, stmt, mapper);
    }

    /**
     * Executes a database query, then uses a mapper function to extract the values from the query result.
     * Cleans up the database connection and statement afterwards.
     *
     * @param query  The query.
     * @param fetchSize The statement fetch size.
     * @param mapper The mapper function.
     * @param params The parameters for the query.
     * @param <T>    The type of value to be queried.
     * @return The results of the query, as a {@link List}. Will never be null.
     * @throws UncheckedSQLException If a {@link SQLException} is thrown while executing the query
     *                               or advancing the {@link ResultSet}.
     * @throws SQLMappingException   If there is something wrong with the query result, and the result can
     *                               not properly be extracted from it.
     * @see RSMapper
     * @see Statement#setFetchSize(int)
     */
    static <T> List<T> executeQueryReturnListWithFetchSize(@Language("SQL") String query,
                                                           int fetchSize,
                                                           RSMapper<T> mapper,
                                                           DatabaseValue<?>... params)
            throws UncheckedSQLException, SQLMappingException {

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, params);
        try {
            stmt.setFetchSize(fetchSize);
        } catch (SQLException e) {
            logger.error("Caught SQL exception while trying to set fetch size.", e);
            throw new UncheckedSQLException("Caught SQL exception while trying to set fetch size.", e);
        }

        return executeQueryReturnList(conn, stmt, mapper);
    }

    /**
     * Executes a database query, then uses a mapper function to extract the values from the query result.
     * Cleans up the database connection and statement afterwards.
     *
     * @param conn   The database connection.
     * @param stmt   THe prepared database statement.
     * @param mapper The mapper function.
     * @param <T>    The type of value to be queried.
     * @return The results of the query, as a {@link List}. Will never be null.
     * @throws UncheckedSQLException If a {@link SQLException} is thrown while executing the query
     *                               or advancing the {@link ResultSet}.
     * @throws SQLMappingException   If there is something wrong with the query result, and the result can
     *                               not properly be extracted from it.
     * @see RSMapper
     */
    private static <T> List<T> executeQueryReturnList(Connection conn, PreparedStatement stmt, RSMapper<T> mapper)
            throws UncheckedSQLException, SQLMappingException {

        try {
            ResultSet resultSet = stmt.executeQuery();
            List<T> values = new ArrayList<>(resultSet.getFetchSize());

            while (resultSet.next()) {
                T value;

                try {
                    value = mapper.extractResultFrom(resultSet);
                } catch (Exception e) {
                    logger.error("Exception while handling result set.", e);
                    throw new SQLMappingException("Exception while handling result set.", e);
                }

                if (value != null) {
                    values.add(value);
                }
            }

            return values;

        } catch (SQLException e) {
            logger.error("SQL exception while executing query.", e);
            throw new UncheckedSQLException("SQL exception while executing query.", e);

        } finally {
            DB.cleanup(conn, stmt);
        }
    }

    /**
     * Provides a way to extract {@link DatabaseValue database values} from an element of given type {@code T}.
     * It should never return {@code null}. If no values are extracted, an empty array should be returned.
     *
     * @param <T> The class to extract the {@link DatabaseValue DatabaseValues} from.
     */
    @FunctionalInterface
    interface DBVExtractor<T> {
        @NotNull
        DatabaseValue<?>[] extractValues(@NotNull T element);
    }

    /**
     * Prepares a statement for batch execution for elements of a given list.
     * {@link DatabaseValue Database values} for the prepared statement are extracted using a {@link DBVExtractor}.
     * Executes the batch statement, collects generated keys and returns them.
     * Cleans up the database connection and statement afterwards.
     *
     * @param query the query to be executed multiple times.
     * @param elements a list of elements which the query is executed for.
     * @param valueExtractor extracts {@link DatabaseValue DatabaseValues} from an element of type {@code T} .
     * @param <T> the type of the elements used for the query.
     * @return a list of generated keys in the same order as the given list of elements.
     */
    static <T> List<Integer> executeBatchQueryReturnKeys(@Language("SQL") String query, List<T> elements,
                                                         DBVExtractor<T> valueExtractor)
            throws UncheckedSQLException, SQLMappingException {

        Connection conn = DB.getConnection();
        PreparedStatement stmt = null;

        try {
            stmt = DB.createPreparedStatement(conn, query);
            for (T element : elements) {
                final DatabaseValue<?>[] values = valueExtractor.extractValues(element);

                for (int position = 0; position < values.length; position++) {
                    final DatabaseValue<?> value = values[position];
                    assignDatabaseValue(stmt, value, position + 1); // parameter index starts at 1
                }

                stmt.addBatch();
            }

            stmt.executeBatch();

            final ResultSet rs = stmt.getGeneratedKeys();
            final List<Integer> generatedIds = new ArrayList<>();
            while (rs.next()) {
                generatedIds.add(rs.getInt(1));
            }
            return generatedIds;
        } catch (SQLException e) {
            logger.error("SQL exception while executing query.", e);
            throw new UncheckedSQLException("SQL exception while executing query.", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
    }
}
