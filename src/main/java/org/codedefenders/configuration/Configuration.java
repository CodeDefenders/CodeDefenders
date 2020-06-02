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

import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is the central place for accessing and defining the configuration for this application.
 *
 * <p>To add configuration values which can be set by the end user simply add one ore more attributes (best with default
 * value)and one ore more access methods which return/use the attributes to this class.
 *
 * <p>Attribute names have to be in camelCase format, as most implementations reformat the attribute name and split the
 * name on the capitalized letters.
 * So the {@link org.codedefenders.configuration.implementation.EnvironmentVariableConfiguration} would try to resolve
 * the {@code dbName} variable by looking for the environment variable {@code CODEDEFENDERS_DB_NAME}.
 *
 * @author degenhart
 */
@Priority(10)
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

    public void validate() throws ConfigurationValidationException {
        Set<String> validationErrors = new HashSet<>();

        // TODO: Do something useful here
        // assert getAntHome().isDirectory();
        // assert getDataDir().isDirectory();

        //noinspection UnstableApiUsage
        if (!(InetAddresses.isUriInetAddress(dbHost) || InternetDomainName.isValid(dbHost))) {
            validationErrors.add(resolveAttributeName("dbHost") + ": " + dbHost
                    + " is neither a valid ip address nor a valid hostname");
        }
        if (dbPort <= 0 || dbPort > 65535) {
            validationErrors.add(resolveAttributeName("dbPort") + ": " + dbPort
                    + " is not a valid port number");
        }

        //if (clusterMode) {
        //    // TODO: Validate clusterOptions
        //}
        if (!validationErrors.isEmpty()) {
            throw new ConfigurationValidationException(validationErrors);
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
        return "Configuration: \n"
                + "  dataDir='" + dataDir + "'\n"
                + "  antHome='" + antHome + "'\n"
                + "  dbHost='" + dbHost + "'\n"
                + "  dbPort=" + dbPort + "\n"
                + "  dbName='" + dbName + "'\n"
                + "  dbUsername='" + dbUsername + "'\n"
                + "  dbPassword='" + dbPassword + "'\n"
                + "  clusterMode=" + clusterMode + "\n"
                + "  clusterJavaHome='" + clusterJavaHome + "'\n"
                + "  clusterReservationName='" + clusterReservationName + "'\n"
                + "  clusterTimeout=" + clusterTimeout + "\n"
                + "  forceLocalExecution=" + forceLocalExecution + "\n"
                + "  parallelize=" + parallelize + "\n"
                + "  blockAttacker=" + blockAttacker + "\n"
                + "  mutantCoverage=" + mutantCoverage;
    }

    protected String resolveAttributeName(String camelCaseName) {
        return camelCaseName;
    }
}
