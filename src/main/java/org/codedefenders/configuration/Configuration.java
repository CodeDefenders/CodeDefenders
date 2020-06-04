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

import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is the central place for accessing and defining the configuration for this application.<br><br>
 *
 * <p>It forms an adapter between the internal accessible configuration and the values configured by the user.
 * This provides us a typesafe way to access the configuration and also allows us to easily change the internal api
 * while keeping the external API (the user configured values) stable.
 * This additionally be used to provide internal configuration or feature switches which shouldn't be accessible to the
 * user at the moment, but could be published for usage at a later point in time. (This would be implemented by a method
 * which has no baking variable and returns a constant value.)<br><br>
 *
 * <h3>Usage</h3>
 *
 * <p>To add configuration values which can be set by the end user simply add one ore more attributes (best with a
 * sensible default value) and one ore more access methods which return/use the attributes to this class.<br><br>
 *
 * <p>Attribute names have to be in camelCase format, as most implementations reformat the attribute name and split the
 * name on the capitalized letters.
 * So the {@link org.codedefenders.configuration.implementation.EnvironmentVariableConfiguration} would try to resolve
 * the {@code dbName} variable by looking for the environment variable {@code CODEDEFENDERS_DB_NAME}.
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

    /**
     * Validates the currently configured Configuration.
     *
     * @throws ConfigurationValidationException This lists all the reasons why the validation failed.
     */
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

        if (getJavaMajorVersion() > 9) {
            validationErrors.add("Unsupported java version! CodeDefenders needs at most Java 9");
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

    public int getNumberOfThreads() {
        return 40;
    }

    private int getJavaMajorVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        /* Allow these formats:
         * 1.8.0_72-ea
         * 9-ea
         * 9
         * 9.0.1
         */
        int dotPos = version.indexOf('.');
        int dashPos = version.indexOf('-');
        return Integer.parseInt(version.substring(0,
                dotPos > -1 ? dotPos : dashPos > -1 ? dashPos : 1));
    }

    @Override
    public String toString() {
        // This could potentially be simplified maybe override this in BaseConfiguration with iterating over the attrs.
        return "Configuration: \n"
                + "  " + resolveAttributeName("dataDir") + "= '" + dataDir + "'\n"
                + "  " + resolveAttributeName("antHome") + "= '" + antHome + "'\n "
                + "  " + resolveAttributeName("dbHost") + "= '" + dbHost + "'\n"
                + "  " + resolveAttributeName("dbPort") + "= " + dbPort + "\n"
                + "  " + resolveAttributeName("dbName") + "= '" + dbName + "'\n"
                + "  " + resolveAttributeName("dbUsername") + "= '" + dbUsername + "'\n"
                + "  " + resolveAttributeName("dbPassword") + "= '" + dbPassword + "'\n"
                + "  " + resolveAttributeName("clusterMode") + "= " + clusterMode + "\n"
                + "  " + resolveAttributeName("clusterJavaHome") + "= '" + clusterJavaHome + "'\n"
                + "  " + resolveAttributeName("clusterReservationName") + "= '" + clusterReservationName
                + "'\n"
                + "  " + resolveAttributeName("clusterTimeout") + "= " + clusterTimeout + "\n"
                + "  " + resolveAttributeName("forceLocalExecution") + "= " + forceLocalExecution + "\n"
                + "  " + resolveAttributeName("parallelize") + "= " + parallelize + "\n"
                + "  " + resolveAttributeName("blockAttacker") + "= " + blockAttacker + "\n"
                + "  " + resolveAttributeName("mutantCoverage") + "= " + mutantCoverage;
    }

    /**
     * This transforms an attribute name from camelCase to the format in which its actually looked up.
     *
     * @param camelCaseName The attribute name in camelCaseFormat
     * @return The attribute name in the format it is looked up.
     */
    protected String resolveAttributeName(String camelCaseName) {
        return camelCaseName;
    }
}
