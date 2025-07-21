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
package org.codedefenders.configuration.implementation;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Properties;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.codedefenders.configuration.configfileresolver.ConfigFileResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;

/**
 * Loads the properties files found by the {@link ConfigFileResolver}s, merges them and then resolves the attributes by
 * looking the lower.dot.separated attribute names up in the merged properties.
 *
 * @author degenhart
 */
@Singleton
@ConfigurationSource
class PropertiesFileConfiguration extends BaseConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesFileConfiguration.class);

    private final Properties properties;

    @Inject
    PropertiesFileConfiguration(@Default Instance<ConfigFileResolver> configFileResolvers) {
        this(configFileResolvers.stream().toList());
    }

    PropertiesFileConfiguration(List<ConfigFileResolver> configFileResolvers) {
        super();
        properties = new Properties();
        readProperties(configFileResolvers, "codedefenders.properties");
    }

    @Override
    protected String resolveAttributeName(String camelCaseName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, camelCaseName).replace('-', '.');
    }

    @Override
    protected String resolveAttribute(String camelCaseName) {
        return properties.getProperty(resolveAttributeName(camelCaseName));
    }

    private void readProperties(List<ConfigFileResolver> loaders, String configFileName) {
        boolean noFileFound = true;

        for (ConfigFileResolver loader : loaders) {
            try (Reader reader = loader.getConfigFile(configFileName)) {
                if (reader != null) {
                    noFileFound = false;
                    logger.info("Loaded properties file found by " + loader.getClass().getSimpleName());
                    properties.load(reader);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (noFileFound) {
            logger.info("No properties files found");
        }
    }
}
