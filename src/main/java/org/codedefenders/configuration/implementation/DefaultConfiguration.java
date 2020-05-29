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
package org.codedefenders.configuration.implementation;

import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.configuration.ConfigurationValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;
import java.io.File;
import java.lang.reflect.Field;

/**
 * This class is the heart of the configuration process.
 *
 * <p>To add a configuration value which can be set by the user simply add a access method to the {@link Configuration}
 * Interface and one ore more class attributes to this file, which are used by the access method to return the
 * configuration value.
 *
 * <p>Attribute names have to be in camelCase format, as most subclasses reformat the attribute name and split the name
 * on the capitalized letters.
 *
 * <p>The subclasses of this file resolve the class attributes of this file with their implementation of the
 * {@link DefaultConfiguration#resolveAttribute(String)} method.
 *
 * @author degenhart
 */
@Priority(10)
@Alternative
@Singleton
@DefaultConfig
public class DefaultConfiguration implements Configuration {
    private static final Logger logger = LoggerFactory.getLogger(DefaultConfiguration.class);

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

    @Override
    public void validate() throws ConfigurationValidationException {

        // TODO: Do something useful here
        assert getAntHome().isDirectory();
        assert getDataDir().isDirectory();

        //noinspection UnstableApiUsage
        if (!(InetAddresses.isUriInetAddress(dbHost) || InternetDomainName.isValid(dbHost))) {
            throw new ConfigurationValidationException("dbHost is neither a valid ip address nor a valid hostname");
        }

        if (clusterMode) {
            // TODO: Validate clusterOptions
        }

    }

    @Override
    public File getDataDir() {
        return new File(dataDir);
    }

    @Override
    public File getAntHome() {
        return new File(antHome);
    }

    @Override
    public String getDbUrl() {
        return "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;
    }

    @Override
    public String getDbUsername() {
        return dbUsername;
    }

    @Override
    public String getDbPassword() {
        return dbPassword;
    }

    @Override
    public boolean isClusterModeEnabled() {
        return clusterMode;
    }

    @Override
    public String getClusterJavaHome() {
        return clusterJavaHome;
    }

    @Override
    public String getClusterReservationName() {
        return clusterReservationName;
    }

    @Override
    public int getClusterTimeout() {
        return clusterTimeout;
    }

    @Override
    public boolean isForceLocalExecution() {
        return forceLocalExecution;
    }

    @Override
    public boolean isParallelize() {
        return parallelize;
    }

    @Override
    public boolean isBlockAttacker() {
        return blockAttacker;
    }

    @Override
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

    @PostConstruct
    protected final void init() {
        Field[] fields = getDefaultConfigClass().getDeclaredFields();
        for (Field f : fields) {
            Object prop = resolveAttribute(f.getName());
            if (prop != null) {
                setField(f, prop);
            }
        }

        try {
            validate();
        } catch (ConfigurationValidationException e) {
            logger.warn(e.getMessage());
        }
    }

    protected Object resolveAttribute(String camelCaseName) {
        return null;
    }

    protected final Class<DefaultConfiguration> getDefaultConfigClass() {
        Class<?> objClass = this.getClass();
        if (objClass != DefaultConfiguration.class) {
            objClass = objClass.getSuperclass();
            while (objClass != DefaultConfiguration.class) {
                objClass = objClass.getSuperclass();
                if (objClass == null) {
                    throw new RuntimeException("This class doesn't inherit from DefaultConfiguration");
                }
            }
        }
        return (Class<DefaultConfiguration>) objClass;
    }

    protected final void setField(Field field, Object prop) {
        Class<?> t = field.getType();
        try {
            // TODO: Does this break when loading subclass objects through context lookups?
            //   e.g.: Having a field type Class and a prop SubClassOfClass?
            if (t == prop.getClass()) {
                field.set(this, prop);
            } else if (prop.getClass() == String.class) {
                String value = (String) prop;
                if (t == Boolean.class) {
                    field.set(this, Boolean.parseBoolean(value) || prop.equals("enabled"));
                } else if (t == Integer.class) {
                    field.set(this, Integer.parseInt(value));
                } else {
                    logger.warn("Couldn't match property " + prop + " to field " + field.getName()
                            + " with Type " + t.getTypeName());
                }
            } else {
                logger.warn("Ignored property " + prop + " with unsupported type " + prop.getClass().getTypeName());
            }
        } catch (IllegalAccessException e) {
            logger.error("Can't set field " + field.getName() + " on Configuration class");
            logger.error(e.toString());
        }
    }
}
