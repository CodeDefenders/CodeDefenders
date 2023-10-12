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

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.codedefenders.database.ConnectionFactory;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.persistence.database.util.TransactionAwareQueryRunner;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * @author Jose Rojas, Alessio Gambi
 */
public class DatabaseRule extends ExternalResource {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseRule.class);

    private final String dbConnectionUrl;
    private final String username;
    private final String password;

    public DatabaseRule() {
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream("database.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        dbConnectionUrl = "jdbc:mysql://" + props.getProperty("url");
        username = props.getProperty("username");
        password = props.getProperty("password");
    }

    public QueryRunner getQueryRunner() throws SQLException {
        return new TransactionAwareQueryRunner(getConnectionFactory());
    }

    public ConnectionFactory getConnectionFactory() throws SQLException {
        DataSource dataSourceMock = mock(DataSource.class);
        lenient().doAnswer(invocation -> getConnection()).when(dataSourceMock).getConnection();

        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        lenient().doAnswer(invocation -> getConnection()).when(connectionFactory).getConnection();
        return connectionFactory;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbConnectionUrl, username, password);
    }

    @Override
    public void before() throws Exception {
        logger.debug("Started Database Migrations");

        // Load the Database Driver
        Class.forName("com.mysql.cj.jdbc.Driver");

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
