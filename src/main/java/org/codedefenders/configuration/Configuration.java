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
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

import org.codedefenders.util.JavaVersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;

/**
 * This class is the central place for accessing and defining the configuration for this application.<br><br>
 *
 * <p>It forms an adapter between the internal accessible configuration and the values configured by the user.
 * This provides us a typesafe way to access the configuration and also allows us to easily change the internal api
 * while keeping the external API (the user configured values) stable.
 * This can additionally be used to provide internal configuration or feature switches which shouldn't be accessible to
 * the user at the moment, but could be published for usage at a later point in time. (This would be implemented by a
 * method which has no baking variable and returns a constant value.)<br><br>
 *
 * <h3>Usage</h3>
 *
 * <p>To add configuration values which can be set by the end user simply add one ore more attributes and one ore more
 * access methods which return/use the attributes to this class.<br>
 * All class attributes <b>must be null initialized</b> objects and <b>must not be</b> primitives.<br>
 * <b>Do not</b> add default values to the class attributes! If you want to provide a default value, set it through the
 * `src/main/resources/codedefenders.properties` file.<br><br>
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
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    private boolean _validated;
    private ConfigurationValidationException _configurationValidationException;

    // All the attributes need to be initialized with a null value and therefore need to be objects
    protected String appUrl;
    protected Optional<URL> _appUrl;
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
    protected Integer parallelizeKillmapCount;
    protected Boolean blockAttacker;
    protected Boolean mutantCoverage;

    @Deprecated
    protected String authAdminRole;
    protected String authAdminUsers;

    protected Boolean metrics;

    protected Boolean javamelody;

    /**
     * Validates the currently configured Configuration.
     *
     * @throws ConfigurationValidationException This lists all the reasons why the validation failed.
     */
    public final void validate() throws ConfigurationValidationException {
        if (!_validated) {
            List<String> validationErrors = new ArrayList<>();

            if (appUrl != null) {
                Optional<URL> realAppUrlOpt = getApplicationURL();
                if (realAppUrlOpt.isEmpty()) {
                    validationErrors.add("Property " + resolveAttributeName("appUrl") + " has invalid format");
                } else {
                    URL realAppUrl = realAppUrlOpt.get();
                    if (realAppUrl.getProtocol() == null
                            || realAppUrl.getHost() == null
                            || realAppUrl.getUserInfo() != null
                            || realAppUrl.getQuery() != null
                            || realAppUrl.getRef() != null) {
                        validationErrors.add("App url invalid");
                    }
                }
            }

            if (dataDir == null || dataDir.equals("")) {
                validationErrors.add("Property " + resolveAttributeName("dataDir") + " is missing");
            } else {
                File dataDir = getDataDir();
                String dataDirCreate = setupDirectory(dataDir);
                if (dataDirCreate != null) {
                    validationErrors.add(dataDirCreate);
                } else {
                    validationErrors.add(setupFile(dataDir, "build.xml",
                            () -> this.getClass().getResourceAsStream("/data/build.xml")));
                    validationErrors.add(setupFile(dataDir, "security.policy",
                            () -> this.getClass().getResourceAsStream("/data/security.policy")));

                    validationErrors.add(setupDirectory(getMutantDir()));
                    validationErrors.add(setupDirectory(getTestsDir()));
                    validationErrors.add(setupDirectory(getSourcesDir()));

                    validationErrors.add(setupDirectory(getLibraryDir()));
                }
            }

            if (antHome == null || antHome.equals("")) {
                validationErrors.add("Property " + resolveAttributeName("antHome") + " is missing");
            } else {
                File antExecutable = new File(getAntHome(), "/bin/ant");
                if (!antExecutable.exists() || !antExecutable.isFile()) {
                    validationErrors.add(resolveAttributeName("antHome") + " doesn't contain the ant executable "
                            + antExecutable);
                }
            }

            if (antJavaHome != null && antJavaHome.trim().isEmpty()) {
                antJavaHome = null;
            }
            if (antJavaHome != null) {
                File javaExecutable = new File(antJavaHome, "/bin/java");
                if (!javaExecutable.exists() || !javaExecutable.isFile()) {
                    validationErrors.add(resolveAttributeName("antJavaHome") + " doesn't contain the java executable "
                            + javaExecutable);
                }
                Optional<Integer> antMajorJavaVersion = JavaVersionUtils.getMajorJavaVersionFromExecutable(
                        javaExecutable.toPath());
                if (antMajorJavaVersion.isEmpty()) {
                    validationErrors.add(String.format("%s: got an error while running the java executable '%s'. Please check the logs.",
                                    resolveAttributeName("antJavaHome"), javaExecutable));
                } else {
                    antJavaVersion = antMajorJavaVersion.get();
                    if (antMajorJavaVersion.get() < 17) {
                        validationErrors.add(resolveAttributeName("antJavaHome") + ": Ant Java version must be >= 17");
                    }
                }
            }

            boolean dbvalid = true;
            if (dbHost == null || dbHost.equals("")) {
                validationErrors.add("Property " + resolveAttributeName("dbHost") + " is missing");
                dbvalid = false;
            } else {
                if (!(InetAddresses.isUriInetAddress(dbHost) || InternetDomainName.isValid(dbHost))) {
                    validationErrors.add(resolveAttributeName("dbHost") + ": " + dbHost
                            + " is neither a valid ip nor a valid hostname");
                    dbvalid = false;
                }
            }
            if (dbPort == null) {
                validationErrors.add("Property " + resolveAttributeName("dbPort") + " is missing");
                dbvalid = false;
            } else if (dbPort <= 0 || dbPort > 65535) {
                validationErrors.add(resolveAttributeName("dbPort") + ": " + dbPort + " is not a valid port number");
                dbvalid = false;
            }
            if (dbName == null || dbName.equals("")) {
                validationErrors.add("Property " + resolveAttributeName("dbName") + " is missing");
                dbvalid = false;
            }
            if (dbvalid) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    try (Connection conn = DriverManager.getConnection(getDbUrl(), getDbUsername(), getDbPassword())) {
                        if (!conn.isValid(10)) { // 10 sec
                            validationErrors.add("Can't get a valid connection within 10 seconds!");
                        }
                    } catch (SQLException e) {
                        logger.debug("The following SQLException occurred while trying to validate "
                                + "the database connection", e);
                        if (e.getMessage().contains("Access denied")) {
                            validationErrors.add("Can't connect to the database. " + e.getMessage());
                        } else {
                            validationErrors.add("Can't connect to the database. " + e.getMessage() + "\n"
                                    + "Please check the database and the connection settings. ");
                        }
                    }
                } catch (ClassNotFoundException e) {
                    validationErrors.add("Could not load the MySQL driver");
                }
            }

            if (JavaVersionUtils.getJavaMajorVersion() < 17) {
                validationErrors.add("Unsupported java version! CodeDefenders needs at least Java 17.");
            }

            /*
            if (clusterMode) {
                // TODO: Validate clusterOptions
            }
             */

            validationErrors.removeIf(Objects::isNull);
            _validated = true;
            if (!validationErrors.isEmpty()) {
                _configurationValidationException = new ConfigurationValidationException(validationErrors);
                throw _configurationValidationException;
            }
        } else {
            if (_configurationValidationException != null) {
                throw _configurationValidationException;
            }
        }
    }

    /**
     * Checks if the given file exists and we can read it.
     * If the file doesn't exist, try to create it with the produceFile content.
     *
     * @param directory   The directory we write to.
     * @param filename    The name of the file we write.
     * @param produceFile A function which returns the content of the file.
     * @return Either a error message or null if the operation was successful.
     */
    private String setupFile(File directory, String filename, Supplier<InputStream> produceFile) {
        File file = new File(directory, filename);
        try {
            Files.copy(produceFile.get(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Created/Overwrote file " + file.toPath().toAbsolutePath());
            return null;
        } catch (AccessDeniedException e) {
            return "Can't write to " + file.toPath() + ". Please check the permissions on the "
                    + directory.toPath() + " directory!";
        } catch (DirectoryNotEmptyException e) {
            return "Can't overwrite " + file.toPath() + " because it is a non empty directory! "
                    + "Please remove this directory!";
        } catch (FileSystemException e) {
            return "The file " + file.toPath() + " doesn't exist, and we can't create it!";
        } catch (IOException e) {
            logger.debug("IOException when trying to write file", e);
            return "Other error when trying to write file!";
        }
    }

    private String setupDirectory(File directory) {
        if (directory.exists() && directory.isDirectory() && !directory.canWrite()) {
            return "Can't write to directory " + directory.toPath()
                    + ". Please check the directory permissions";
        }
        try {
            Files.createDirectories(directory.toPath());
            return null;
        } catch (FileAlreadyExistsException e) {
            return "The path " + directory.toPath() + " already exists, but is no directory!";
        } catch (FileSystemException e) {
            String message = "The directory " + directory.toPath() + " doesn't exist, and we can't create ";
            if (e.getFile().equals(directory.toPath().toAbsolutePath().toString())) {
                message += "it";
            } else {
                message += "the intermediate directory " + e.getFile();
            }
            message += ". Reason: " + e.getReason();
            return message;
        } catch (IOException e) {
            logger.debug("IOException when trying to create directory", e);
            return "Other error when trying to create directory!";
        }
    }

    public boolean isValid() {
        try {
            validate();
        } catch (ConfigurationValidationException e) {
            return false;
        }
        return true;
    }


    /**
     * @return A URL that has a protocol, host, path, and optional a port.
     */
    public Optional<URL> getApplicationURL() {
        //noinspection OptionalAssignedToNull
        if (_appUrl == null) {
            _appUrl = Optional.ofNullable(appUrl)
                    .filter(s -> !s.trim().isEmpty())
                    .map(s -> {
                        try {
                            return new URL(s);
                        } catch (MalformedURLException ignored) {
                            return null;
                        }
                    });
        }
        return _appUrl;
    }

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
     * This transforms an attribute name from camelCase to the format in which its actually looked up.
     *
     * @param camelCaseName The attribute name in camelCaseFormat
     * @return The attribute name in the format it is looked up.
     */
    protected String resolveAttributeName(String camelCaseName) {
        return camelCaseName;
    }
}
