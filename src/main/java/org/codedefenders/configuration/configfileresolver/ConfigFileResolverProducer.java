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

package org.codedefenders.configuration.configfileresolver;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigFileResolverProducer {

    ClasspathConfigFileResolver classpathCfr;
    TomcatConfigFileResolver tomcatCfr;
    EnvironmentVariableConfigFileResolver environmentVarCfr;
    SystemPropertyConfigFileResolver systemPropertyCfr;

    @Inject
    ConfigFileResolverProducer(ClasspathConfigFileResolver classpathCfr,
                               TomcatConfigFileResolver tomcatCfr,
                               EnvironmentVariableConfigFileResolver environmentVarCfr,
                               SystemPropertyConfigFileResolver systemPropertyCfr) {
        this.classpathCfr = classpathCfr;
        this.tomcatCfr = tomcatCfr;
        this.environmentVarCfr = environmentVarCfr;
        this.systemPropertyCfr = systemPropertyCfr;
    }

    @Produces
    List<ConfigFileResolver> getConfigFileResolvers() {
        return new ArrayList<>(Arrays.asList(classpathCfr, tomcatCfr, environmentVarCfr, systemPropertyCfr));
    }
}
