package org.codedefenders.database;

import java.sql.Connection;

import javax.enterprise.inject.Produces;

public class ConnectionProducer {

    @Produces
    public Connection produceConnection() {
        return DB.getConnection();
    }
}
