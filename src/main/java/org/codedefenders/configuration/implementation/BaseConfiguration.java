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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import jakarta.annotation.PostConstruct;

import org.codedefenders.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the heart of the configuration reading process.
 *
 * <p>It does the heavy lifting: Iterating over all the attributes of the {@link Configuration} class, trying to resolve
 * them with the {@link BaseConfiguration#resolveAttribute(String)} method which has to be implemented by the subclasses
 * and then the Type matching/converting.
 *
 * @author degenhart
 */
abstract class BaseConfiguration extends Configuration {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @PostConstruct
    protected final void init() {
        Field[] fields = Configuration.class.getDeclaredFields();
        for (Field f : fields) {
            if (!f.getName().startsWith("_")) {
                Object prop = resolveAttribute(f.getName());
                if (prop != null) {
                    setField(f, prop);
                }
            }
        }
    }

    protected abstract Object resolveAttribute(String camelCaseName);

    /**
     * Tries to set the specified field on this object to the specified Object/Value.
     *
     * <p>This makes some basic type checking and conversion between string and other types.
     *
     * @param field The field on this object to set
     * @param prop  The value the field should be set
     */
    protected final void setField(Field field, Object prop) {
        Class<?> t = field.getType();
        if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
            try {
                if (t.isInstance(prop)) {
                    field.set(this, prop);
                } else if (prop.getClass() == String.class) {
                    String value = (String) prop;
                    if (value.isEmpty()) {
                        field.set(this, null);
                    } else if (t == Boolean.class) {
                        // TODO: Maybe only parse true, false, enabled, disabled and ignore the others?
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
                logger.error("Can't set field " + field.getName() + " on Configuration class", e);
            }
        }
    }
}
