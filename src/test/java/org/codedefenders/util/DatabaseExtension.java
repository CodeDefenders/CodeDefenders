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
package org.codedefenders.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.codedefenders.database.ConnectionFactory;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.persistence.database.util.TransactionAwareQueryRunner;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Injects QueryRunner, Connection and ConnectionFactory parameters.
 *
 * <p>The injected database objects connect to the DB specified in 'src/integration/resources/database.properties'.
 * <p>The database is cleaned and has all migrations applied before each test.
 */
public class DatabaseExtension implements ParameterResolver, BeforeAllCallback, BeforeEachCallback {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseExtension.class);

    private String dbConnectionUrl;
    private String username;
    private String password;

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
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

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
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

    public QueryRunner getQueryRunner() throws SQLException {
        return new TransactionAwareQueryRunner(getConnectionFactory());
    }

    public ConnectionFactory getConnectionFactory() throws SQLException {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        lenient().when(connectionFactory.getConnection())
                .thenAnswer(invocation -> getConnection());

        return connectionFactory;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbConnectionUrl, username, password);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return type.equals(QueryRunner.class)
                || type.equals(Connection.class)
                || type.equals(ConnectionFactory.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        try {
            if (type.equals(QueryRunner.class)) {
                return getQueryRunner();
            }
            if (type.equals(Connection.class)) {
                return getConnection();
            }
            if (type.equals(ConnectionFactory.class)) {
                return getConnectionFactory();
            }
        } catch (SQLException e) {
            throw new ParameterResolutionException("Failed to inject DB connection.", e);
        }
        throw new ParameterResolutionException("Couldn't resolve parameter.");
    }
}
