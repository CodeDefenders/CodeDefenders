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
package org.codedefenders;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.codedefenders.database.ConnectionFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * @author Jose Rojas, Alessio Gambi
 */
public class DatabaseRule extends ExternalResource {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseRule.class);

    // These ensures we do not mess up with the timeZone problem during integration testing
    private static final String[] DEFAULT_OPTIONS = new String[]{
            "useUnicode=true",
            "useJDBCCompliantTimezoneShift=true",
            "useLegacyDatetimeCode=false",
            "serverTimezone=UTC",
            "generateSimpleParameterMetadata=true"
    };

    private DB embeddedDatabase;
    private String dbConnectionUrl;

    private final String dbName = "database";
    private final String username = "database";
    private final String password = "database";

    private String connectionOptions = "";

    public DatabaseRule() {
        this(DEFAULT_OPTIONS);
    }

    public DatabaseRule(String[] options) {
        if (options != null && options.length > 0) {
            connectionOptions = "?" + String.join("&", options);
        }
    }

    public ConnectionFactory getConnectionFactory() throws SQLException {
        DataSource dataSourceMock = mock(DataSource.class);
        when(dataSourceMock.getConnection()).thenAnswer(invocation -> getConnection());
        QueryRunner queryRunner = new QueryRunner(dataSourceMock);

        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.getConnection()).thenAnswer(invocation -> getConnection());
        when(connectionFactory.getQueryRunner()).thenReturn(queryRunner);
        return connectionFactory;
    }

    public Connection getConnection() throws SQLException {
        logger.debug(dbConnectionUrl);
        return DriverManager.getConnection(dbConnectionUrl, username, password);
    }

    @Override
    public void before() throws Exception {
        logger.debug("Started Embedded Database creation");

        /*
        DBConfigurationBuilder databaseConfig = DBConfigurationBuilder.newBuilder();
        // This is necessary to allow the database to run as root, which can be the case if these tests are run inside
        // an (docker) container e.g. in the CI.
        databaseConfig.addArg("--user=root");
        // Setting this to 0 will let the DB to dynamically pick an open port
        // For debugging the database, it can be helpful to change this to a static value
        databaseConfig.setPort(0);

        embeddedDatabase = DB.newEmbeddedDB(databaseConfig.build());
        embeddedDatabase.start();
        embeddedDatabase.createDB(dbName);

        dbConnectionUrl = databaseConfig.getURL(dbName) + connectionOptions;
         */
        dbConnectionUrl = "jdbc:mysql://database:3306/database";
        logger.debug("Finished Embedded Database creation");

        // Load the
        Class.forName("com.mysql.cj.jdbc.Driver");

        logger.debug("Started Database Migrations");
        FluentConfiguration flywayConfig = Flyway.configure();
        flywayConfig.dataSource(dbConnectionUrl, username, password);
        flywayConfig.locations("classpath:db/migrations");

        Flyway flyway = flywayConfig.load();
        // For the Tests we always clean the database
        flyway.clean();
        flyway.migrate();
        logger.debug("Finished Database Migrations");
    }

    @Override
    public void after() {
        try {
            logger.debug("Stopping Embedded Database");
            embeddedDatabase.stop();
        } catch (ManagedProcessException e) {
            // quiet
        }
    }
}
