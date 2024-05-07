/*
 * Copyright (C) 2021 Code Defenders contributors
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

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.dbutils.ResultSetHandler;
import org.codedefenders.database.UncheckedSQLException;
import org.intellij.lang.annotations.Language;

/**
 * Interface for classes which implement common logic for database operations without exposing a connection.
 *
 * @author degenhart
 */
public interface QueryRunner {

    /**
     * Executes the given SELECT SQL statement without any replacement parameters and returns a result object.
     *
     * @param sql    The SQL statement to execute.
     * @param mapper A mapper from the ResultSet to the result object.
     * @param <T>    The type of object the mapper returns
     * @return An object generated by the handler.
     * @throws UncheckedSQLException if a database access error occurs
     */
    <T> T query(@Language("SQL") @Nonnull String sql, @Nonnull ResultSetHandler<T> mapper) throws UncheckedSQLException;

    /**
     * Executes the given SELECT SQL statement and returns a result object.
     *
     * @param sql    The SQL statement to execute.
     * @param mapper A mapper from the ResultSet to the result object.
     * @param params Initialize the PreparedStatement's IN parameters with this array.
     * @param <T>    The type of object the mapper returns
     * @return An object generated by the handler.
     * @throws UncheckedSQLException if a database access error occurs
     */
    <T> T query(@Language("SQL") @Nonnull String sql, @Nonnull ResultSetHandler<T> mapper,
            @Nonnull Object... params) throws UncheckedSQLException;

    /**
     * Executes the given INSERT SQL statement without any replacement parameters.
     *
     * @param sql    The SQL statement to execute.
     * @param mapper A mapper from the ResultSet of auto-generated keys to the result object.
     * @param <T>    The type of object the mapper returns
     * @return An object generated by the mapper.
     * @throws UncheckedSQLException if a database access error occurs
     */
    <T> T insert(@Language("SQL") @Nonnull String sql, @Nonnull ResultSetHandler<T> mapper)
            throws UncheckedSQLException;

    /**
     * Executes the given INSERT SQL statement.
     *
     * @param sql    The SQL statement to execute.
     * @param mapper A mapper from the ResultSet of auto-generated keys to the result object.
     * @param params Initialize the PreparedStatement's IN parameters with this array.
     * @param <T>    The type of object the mapper returns
     * @return An object generated by the mapper.
     * @throws UncheckedSQLException if a database access error occurs
     */
    <T> T insert(@Language("SQL") @Nonnull String sql, @Nonnull ResultSetHandler<T> mapper,
            @Nonnull Object... params) throws UncheckedSQLException;


    <T> T insertBatch(@Language("SQL") @Nonnull String sql, @Nonnull ResultSetHandler<T> mapper,
            @Nonnull Object[][] params) throws UncheckedSQLException;

    /**
     * Executes the given INSERT, UPDATE, or DELETE SQL statement without any replacement parameters.
     *
     * @param sql The SQL statement to execute.
     * @return The number of rows updated.
     * @throws UncheckedSQLException if a database access error occurs
     */
    int update(@Language("SQL") @Nonnull String sql) throws UncheckedSQLException;

    /**
     * Executes the given INSERT, UPDATE, or DELETE SQL statement.
     *
     * @param sql    The SQL statement to execute.
     * @param params Initialize the PreparedStatement's IN parameters with this array.
     * @return The number of rows updated.
     * @throws UncheckedSQLException if a database access error occurs
     */
    int update(@Language("SQL") @Nonnull String sql, @Nonnull Object... params) throws UncheckedSQLException;

    /**
     * Execute an SQL statement which does not return any result sets.
     *
     * @param sql    The SQL statement to execute
     * @param params The query replacement parameters
     * @return The number of rows updated.
     * @throws UncheckedSQLException if a database access error occurs
     */
    int execute(@Language("SQL") @Nonnull String sql, @Nonnull Object... params) throws UncheckedSQLException;

    /**
     * Execute an SQL statement which returns one or more result sets.
     *
     * @param sql    The SQL statement to execute.
     * @param mapper A mapper from the ResultSet to the result object.
     * @param params The query replacement parameters
     * @param <T>    The type of object that the handler returns
     * @return A list of objects generated by the handler
     * @throws UncheckedSQLException if a database access error occurs
     */
    <T> List<T> execute(@Language("SQL") @Nonnull String sql, @Nonnull ResultSetHandler<T> mapper,
            @Nonnull Object... params) throws UncheckedSQLException;

    int[] batch(@Language("SQL") @Nonnull String sql, @Nonnull Object[][] params) throws UncheckedSQLException;
}
