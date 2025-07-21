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
package org.codedefenders.util;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;

import org.codedefenders.database.ConnectionFactory;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.persistence.database.util.TransactionAwareQueryRunner;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Injects QueryRunner, Connection and, ConnectionFactory parameters.
 *
 * <p> If used with the default configuration, the injected database objects connect
 * to the DB specified in "src/integration/resources/database.properties".
 * <p> The database is cleaned and has all migrations applied before each test.
 * <p> This behavior can be changed with {@link DatabaseExtensionConfig}.
 */
public class DatabaseExtension implements ParameterResolver, BeforeEachCallback {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseExtension.class);

    private DatabaseExtensionConfig config;

    public Optional<DatabaseExtensionConfig> findConfig(Object testInstance) {
        for (Class<?> clazz = testInstance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(DatabaseSetup.class)) {
                    Object fieldInstance;
                    try {
                        field.setAccessible(true);
                        fieldInstance = field.get(testInstance);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                    if (fieldInstance instanceof DatabaseExtensionConfig c) {
                        return Optional.of(c);
                    } else {
                        throw new IllegalStateException(
                            "@DatabaseSetup can only be used with DatabaseExtensionConfig");
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        config = extensionContext.getTestInstance().flatMap(this::findConfig)
            .orElseGet(DatabaseExtensionConfig::defaultConfig);

        // Load the Database Driver
        Class.forName("com.mysql.cj.jdbc.Driver");

        FluentConfiguration flywayConfig = Flyway.configure();
        flywayConfig.dataSource(config.getUrl(), config.getUsername(), config.getPassword());
        flywayConfig.locations("classpath:db/migrations");
        flywayConfig.cleanDisabled(false);

        Flyway flyway = flywayConfig.load();

        // For the Tests we always clean the database
        if (config.isPerformClean()) {
            logger.debug("Cleaning Database");
            flyway.clean();
        }

        var preMigrationCallback = config.getPreMigrationCallback().orElse(null);
        if (preMigrationCallback != null) {
            logger.debug("Started Executing Pre-Migration Callback");
            try {
                preMigrationCallback.execute(getQueryRunner());
            } catch (SQLException e) {
                throw new SQLException("SQL Exception during pre-migration callback.", e);
            }
            logger.debug("Finished Executing Pre-Migration Callback");
        }

        if (config.isPerformMigrations()) {
            logger.debug("Started Database Migrations");
            flyway.migrate();
            logger.debug("Finished Database Migrations");
        }

        var postMigrationCallback = config.getPostMigrationCallback().orElse(null);
        if (postMigrationCallback != null) {
            logger.debug("Started Executing Post-Migration Callback");
            try {
                postMigrationCallback.execute(getQueryRunner());
            } catch (SQLException e) {
                throw new SQLException("SQL Exception during post-migration callback.", e);
            }
            logger.debug("Finished Executing Post-Migration Callback");
        }
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
        return DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
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
