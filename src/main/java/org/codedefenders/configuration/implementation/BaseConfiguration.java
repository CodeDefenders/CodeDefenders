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

import org.codedefenders.configuration.Configuration;
import org.codedefenders.configuration.ConfigurationValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is the heart of the configuration reading process.
 *
 * <p>To add a configuration value which can be set by the user simply add a access method to the {@link Configuration}
 * Interface and one ore more class attributes to this file, which are used by the access method to return the
 * configuration value.
 *
 * <p>Attribute names have to be in camelCase format, as most subclasses reformat the attribute name and split the name
 * on the capitalized letters.
 *
 * <p>The subclasses of this file resolve the class attributes of this file with their implementation of the
 * {@link BaseConfiguration#resolveAttribute(String)} method.
 *
 * @author degenhart
 */
public abstract class BaseConfiguration extends Configuration {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected List<String> attributeSet = new ArrayList<>();

    @PostConstruct
    protected final void init() {
        Field[] fields = Configuration.class.getDeclaredFields();
        for (Field f : fields) {
            if (!f.getName().startsWith("$")) {
                Object prop = resolveAttribute(f.getName());
                if (prop != null) {
                    setField(f, prop);
                }
            }
        }

        try {
            validate();
        } catch (ConfigurationValidationException e) {
            logger.warn(e.getMessage());
        }
    }

    protected abstract Object resolveAttribute(String camelCaseName);

    protected final void setField(Field field, Object prop) {
        Class<?> t = field.getType();
        try {
            // TODO: Does this break when loading subclass objects through context lookups?
            //   e.g.: Having a field type Class and a prop SubClassOfClass?
            if (t == prop.getClass()) {
                field.set(this, prop);
                attributeSet.add(field.getName());
            } else if (prop.getClass() == String.class) {
                String value = (String) prop;
                if (t == Boolean.class) {
                    // TODO: Maybe only parse true, false, enabled, disabled and ignore the others?
                    field.set(this, Boolean.parseBoolean(value) || prop.equals("enabled"));
                    attributeSet.add(field.getName());
                } else if (t == Integer.class) {
                    field.set(this, Integer.parseInt(value));
                    attributeSet.add(field.getName());
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
