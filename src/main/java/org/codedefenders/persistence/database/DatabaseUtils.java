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

package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.apache.commons.dbutils.ResultSetHandler;

public class DatabaseUtils {

    /**
     * Map the first entry of a given {@link ResultSet} {@code rs} with the given {@code handler} to an object and
     * return it wrapped in an Optional if {@code rs} has a next row, or return an empty {@code Optional} if {@code rs}
     * has no next row.
     *
     * @throws SQLException if an {@code SQLException} occurs while accessing the {@code ResultSet}
     */
    public static <T> Optional<T> nextFromRS(@Nonnull ResultSet rs, @Nonnull ResultSetHandler<T> handler)
            throws SQLException {
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
}
