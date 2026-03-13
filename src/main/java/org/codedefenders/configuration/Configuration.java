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
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.codedefenders.configuration.source.ConfigurationSource;
import org.codedefenders.configuration.source.TieredSource;
import org.codedefenders.util.JavaVersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;

/**
 * Eagerly loads the configuration values into {@link BaseConfiguration} and validates the values.
 */
// TODO(kreismar): Move validation into BaseConfiguration
// TODO(kreismar): Move directory and file setup elsewhere
@Singleton
public class Configuration extends BaseConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    private final ConfigurationSource _source;
    private boolean _validated;
    private ConfigurationValidationException _configurationValidationException;

    // All the attributes need to be initialized with a null value and therefore need to be objects
    private Optional<URL> _appUrl;
    private boolean _appUrlCached;

    @Inject
    public Configuration(Instance<ConfigurationSource> sources) {
        this(new TieredSource(sources.stream()
                .sorted(Comparator.comparing(ConfigurationSource::getPriority))
                .toList()));
    }

    public Configuration(ConfigurationSource source) {
        this._source = source;
    }

    protected Optional<?> coerceType(String fieldName, Class<?> fieldType, @Nonnull String prop) {
        if (prop.isEmpty()) {
            return Optional.empty();
        } else if (fieldType == String.class) {
            return Optional.of(prop);
        } else if (fieldType == Boolean.class) {
            return Optional.of(Boolean.parseBoolean(prop) || prop.equals("enabled"));
        } else if (fieldType == Integer.class) {
            return Optional.of(Integer.parseInt(prop));
        }
        logger.warn("Couldn't match property {} to field {} with type {}",
            prop, fieldName, fieldType.getTypeName());
        return Optional.empty();
    }

    @PostConstruct
    public void init() {
        Field[] fields = BaseConfiguration.class.getDeclaredFields();
        for (Field field : fields) {
            Optional<String> value = _source.resolveAttribute(field.getName());
            if (value.isPresent()) {
                Optional<?> coercedValue = coerceType(field.getName(), field.getType(), value.get());
                if (coercedValue.isPresent()) {
                    try {
                        field.set(this, coercedValue.get());
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * Validates the currently configured Configuration.
     *
     * @throws ConfigurationValidationException This lists all the reasons why the validation failed.
     */
    public void validate() throws ConfigurationValidationException {
        if (!_validated) {
            List<String> validationErrors = new ArrayList<>();

            if (appUrl != null) {
                Optional<URL> realAppUrlOpt = getApplicationURL();
                if (realAppUrlOpt.isEmpty()) {
                    validationErrors.add("Property appUrl has invalid format");
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

            if (dataDir == null || dataDir.isEmpty()) {
                validationErrors.add("Property dataDir is missing");
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

            if (antHome == null || antHome.isEmpty()) {
                validationErrors.add("Property antHome is missing");
            } else {
                File antExecutable = new File(getAntHome(), "/bin/ant");
                if (!antExecutable.exists() || !antExecutable.isFile()) {
                    validationErrors.add("antHome doesn't contain the ant executable "
                            + antExecutable);
                }
            }

            if (antJavaHome != null && antJavaHome.trim().isEmpty()) {
                antJavaHome = null;
            }
            if (antJavaHome != null) {
                File javaExecutable = new File(antJavaHome, "/bin/java");
                if (!javaExecutable.exists() || !javaExecutable.isFile()) {
                    validationErrors.add("antJavaHome doesn't contain the java executable "
                            + javaExecutable);
                }
                Optional<Integer> antMajorJavaVersion = JavaVersionUtils.getMajorJavaVersionFromExecutable(
                        javaExecutable.toPath());
                if (antMajorJavaVersion.isEmpty()) {
                    validationErrors.add(String.format(
                        "%s: got an error while running the java executable '%s'. Please check the logs.",
                                    "antJavaHome", javaExecutable));
                } else {
                    antJavaVersion = antMajorJavaVersion.get();
                    if (antMajorJavaVersion.get() < 17) {
                        validationErrors.add("antJavaHome: Ant Java version must be >= 17");
                    }
                }
            }

            boolean dbvalid = true;
            if (dbHost == null || dbHost.isEmpty()) {
                validationErrors.add("Property dbHost is missing");
                dbvalid = false;
            } else {
                if (!(InetAddresses.isUriInetAddress(dbHost) || InternetDomainName.isValid(dbHost))) {
                    validationErrors.add("dbHost: " + dbHost
                            + " is neither a valid ip nor a valid hostname");
                    dbvalid = false;
                }
            }
            if (dbPort == null) {
                validationErrors.add("Property dbPort is missing");
                dbvalid = false;
            } else if (dbPort <= 0 || dbPort > 65535) {
                validationErrors.add("dbPort: " + dbPort + " is not a valid port number");
                dbvalid = false;
            }
            if (dbName == null || dbName.isEmpty()) {
                validationErrors.add("Property dbName is missing");
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
     * Checks if the given file exists, and if we can read it.
     * If the file doesn't exist, try to create it with the produceFile content.
     *
     * @param directory   The directory we write to.
     * @param filename    The name of the file we write.
     * @param produceFile A function which returns the content of the file.
     * @return Either an error message or null if the operation was successful.
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

    public Optional<URL> getApplicationURL() {
        if (!_appUrlCached) {
            _appUrl = super.getApplicationURL();
            _appUrlCached = true;
        }
        return _appUrl;
    }
}
