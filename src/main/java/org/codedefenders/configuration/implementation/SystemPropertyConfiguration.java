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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;
import java.lang.reflect.Field;

@Priority(10)
@Alternative
@Singleton
public class SystemPropertyConfiguration extends Configuration {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesFileConfiguration.class);

    @PostConstruct
    private void init() {
        Field[] fields = this.getClass().getSuperclass().getDeclaredFields();
        for (Field f : fields) {
            String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, f.getName()).replace('-', '.');
            String prop = System.getProperty(name);
            if (prop != null) {
                setField(f, prop);
            }
        }

        validate();
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

