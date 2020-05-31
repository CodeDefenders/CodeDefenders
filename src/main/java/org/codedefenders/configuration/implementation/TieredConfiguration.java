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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * @author degenhart
 */
@Priority(100)
@Alternative
@Singleton
class TieredConfiguration extends BaseConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(TieredConfiguration.class);

    private final List<BaseConfiguration> config;

    @Inject
    TieredConfiguration(SystemPropertyConfiguration sysPropConf,
                        EnvironmentVariableConfiguration envVarConf,
                        PropertiesFileConfiguration propFileConf) {
        this((BaseConfiguration) sysPropConf,
                envVarConf,
                propFileConf);
    }

    TieredConfiguration(BaseConfiguration... configurations) {
        super();
        config = Arrays.asList(configurations);
    }

    @Override
    protected Object resolveAttribute(String camelCaseName) {
        Object result = null;
        try {
            Field field = Configuration.class.getDeclaredField(camelCaseName);
            field.setAccessible(true);
            for (BaseConfiguration otherConfig : config) {
                Object otherConf = field.get(otherConfig);
                if (otherConf != null && otherConfig.attributeSet.contains(field.getName())) {
                    logger.info(otherConfig.getClass().getSimpleName() + " overwrote property " + field.getName());
                    result = otherConf;
                }
            }
        } catch (NoSuchFieldException e) {
            logger.info("Could not access nonexistent field " + e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return result;
    }
}
