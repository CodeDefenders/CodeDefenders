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

import java.sql.SQLException;

/**
 * Wraps around a {@link SQLException} to make handling the exception optional.
 */
public class UncheckedSQLException extends RuntimeException {
    private final int errorCode;

    public UncheckedSQLException(String message) {
        super(message);
        errorCode = 0;
    }

    public UncheckedSQLException(SQLException e) {
        super(e);
        errorCode = e.getErrorCode();
    }

    public UncheckedSQLException(String message, SQLException e) {
        super(message, e);
        errorCode = e.getErrorCode();
    }

    public int getErrorCode() {
        return errorCode;
    }

    public boolean isDataTooLong() {
        // MySQL / MariaDB error code for ER_DATA_TOO_LONG
        return getErrorCode() == 1406;
    }
}
