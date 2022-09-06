/*
 * Copyright (C) 2016-2019,2021 Code Defenders contributors
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

import org.codedefenders.database.ConnectionFactory;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.persistence.database.util.TransactionAwareQueryRunner;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jose Rojas, Alessio Gambi
 */
public class DatabaseRule extends ExternalResource {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseRule.class);

    private String dbConnectionUrl;

    private final String username = "root";
    private final String password = "";

    public QueryRunner getQueryRunner() throws SQLException {
        return new TransactionAwareQueryRunner(getConnectionFactory());
    }

    public ConnectionFactory getConnectionFactory() throws SQLException {
        DataSource dataSourceMock = mock(DataSource.class);
        when(dataSourceMock.getConnection()).thenAnswer(invocation -> getConnection());

        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.getConnection()).thenAnswer(invocation -> getConnection());
        return connectionFactory;
    }

    public Connection getConnection() throws SQLException {
        logger.debug(dbConnectionUrl);
        return DriverManager.getConnection(dbConnectionUrl, username, password);
    }

    @Override
    public void before() throws Exception {
        logger.debug("Started Embedded Database creation");

        dbConnectionUrl = "jdbc:mysql://database:3306/codedefenders";
        logger.debug("Finished Embedded Database creation");

        // Load the Database Driver
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
}
