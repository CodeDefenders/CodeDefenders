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
package org.codedefenders.util.database;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.codedefenders.persistence.database.util.QueryRunner;

/**
 * Optional configuration for {@link DatabaseExtension}
 */
public class DatabaseExtensionConfig {
    private String host;
    private Integer port;
    private String name;
    private String username;
    private String password;
    private String url;
    private boolean performClean;
    private boolean performMigrations;
    private SQLCallback preMigrationCallback;
    private SQLCallback postMigrationCallback;

    private DatabaseExtensionConfig() {
        performClean = true;
        performMigrations = true;
    }

    private void readProperties(InputStream input) {
        Properties props = new Properties();
        try {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        url = props.getProperty("url");
        if (url == null) {
            host = Objects.requireNonNull(props.getProperty("host"));
            port = Integer.parseInt(Objects.requireNonNull(props.getProperty("port")));
            name = Objects.requireNonNull(props.getProperty("name"));
        }
        username = Objects.requireNonNull(props.getProperty("username"));
        password = Objects.requireNonNull(props.getProperty("password"));
    }


    /** Loads the config from the classpath under "database.properties" */
    public static DatabaseExtensionConfig defaultConfig() {
        var input = Objects.requireNonNull(
            DatabaseExtensionConfig.class.getClassLoader().getResourceAsStream("database.properties"));
        Objects.requireNonNull(input);
        var config = new DatabaseExtensionConfig();
        config.readProperties(input);
        return config;
    }

    /** Loads the config from the classpath under the specified resource name. */
    public static DatabaseExtensionConfig fromResource(String resourceName) {
        var input = Objects.requireNonNull(
            DatabaseExtensionConfig.class.getClassLoader().getResourceAsStream(resourceName));
        var config = new DatabaseExtensionConfig();
        config.readProperties(input);
        return config;
    }

    /** Loads the default config from the specified properties string (properties file content). */
    public static DatabaseExtensionConfig fromProperties(String properies) {
        var input = new ByteArrayInputStream(properies.getBytes(StandardCharsets.UTF_8));
        var config = new DatabaseExtensionConfig();
        config.readProperties(input);
        return config;
    }

    public static DatabaseExtensionConfig empty() {
        return new DatabaseExtensionConfig();
    }

    public DatabaseExtensionConfig withHost(String host) {
        this.host = host;
        return this;
    }

    public DatabaseExtensionConfig withPort(int port) {
        this.port = port;
        return this;
    }

    public DatabaseExtensionConfig withName(String name) {
        this.name = name;
        return this;
    }

    public DatabaseExtensionConfig withUsername(String username) {
        this.username = username;
        return this;
    }

    public DatabaseExtensionConfig withPassword(String password) {
        this.password = password;
        return this;
    }

    public DatabaseExtensionConfig withURL(String url) {
        this.url = url;
        return this;
    }

    public DatabaseExtensionConfig performingClean(boolean clean) {
        this.performClean = clean;
        return this;
    }

    public DatabaseExtensionConfig performingMigrations(boolean migration) {
        this.performMigrations = migration;
        return this;
    }

    public DatabaseExtensionConfig withPreMigrationScript(SQLCallback preMigrationCallback) {
        this.preMigrationCallback = preMigrationCallback;
        return this;
    }

    public DatabaseExtensionConfig withPostMigrationScript(SQLCallback postMigrationCallback) {
        this.postMigrationCallback = postMigrationCallback;
        return this;
    }

    public String getHost() {
        return Objects.requireNonNull(host);
    }

    public int getPort() {
        return Objects.requireNonNull(port);
    }

    public String getName() {
        return Objects.requireNonNull(name);
    }

    public String getUsername() {
        return Objects.requireNonNull(username);
    }

    public String getPassword() {
        return Objects.requireNonNull(password);
    }

    public String getUrl() {
        if (url == null) {
            url = String.format("jdbc:mysql://%s:%s/%s", getHost(), getPort(), getName());
        }
        if (url.startsWith("jdbc:mysql://")) {
            return url;
        } else {
            return "jdbc:mysql://" + url;
        }
    }

    public Optional<SQLCallback> getPreMigrationCallback() {
        return Optional.ofNullable(preMigrationCallback);
    }

    public Optional<SQLCallback> getPostMigrationCallback() {
        return Optional.ofNullable(postMigrationCallback);
    }

    public boolean isPerformClean() {
        return performClean;
    }

    public boolean isPerformMigrations() {
        return performMigrations;
    }

    @FunctionalInterface
    public interface SQLCallback {
        void execute(QueryRunner queryRunner) throws SQLException;
    }
}
