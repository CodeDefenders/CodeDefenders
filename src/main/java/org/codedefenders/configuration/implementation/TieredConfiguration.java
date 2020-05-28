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

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Priority(100)
@Alternative
@Singleton
class TieredConfiguration extends Configuration {
    private static final Logger logger = LoggerFactory.getLogger(TieredConfiguration.class);

    List<Configuration> config;

    @Inject
    TieredConfiguration(SystemPropertyConfiguration sysPropConf,
                        EnvironmentVariableConfiguration envVarConf,
                        PropertiesFileConfiguration propFileConf) {
        config = new ArrayList<>();
        config.add(sysPropConf);
        config.add(envVarConf);
        config.add(propFileConf);
    }

    @PostConstruct
    private void readConfig() {
        logger.info("Assembling configuration");

        logger.info("System Properties: " + config.get(0).toString());
        logger.info("Environment Variables: " + config.get(1).toString());
        logger.info("Property File: " + config.get(2).toString());


        config.forEach(this::merge);

        logger.info("Tiered Configuration: " + this.toString());

        validate();
    }

    private void merge(Configuration otherConfig) {
        try {
            Field[] fields = this.getClass().getSuperclass().getDeclaredFields();
            Configuration defaultConfig = (Configuration) this.getClass().getSuperclass().newInstance();
            for (Field f : fields) {
                f.setAccessible(true);
                try {
                    // TODO Resolve superclass fields on other way?
                    Field otherConfField = otherConfig.getClass().getSuperclass().getDeclaredField(f.getName());
                    otherConfField.setAccessible(true);
                    Field defaultConfField = defaultConfig.getClass().getDeclaredField(f.getName());
                    defaultConfField.setAccessible(true);

                    Object otherConf = otherConfField.get(otherConfig);
                    String otherConfName = otherConfField.getName();
                    Object defaultConf = defaultConfField.get(defaultConfig);
                    String defaultConfName = defaultConfField.getName();
                    if (otherConf != null && defaultConf != null && otherConfName.equals(defaultConfName) && !otherConf.equals(defaultConf)) {
                        logger.info(otherConfig.getClass().getSimpleName() + " overwrote property " + f.getName());
                        f.set(this, otherConfField.get(otherConfig));
                    }
                } catch (IllegalAccessException e) {
                    logger.info("Could not access field " + f.getName());
                } catch (NoSuchFieldException e) {
                    logger.info("Could not access nonexistent field " + f.getName() );
                }
            }
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
    }

}
