/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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

import java.sql.ResultSet;

/**
 * Indicates that something went wrong while extracting values from a {@link ResultSet}, or that there is something
 * wrong with the query result and the result can't properly be extracted from it.
 */
public class SQLMappingException extends RuntimeException {
    public SQLMappingException(String message) {
        super(message);
    }

    public SQLMappingException(Exception e) {
        super(e);
    }

    public SQLMappingException(String message, Exception e) {
        super(message, e);
    }
}
