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
