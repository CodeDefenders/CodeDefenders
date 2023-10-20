/*
 * Copyright (C) 2020 Code Defenders contributors
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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.ThreadSafe;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;
import org.codedefenders.configuration.Configuration;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ThreadSafe // Probably
@Singleton
public class ConnectionFactory {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionFactory.class);

    private final BasicDataSource dataSource;

    @Inject
    public ConnectionFactory(@SuppressWarnings("CdiInjectionPointsInspection") final Configuration config) {
        if (config.isValid()) {
            dataSource = new BasicDataSource();
            dataSource.setDriverClassName("net.bull.javamelody.JdbcDriver");
            dataSource.setUrl(config.getDbUrl() + "?driver=com.mysql.cj.jdbc.Driver");
            dataSource.setUsername(config.getDbUsername());
            dataSource.setPassword(config.getDbPassword());
            dataSource.setMaxTotal(config.getMaximumTotalDatabaseConnections());
            dataSource.setMaxWaitMillis(config.getDatabaseConnectionTimeout());

            migrate(config.getDbName());
        } else {
            throw new RuntimeException("Configuration invalid");
        }
    }

    @PostConstruct
    void init() {
    }

    @PreDestroy
    void shutdown() {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (SQLException e) {
                logger.error("Error in closing database connections", e);
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void migrate(String dbName) {
        FluentConfiguration flywayConfig = Flyway.configure();
        flywayConfig.dataSource(dataSource);
        flywayConfig.locations("classpath:db/migrations");

        // Load JavaMigrations from CDI
        flywayConfig.javaMigrations(CDI.current().select(BaseJavaMigration.class).stream().toArray(JavaMigration[]::new));

        Map<String, Boolean> check = checkDatabase(dbName);

        if (!check.get("databaseEmpty") && !check.get("flywayHistoryExists")) {
            if (check.get("databaseBaseline1.7")) {
                flywayConfig.baselineVersion("3");
                flywayConfig.baselineOnMigrate(true);
            } else if (check.get("databaseBaseline1.6")) {
                flywayConfig.baselineVersion("1");
                flywayConfig.baselineOnMigrate(true);
            } else {
                throw new IllegalStateException("Unsupported database update! Please first update to 1.6");
            }
        }

        Flyway flyway = flywayConfig.load();
        flyway.migrate();
    }

    @FunctionalInterface
    private interface DataBaseCheck<T, R> {
        R check(T input) throws SQLException;
    }

    private Map<String, Boolean> checkDatabase(String dbName) {
        Map<String, Boolean> result = new HashMap<>();
        result.put("databaseEmpty",
                !checkNonEmpty(metaData -> metaData.getTables(dbName, null, null, null)));
        result.put("flywayHistoryExists",
                checkNonEmpty(metaData -> metaData.getTables(null, null, "flyway_schema_history", null)));
        result.put("databaseBaseline1.6",
                checkNonEmpty(metaData -> metaData.getColumns(null, null, "mutants", "KillMessage")));
        result.put("databaseBaseline1.7",
                checkNonEmpty(metaData -> metaData.getColumns(null, null, "puzzles", "Active")));
        return result;
    }

    private boolean checkNonEmpty(DataBaseCheck<DatabaseMetaData, ResultSet> checkFunction) {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = checkFunction.check(metaData)) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Exception while trying to access database", e);
            return false;
        }
    }
}
