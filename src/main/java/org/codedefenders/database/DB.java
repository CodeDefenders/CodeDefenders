/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class DB {
    private static ConnectionPool connPool = ConnectionPool.getInstanceOf();
    private static final Logger logger = LoggerFactory.getLogger(DB.class);

    // TODO Why throw a StorageException here (make NoMoreConnectionsException a RuntimeException?)
    public synchronized static Connection getConnection() {
        try {
            return connPool.getDBConnection();
        } catch (ConnectionPool.NoMoreConnectionsException e) {
            logger.error("No more Connections", e);
            throw new ConnectionPool.StorageException("No more Connections");
        }
    }

    public static void cleanup(Connection conn, PreparedStatement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException se) {
            logger.error("SQL exception while closing statement!", se);
        }
        connPool.releaseDBConnection(conn);
    }

    public static PreparedStatement createPreparedStatement(Connection conn, String query, DatabaseValue... values) {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            int count = 1;
            for (DatabaseValue val : values) {
                final DatabaseValue.Type type = val.getType();
                switch (type) {
                    case BOOLEAN:
                        stmt.setBoolean(count++, val.getBoolVal());
                        break;
                    case INT:
                        stmt.setInt(count++, val.getIntVal());
                        break;
                    case STRING:
                        stmt.setString(count++, val.getStringVal());
                        break;
                    case LONG:
                        stmt.setLong(count++, val.getLongVal());
                        break;
                    case FLOAT:
                        stmt.setFloat(count++, val.getFloatVal());
                        break;
                    case TIMESTAMP:
                        stmt.setTimestamp(count++, val.getTimestampVal());
                        break;
                    default:
                        final IllegalArgumentException e =
                                new IllegalArgumentException("Unknown database value type: " + type);
                        logger.error("Failed to create prepared statement due to unknown database value type.", e);
                        throw e;
                }
            }
        } catch (SQLException se) {
            logger.error("SQLException while creating prepared statement. Query was:\n\t" + query, se);
            DB.cleanup(conn, stmt);
            throw new UncheckedSQLException(se);
        }
        return stmt;
    }

    /*
     * Does not clean up!
     */
    static ResultSet executeQueryReturnRS(Connection conn, PreparedStatement stmt) {
        try {
            stmt.executeQuery();
            return stmt.getResultSet();
        } catch (SQLException se) {
            logger.error("SQLException while getting Result Set for Statement\n\t" + stmt, se);
            DB.cleanup(conn, stmt);
        }
        return null;
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

    public static DatabaseValue getDBV(String v) {
        return new DatabaseValue(v);
    }

    public static DatabaseValue getDBV(int v) {
        return new DatabaseValue(v);
    }

    public static DatabaseValue getDBV(boolean v) {
        return new DatabaseValue(v);
    }

    /*
     * Caution: Explicitly cast to Long or value will be converted to float
     */
    public static DatabaseValue getDBV(float v) {
        return new DatabaseValue(v);
    }

    public static DatabaseValue getDBV(Timestamp v) {
        return new DatabaseValue(v);
    }

    public static DatabaseValue getDBV(Long v) {
        return new DatabaseValue(v);
    }

    /**
     * Provides a way to extract the query result from a {@link ResultSet} entry.
     * The implementation must not advance the {@link ResultSet}. It can return {@code null} to skip an entry.
     * If it throws any {@link Exception}, the query will fail with a {@link SQLMappingException}.
     * If something is wrong with the query result, and the result can not properly be extracted from it,
     * the implementation should throw a {@link SQLMappingException}.
     * @param <T> The class to convert {@link ResultSet} entries to.
     */
    @FunctionalInterface
    public interface RSMapper<T> {
        T extractResultFrom(ResultSet rs) throws SQLException, Exception;
    }

    /**
     * Executes a database query, then uses a mapper function to extract the first value from the query result.
     * Cleans up the database connection and statement afterwards.
     * @param query The query.
     * @param mapper The mapper function.
     * @param params The parameters for the query.
     * @param <T> The type of value to be queried.
     * @return The first result of the query, or {@code null} if the query had no result.
     * @throws UncheckedSQLException If a {@link SQLException} is thrown while executing the query
     *                               or advancing the {@link ResultSet}.
     * @throws SQLMappingException If there is something wrong with the query result, and the result can not properly be
     *                             extracted from it.
     * @see RSMapper
     */
    public static <T> T executeQueryReturnValue(String query, RSMapper<T> mapper, DatabaseValue... params)
        throws UncheckedSQLException, SQLMappingException {

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, params);

        return executeQueryReturnValue(conn, stmt, mapper);
    }

    /**
     * Executes a database query, then uses a mapper function to extract the first value from the query result.
     * Cleans up the database connection and statement afterwards.
     * @param conn The database connection.
     * @param stmt THe prepared database statement.
     * @param mapper The mapper function.
     * @param <T> The type of value to be queried.
     * @return The first result of the query, or {@code null} if the query had no result.
     * @throws UncheckedSQLException If a {@link SQLException} is thrown while executing the query
     *                               or advancing the {@link ResultSet}.
     * @throws SQLMappingException If there is something wrong with the query result, and the result can not properly be
     *                             extracted from it.
     * @see RSMapper
     */
    public static <T> T executeQueryReturnValue(Connection conn, PreparedStatement stmt, RSMapper<T> mapper)
            throws UncheckedSQLException, SQLMappingException {

        try {
            ResultSet resultSet = stmt.executeQuery();

            if (resultSet.next()) {
                try {
                    return mapper.extractResultFrom(resultSet);
                } catch (Exception e){
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
     * @param query The query.
     * @param mapper The mapper function.
     * @param params The parameters for the query.
     * @param <T> The type of value to be queried.
     * @return The results of the query, as a {@link List}. Will never be null.
     * @throws UncheckedSQLException If a {@link SQLException} is thrown while executing the query
     *                               or advancing the {@link ResultSet}.
     * @throws SQLMappingException If there is something wrong with the query result, and the result can not properly be
     *                             extracted from it.
     * @see RSMapper
     */
    public static <T> List<T> executeQueryReturnList(String query, RSMapper<T> mapper, DatabaseValue... params)
        throws UncheckedSQLException, SQLMappingException {

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, params);

        return executeQueryReturnList(conn, stmt, mapper);
    }

    /**
     * Executes a database query, then uses a mapper function to extract the values from the query result.
     * Cleans up the database connection and statement afterwards.
     * @param conn The database connection.
     * @param stmt THe prepared database statement.
     * @param mapper The mapper function.
     * @param <T> The type of value to be queried.
     * @return The results of the query, as a {@link List}. Will never be null.
     * @throws UncheckedSQLException If a {@link SQLException} is thrown while executing the query
     *                               or advancing the {@link ResultSet}.
     * @throws SQLMappingException If there is something wrong with the query result, and the result can not properly be
     *                             extracted from it.
     * @see RSMapper
     */
    public static <T> List<T> executeQueryReturnList(Connection conn, PreparedStatement stmt, RSMapper<T> mapper)
            throws UncheckedSQLException, SQLMappingException {

        try {
            ResultSet resultSet = stmt.executeQuery();
            List<T> values = new ArrayList<>(resultSet.getFetchSize());

            while (resultSet.next()) {
                T value;

                try {
                    value = mapper.extractResultFrom(resultSet);
                } catch (Exception e){
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
}
