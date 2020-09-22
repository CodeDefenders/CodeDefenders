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
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.codedefenders.configuration.Configuration;

import com.mysql.cj.jdbc.MysqlDataSource;

@ApplicationScoped
public class ConnectionFactory {

    DataSource dataSource;

    @Inject
    ConnectionFactory(Configuration config) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(config.getDbUrl());
        dataSource.setUser(config.getDbUsername());
        dataSource.setPassword(config.getDbPassword());
        this.dataSource = dataSource;
    }

    @PreDestroy
    void shutdown() {
        //
        // dataSource.close();
    }

    @Produces
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
