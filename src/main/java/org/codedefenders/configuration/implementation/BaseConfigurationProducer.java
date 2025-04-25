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

import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.inject.Produces;

public class BaseConfigurationProducer {

    /**
     * Get a list of Configuration classes.
     * Configuration properties of classes further back in the list overwrite configuration properties of classes
     * further ahead in the list.
     *
     * @return A list of Configuration classes sorted by ascending priority.
     */
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Produces
    public List<BaseConfiguration> getConfiguration(SystemPropertyConfiguration sysPropConf,
            ContextConfiguration contextConf,
            EnvironmentVariableConfiguration envVarConf,
            PropertiesFileConfiguration propFileConf) {
        return Arrays.asList(propFileConf, envVarConf, sysPropConf, contextConf);
    }
}
