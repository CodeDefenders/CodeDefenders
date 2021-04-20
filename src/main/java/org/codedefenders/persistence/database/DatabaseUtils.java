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
import java.util.List;

import org.apache.commons.dbutils.ResultSetHandler;

public class DatabaseUtils {

    public static <T> T nextFromRS(ResultSet rs, ResultSetHandler<T> handler) throws SQLException {
        if (rs.next()) {
            return handler.handle(rs);
        } else {
            return null;
        }
    }

    public static <T> List<T> listFromRS(ResultSet rs, ResultSetHandler<T> handler) throws SQLException {
        List<T> result = new ArrayList<>();
        while (rs.next()) {
            result.add(handler.handle(rs));
        }
        return result;
    }
}
