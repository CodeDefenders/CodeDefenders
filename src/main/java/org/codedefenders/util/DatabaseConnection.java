package org.codedefenders.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    public static Connection getConnection() throws SQLException, NamingException {
        Context initialContext = new InitialContext();
        Context environmentContext = (Context) initialContext.lookup("java:comp/env");
        String dataResourceName = "jdbc/codedefenders";
        DataSource dataSource = (DataSource) environmentContext.lookup(dataResourceName);
        return dataSource.getConnection();
    }
}
