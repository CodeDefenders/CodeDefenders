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
package org.codedefenders.configuration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specifies the configuration keys, their types, and their getters.
 *
 * @see org.codedefenders.configuration.source configuration sources
 * @see org.codedefenders.configuration.configfileresolver configuration file resolvers
 * @see org.codedefenders.configuration.BaseConfiguration configuration specification
 * @see org.codedefenders.configuration.Configuration logic and validation
 */
public abstract class BaseConfiguration {
    // All the attributes need to be initialized with a null value and therefore need to be objects
    protected String appUrl;
    protected String dataDir;
    protected String libDir;
    protected String antHome;
    protected String antJavaHome;
    protected Integer antJavaVersion;
    protected String dbHost;
    protected Integer dbPort;
    protected String dbName;
    protected String dbUsername;
    protected String dbPassword;
    protected Integer dbConnectionsMax;
    protected Integer dbConnectionsTimeout;
    protected Boolean clusterMode;
    protected String clusterJavaHome;
    protected String clusterReservationName;
    protected Integer clusterTimeout;
    protected Boolean forceLocalExecution;
    protected Boolean parallelize;
    protected Integer parallelizeCount;
    protected Integer parallelizeKillmapCount; // TODO(kreismar): is this needed?
    protected Boolean blockAttacker;
    protected Boolean mutantCoverage;

    @Deprecated
    protected String authAdminRole;
    protected String authAdminUsers;

    protected Boolean metrics;
    protected Boolean javamelody;


    public File getDataDir() {
        return new File(dataDir);
    }

    public File getMutantDir() {
        return new File(getDataDir(), "mutants");
    }

    public File getTestsDir() {
        return new File(getDataDir(), "tests");
    }

    public File getSourcesDir() {
        return new File(getDataDir(), "sources");
    }

    public File getLibraryDir() {
        return getDataDir().toPath().resolve(libDir).toFile();
    }

    public File getAntHome() {
        return new File(antHome);
    }

    public Optional<File> getAntJavaHome() {
        return antJavaHome == null
                ? Optional.empty()
                : Optional.of(new File(antJavaHome));
    }

    public Optional<Integer> getAntJavaVersion() {
        return Optional.ofNullable(antJavaVersion);
    }

    public String getDbUrl() {
        return "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public Integer getMaximumTotalDatabaseConnections() {
        return dbConnectionsMax;
    }

    public Integer getDatabaseConnectionTimeout() {
        return dbConnectionsTimeout;
    }

    public boolean isClusterModeEnabled() {
        return clusterMode;
    }

    public String getClusterJavaHome() {
        return clusterJavaHome;
    }

    public String getClusterReservationName() {
        return clusterReservationName;
    }

    public int getClusterTimeout() {
        return Objects.requireNonNullElse(clusterTimeout, -1);
    }

    public boolean isForceLocalExecution() {
        return forceLocalExecution;
    }

    public boolean isParallelize() {
        return parallelize;
    }

    public int getNumberOfParallelAntExecutions() {
        return parallelizeCount;
    }

    public boolean isBlockAttacker() {
        return blockAttacker;
    }

    public boolean isMutantCoverage() {
        return mutantCoverage;
    }

    public int getNumberOfKillmapThreads() {
        return Objects.requireNonNullElseGet(clusterTimeout,
                () -> Runtime.getRuntime().availableProcessors());
    }

    public boolean isMetricsCollectionEnabled() {
        return metrics != null ? metrics : false;
    }

    public boolean isJavaMelodyEnabled() {
        return javamelody != null ? javamelody : false;
    }

    @Deprecated
    public String getAuthAdminRole() {
        return authAdminRole;
    }

    public List<String> getAuthAdminUsers() {
        if (authAdminUsers == null || authAdminUsers.trim().isEmpty()) {
            return Collections.emptyList();
        } else {
            return Arrays.stream(authAdminUsers.split(","))
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .toList();
        }
    }

    /**
     * @return A URL that has a protocol, host, path, and optional a port.
     */
    public Optional<URL> getApplicationURL() {
        return Optional.ofNullable(appUrl)
        .filter(s -> !s.trim().isEmpty())
        .map(s -> {
            try {
                return new URL(s);
            } catch (MalformedURLException ignored) {
                return null;
            }
        });
    }
}
