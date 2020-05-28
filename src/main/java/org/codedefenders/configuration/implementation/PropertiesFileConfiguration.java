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
import org.codedefenders.configuration.Configuration;
import org.codedefenders.configuration.implementation.configfileresolver.ClasspathConfigFileResolver;
import org.codedefenders.configuration.implementation.configfileresolver.EnvironmentVariableConfigFileResolver;
import org.codedefenders.configuration.implementation.configfileresolver.SystemPropertyConfigFileLoader;
import org.codedefenders.configuration.implementation.configfileresolver.TomcatConfigFileResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.Properties;

@Priority(20)
@Alternative
@Singleton
class PropertiesFileConfiguration extends Configuration {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesFileConfiguration.class);

    @PostConstruct
    private void readConfig() {

        Properties properties = readProperties();

        Field[] fields = this.getClass().getSuperclass().getDeclaredFields();
        for (Field f : fields) {
            String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, f.getName()).replace('-', '.');
            String prop = properties.getProperty(name);
            if (prop != null) {
                setField(f, prop);
            }
        }

        validate();

    }

    private Properties readProperties() {
        Properties properties = new Properties();

        // TODO: Can we add these per Constructor injection to enable mocking?
        ConfigFileResolver[] loaders = new ConfigFileResolver[]{
                new SystemPropertyConfigFileLoader(),
                new EnvironmentVariableConfigFileResolver(),
                new TomcatConfigFileResolver(),
                new ClasspathConfigFileResolver()
        };

        for (ConfigFileResolver loader : loaders) {
            try {
                try (Reader reader = loader.getConfigFile("codedefenders.properties")) {
                    if (reader != null) {
                        logger.info("Loaded properties file found by " + loader.getClass().getSimpleName());
                        properties.load(reader);
                    } else {
                        logger.info(loader.getClass().getSimpleName() + " Didn't provided a reader!");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return properties;
    }

    // TODO Can this be part of the superClass?
    private void setField(Field field, String prop) {
        Class<?> t = field.getType();
        try {
            if (t == String.class) {
                field.set(this, prop);
            } else if (t == Boolean.class) {
                field.set(this, Boolean.parseBoolean(prop) || prop.equals("enabled"));
            } else if (t == Integer.class) {
                field.set(this, Integer.parseInt(prop));
            }
        } catch (IllegalAccessException e) {
            logger.error("Can't set field " + field.getName() + " on Configuration class");
            logger.error(e.toString());
        }
    }
}
