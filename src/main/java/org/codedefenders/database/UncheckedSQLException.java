package org.codedefenders.database;

import java.sql.SQLException;

/**
 * Wraps around a {@link SQLException} to make handling the exception optional.
 */
public class UncheckedSQLException extends RuntimeException {
    public UncheckedSQLException(String message) {
        super(message);
    }

    public UncheckedSQLException(SQLException e) {
        super(e);
    }

    public UncheckedSQLException(String message, SQLException e) {
        super(message, e);
    }
}
