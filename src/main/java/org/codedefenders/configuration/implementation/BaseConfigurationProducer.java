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

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BaseConfigurationProducer {

    SystemPropertyConfiguration sysPropConf;
    ContextConfiguration contextConf;
    EnvironmentVariableConfiguration envVarConf;
    PropertiesFileConfiguration propFileConf;

    @Inject
    BaseConfigurationProducer(SystemPropertyConfiguration sysPropConf,
                              ContextConfiguration contextConf,
                              EnvironmentVariableConfiguration envVarConf,
                              PropertiesFileConfiguration propFileConf) {
        this.sysPropConf = sysPropConf;
        this.contextConf = contextConf;
        this.envVarConf = envVarConf;
        this.propFileConf = propFileConf;
    }

    @Produces
    public List<BaseConfiguration> getConfiguration() {
        return new ArrayList<>(Arrays.asList(sysPropConf, contextConf, envVarConf, propFileConf));
    }
}
