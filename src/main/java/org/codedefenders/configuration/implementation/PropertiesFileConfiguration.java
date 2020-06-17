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

import com.google.common.base.CaseFormat;
import org.codedefenders.configuration.configfileresolver.ClasspathConfigFileResolver;
import org.codedefenders.configuration.configfileresolver.ConfigFileResolver;
import org.codedefenders.configuration.configfileresolver.EnvironmentVariableConfigFileResolver;
import org.codedefenders.configuration.configfileresolver.SystemPropertyConfigFileResolver;
import org.codedefenders.configuration.configfileresolver.TomcatConfigFileResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Loads the properties files found by the {@link ConfigFileResolver}s, merges them and then resolves the attributes by
 * looking the lower.dot.separated attribute names up in the merged properties.
 *
 * @author degenhart
 */
@Priority(20)
@Alternative
@Singleton
class PropertiesFileConfiguration extends BaseConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesFileConfiguration.class);

    private final Properties properties;

    // Maybe we could replace this in the future with injecting a list of ConfigFileResolver
    @Inject
    PropertiesFileConfiguration(List<ConfigFileResolver> configFileResolvers) {
        super();
        properties = readProperties(configFileResolvers);
    }

    @Override
    protected String resolveAttributeName(String camelCaseName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, camelCaseName).replace('-', '.');
    }

    @Override
    protected String resolveAttribute(String camelCaseName) {
        return properties.getProperty(resolveAttributeName(camelCaseName));
    }

    private Properties readProperties(List<ConfigFileResolver> loaders) {
        Properties properties = new Properties();

        boolean noFileFound = true;

        for (ConfigFileResolver loader : loaders) {
            try (Reader reader = loader.getConfigFile("codedefenders.properties")) {
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
        return properties;
    }
}
