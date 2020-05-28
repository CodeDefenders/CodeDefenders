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
package org.codedefenders.configuration;

import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;
import java.io.File;

/**
 * The Configuration class acts as interface for typesafe access to configuration properties.
 *
 * @author degenhart
 */
@Alternative
@Singleton
public class Configuration {
    protected String dataDir = "/srv/codedefenders";
    protected String antHome = "/usr/share/ant";

    protected String dbHost = "127.0.0.1";
    protected Integer dbPort = 3306;
    protected String dbName = "codedefenders";
    protected String dbUsername = "codedefenders";
    protected String dbPassword = "test";

    protected Boolean clusterMode = false;
    protected String clusterJavaHome;
    protected String clusterReservationName;
    protected Integer clusterTimeout = 2;

    protected Boolean forceLocalExecution = true;

    protected Boolean parallelize = true;
    protected Boolean blockAttacker = true;
    protected Boolean mutantCoverage = true;


    protected void validate() {
        // TODO: Do something useful here
        assert getAntHome().isDirectory();
        assert getDataDir().isDirectory();

        if (clusterMode) {
            // TODO: Validate clusterOptions
        }
    }

    public File getDataDir() {
        return new File(dataDir);
    }

    public File getAntHome() {
        return new File(antHome);
    }

    public String getDbUrl() {
        return "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
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
        return clusterTimeout;
    }

    public boolean isForceLocalExecution() {
        return forceLocalExecution;
    }

    public boolean isParallelize() {
        return parallelize;
    }

    public boolean isBlockAttacker() {
        return blockAttacker;
    }

    public boolean isMutantCoverage() {
        return mutantCoverage;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "dataDir='" + dataDir + '\'' +
                ", antHome='" + antHome + '\'' +
                ", dbHost='" + dbHost + '\'' +
                ", dbPort=" + dbPort +
                ", dbName='" + dbName + '\'' +
                ", dbUsername='" + dbUsername + '\'' +
                ", dbPassword='" + dbPassword + '\'' +
                ", clusterMode=" + clusterMode +
                ", clusterJavaHome='" + clusterJavaHome + '\'' +
                ", clusterReservationName='" + clusterReservationName + '\'' +
                ", clusterTimeout=" + clusterTimeout +
                ", forceLocalExecution=" + forceLocalExecution +
                ", parallelize=" + parallelize +
                ", blockAttacker=" + blockAttacker +
                ", mutantCoverage=" + mutantCoverage +
                '}';
    }
}
