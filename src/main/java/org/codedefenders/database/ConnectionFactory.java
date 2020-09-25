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
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;
import org.codedefenders.configuration.Configuration;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.cj.jdbc.Driver;


@Singleton
public class ConnectionFactory {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionFactory.class);

    BasicDataSource dataSource;

    @Inject
    ConnectionFactory(Configuration config) {
        dataSource = new BasicDataSource();
        try {
            dataSource.setDriver(new Driver());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        dataSource.setUrl(config.getDbUrl());
        dataSource.setUsername(config.getDbUsername());
        dataSource.setPassword(config.getDbPassword());
    }

    @PostConstruct
    void init() {
        migrate();
    }

    @PreDestroy
    void shutdown() {
        try {
            dataSource.close();
        } catch (SQLException e) {
            logger.error("Error in closing database connections", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void migrate() {
        FluentConfiguration flywayConfig = Flyway.configure();
        flywayConfig.dataSource(dataSource);
        flywayConfig.locations("classpath:db/migrations");

        Map<String, Boolean> check = checkDatabase();

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
    interface DataBaseCheck<T, R> {
        R check(T input) throws SQLException;
    }

    private Map<String, Boolean> checkDatabase() {
        Map<String, Boolean> result = new HashMap<>();
        result.put("databaseEmpty", checkDatabase(metaData -> metaData.getTables(null, null, null, null), true));
        result.put("flywayHistoryExists",
                checkDatabase(metaData -> metaData.getTables(null, null, "flyway_schema_histry", null)));
        result.put("databaseBaseline1.6",
                checkDatabase(metaData -> metaData.getColumns(null, null, "games", "ForceHamcrest")));
        result.put("databaseBaseline1.7",
                checkDatabase(metaData -> metaData.getColumns(null, null, "puzzles", "Active")));
        return result;
    }

    private Boolean checkDatabase(DataBaseCheck<DatabaseMetaData, ResultSet> checkFunction) {
        return checkDatabase(checkFunction, false);
    }

    private Boolean checkDatabase(DataBaseCheck<DatabaseMetaData, ResultSet> checkFunction, boolean noResult) {
        Boolean result = null;
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = checkFunction.check(metaData)) {
                result = rs.next() == !noResult;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return result;
    }

    public void updateSize(int maxTotalConnections) {
        //dataSource.setMaxTotal(maxTotalConnections);
    }

    public void updateWaitingTime(int parseInt) {
    }
}
