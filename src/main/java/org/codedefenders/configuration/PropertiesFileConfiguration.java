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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.Properties;

@ApplicationScoped
class PropertiesFileConfiguration extends Configuration {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesFileConfiguration.class);

    @PostConstruct
    private void readConfig() {

        Properties properties = readProperties();

        Field[] fields = this.getClass().getSuperclass().getDeclaredFields();
        for (Field f : fields) {
            String name = formatLowerDot(f.getName());
            String prop = properties.getProperty(name);
            if (prop != null) {
                setField(f, prop);
            }
        }

        validate();

    }

    private Properties readProperties() {
        Properties properties = new Properties();

        String confPath = System.getProperty("catalina.base") + "/conf/codedefenders.properties";

        try (Reader reader = new InputStreamReader(new FileInputStream(new File(confPath)))) {
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    private void setField(Field field, String prop) {
        Class<?> t = field.getType();
        try {
            if (t == String.class) {
                field.set(this, prop);
            } else if (t == Boolean.class) {
                field.set(this, Boolean.parseBoolean(prop));
            } else if (t == Integer.class) {
                field.set(this, Integer.parseInt(prop));
            }
        } catch (IllegalAccessException e) {
            logger.error("Can't set field " + field.getName() + " on Configuration class");
            logger.error(e.toString());
        }
    }
}
