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
import java.sql.SQLException;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;
import org.codedefenders.configuration.Configuration;
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

    public void updateSize(int maxTotalConnections) {
        //dataSource.setMaxTotal(maxTotalConnections);
    }

    public void updateWaitingTime(int parseInt) {
    }
}
