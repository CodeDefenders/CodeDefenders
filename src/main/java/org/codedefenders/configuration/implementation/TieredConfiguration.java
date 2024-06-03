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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.codedefenders.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Merges other {@link BaseConfiguration}s in the order they are specified and then resolves attributes on the result.
 *
 * @author degenhart
 */
@Priority(100)
@Alternative
@Singleton
class TieredConfiguration extends BaseConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(TieredConfiguration.class);

    private final List<BaseConfiguration> configurations;

    @Inject
    TieredConfiguration(@SuppressWarnings("CdiInjectionPointsInspection") List<BaseConfiguration> configurations) {
        super();
        this.configurations = configurations;
    }

    @Override
    protected Object resolveAttribute(String camelCaseName) {
        Object result = null;
        try {
            Field field = Configuration.class.getDeclaredField(camelCaseName);
            if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                field.setAccessible(true);
                String propSetBy = null;
                for (BaseConfiguration otherConfig : configurations) {
                    Object otherConf = field.get(otherConfig);
                    if (otherConf != null) {
                        propSetBy = otherConfig.getClass().getSimpleName();
                        logger.debug("Property " + field.getName() + " present in " + propSetBy);
                        result = otherConf;
                    }
                }
                logger.info("Property " + field.getName() + (propSetBy != null ? " set by " + propSetBy : " not set."));
            }
        } catch (NoSuchFieldException e) {
            logger.info("Could not access nonexistent field ", e);
            // TODO Is this really impossible?
            throw new AssertionError("This shouldn't be possible!");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return result;
    }
}
