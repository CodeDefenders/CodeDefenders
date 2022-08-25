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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;

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
    private boolean $validated;
    private ConfigurationValidationException $configurationValidationException;

    // All the attributes need to be initialized with a null value and therefore need to be objects
    protected String dataDir;
    protected String antHome;
    protected String javaHome;
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
    protected Boolean blockAttacker;
    protected Boolean mutantCoverage;

    /**
     * Validates the currently configured Configuration.
     *
     * @throws ConfigurationValidationException This lists all the reasons why the validation failed.
     */
    public final void validate() throws ConfigurationValidationException {
        if (!$validated) {
            List<String> validationErrors = new ArrayList<>();

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

                    validationErrors.add(setupDirectory(getAiDir()));
                    validationErrors.add(setupDirectory(getMutantDir()));
                    validationErrors.add(setupDirectory(getTestsDir()));
                    validationErrors.add(setupDirectory(getSourcesDir()));

                    validationErrors.add(setupDirectory(getLibraryDir()));
                    // TODO: Replace this with something which can resolve the dependencies.
                    try (Stream<Path> entries = Files.list(getLibraryDir().toPath())) {
                        if (!entries.findFirst().isPresent()) {
                            validationErrors.add("The library directory " + getLibraryDir().toPath().toString()
                                    + " is empty! Please download the dependencies via the installation-pom.xml!");
                        }
                    } catch (IOException ignored) {
                        // ignored
                    }
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

            javaHome = javaHome.trim().isEmpty() ? null : javaHome;
            if (javaHome != null) {
                File javaExecutable = new File(javaHome, "/bin/java");
                if (!javaExecutable.exists() || !javaExecutable.isFile()) {
                    validationErrors.add(resolveAttributeName("javaHome") + " doesn't contain the java executable "
                            + javaExecutable);
                }
            }

            boolean dbvalid = true;
            if (dbHost == null || dbHost.equals("")) {
                validationErrors.add("Property " + resolveAttributeName("dbHost") + " is missing");
                dbvalid = false;
            } else { //noinspection UnstableApiUsage
                if (!(InetAddresses.isUriInetAddress(dbHost) || InternetDomainName.isValid(dbHost))) {
                    validationErrors.add(resolveAttributeName("dbHost") + ": " + dbHost
                            + " is neither a valid ip nor a valid hostname");
                    dbvalid = false;
                }
            }
            if (dbPort == null) {
                validationErrors.add("Property " + resolveAttributeName("dbPort") + " is missing");
                dbvalid = false;
            } else if (dbPort <= 0 | dbPort > 65535) {
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

            if (getJavaMajorVersion() > 11) {
                validationErrors.add("Unsupported java version! CodeDefenders needs at most Java 11");
            }

            /*
            if (clusterMode) {
                // TODO: Validate clusterOptions
            }
             */

            validationErrors.removeIf(Objects::isNull);
            $validated = true;
            if (!validationErrors.isEmpty()) {
                $configurationValidationException = new ConfigurationValidationException(validationErrors);
                throw $configurationValidationException;
            }
        } else {
            if ($configurationValidationException != null) {
                throw $configurationValidationException;
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
            logger.info("Created/Overwrote file " + file.toPath().toAbsolutePath().toString());
            return null;
        } catch (AccessDeniedException e) {
            return "Can't write to " + file.toPath().toString() + ". Please check the permissions on the "
                    + directory.toPath().toString() + " directory!";
        } catch (DirectoryNotEmptyException e) {
            return "Can't overwrite " + file.toPath().toString() + " because it is a non empty directory! "
                    + "Please remove this directory!";
        } catch (FileSystemException e) {
            return "The file " + file.toPath().toString() + " doesn't exist, and we can't create it!";
        } catch (IOException e) {
            logger.debug("IOException when trying to write file", e);
            return "Other error when trying to write file!";
        }
    }

    private String setupDirectory(File directory) {
        if (directory.exists() && directory.isDirectory() && !directory.canWrite()) {
            return "Can't write to directory " + directory.toPath().toString()
                    + ". Please check the directory permissions";
        }
        try {
            Files.createDirectories(directory.toPath());
            return null;
        } catch (FileAlreadyExistsException e) {
            return "The path " + directory.toPath().toString() + " already exists, but is no directory!";
        } catch (FileSystemException e) {
            String message = "The directory " + directory.toPath().toString() + " doesn't exist, and we can't create ";
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
        return new File(getDataDir(), "lib");
    }

    public File getAiDir() {
        return new File(getDataDir(), "ai");
    }

    public File getAntHome() {
        return new File(antHome);
    }

    public Optional<File> getJavaHome() {
        return javaHome == null
                ? Optional.empty()
                : Optional.of(new File(javaHome));
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
        if (clusterTimeout == null) {
            return -1;
        } else {
            return clusterTimeout;
        }
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

    public int getNumberOfKillmapThreads() {
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
