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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.Nonnull;

import org.apache.commons.dbutils.ResultSetHandler;

public class ResultSetUtils {

    /**
     * Map the next entry of a given {@link ResultSet} {@code rs} with the given {@code handler} to an object and
     * return it wrapped in an Optional if {@code rs} has a next row, or return an empty {@code Optional} if {@code rs}
     * has no next row.
     *
     * @throws SQLException if an {@code SQLException} occurs while accessing the {@code ResultSet}
     */
    @Nonnull
    public static <T> Optional<T> nextFromRS(@Nonnull ResultSet rs,
            @Nonnull ResultSetHandler<T> handler) throws SQLException {
        if (rs.next()) {
            return Optional.ofNullable(handler.handle(rs));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Map all entries of the given {@link ResultSet} with the given {@code handler} to objects and collect them in a
     * {@code List}.
     *
     * @throws SQLException if an {@code SQLException} occurs while accessing the {@code ResultSet}
     */
    @Nonnull
    public static <T> List<T> listFromRS(ResultSet rs, ResultSetHandler<T> handler) throws SQLException {
        List<T> result = new ArrayList<>();
        while (rs.next()) {
            result.add(handler.handle(rs));
        }
        return result;
    }

    /**
     * Map the next entry of a given {@link ResultSet} {@code rs} with the given {@code handler} to an object ensure it
     * is the last entry of {@code rs} and return it wrapped in an Optional if {@code rs} has a next row, or return an
     * empty {@code Optional} if {@code rs} has no next row.
     *
     * @throws SQLException          if an {@code SQLException} occurs while accessing the {@code ResultSet}
     * @throws IllegalStateException if there are more than on entry in the {@link ResultSet}.
     */
    @Nonnull
    public static <T> Optional<T> oneFromRS(@Nonnull ResultSet rs,
            @Nonnull ResultSetHandler<T> handler) throws SQLException {
        Optional<T> result = nextFromRS(rs, handler);
        if (rs.next()) {
            throw new IllegalStateException("Provided ResultSet has more then one entry!");
        }
        return result;
    }

    /**
     * @see ResultSetUtils#nextFromRS(ResultSet, ResultSetHandler)
     */
    public static <T> ResultSetHandler<Optional<T>> nextFromRS(ResultSetHandler<T> handler) {
        return resultSet -> nextFromRS(resultSet, handler);
    }

    /**
     * @see ResultSetUtils#listFromRS(ResultSet, ResultSetHandler)
     */
    public static <T> ResultSetHandler<List<T>> listFromRS(ResultSetHandler<T> handler) {
        return resultSet -> listFromRS(resultSet, handler);
    }

    /**
     * @see ResultSetUtils#oneFromRS(ResultSet, ResultSetHandler)
     */
    public static <T> ResultSetHandler<Optional<T>> oneFromRS(ResultSetHandler<T> handler) {
        return resultSet -> oneFromRS(resultSet, handler);
    }

    /**
     * Extracts exactly one generated key from an insert query {@link ResultSet}.
     * @see ResultSetUtils#oneFromRS(ResultSetHandler)
     */
    public static ResultSetHandler<Optional<Integer>> generatedKeyFromRS() {
        // Insert queries with 'ON DUPLICATE KEY UPDATE' return multiple keys. The first key should be the updated one.
        return nextFromRS(rs ->  rs.getInt(1));
    }
}
