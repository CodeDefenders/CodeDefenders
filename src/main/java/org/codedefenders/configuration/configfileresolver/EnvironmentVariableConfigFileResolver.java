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
package org.codedefenders.configuration.configfileresolver;

import java.io.Reader;

import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The EnvironmentVariableConfigFileResolver looks up a path from an EnvironmentVariable and and tries to read the named
 * config file from this directory path.
 *
 * @author degenhart
 */
@Singleton
class EnvironmentVariableConfigFileResolver extends ConfigFileResolver {
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentVariableConfigFileResolver.class);

    @Override
    public Reader getConfigFile(String filename) {
        String envVar = System.getenv("CODEDEFENDERS_CONFIG");
        if (envVar == null) {
            return null;
        } else {
            return getConfigFileImpl(envVar, filename);
        }
    }

    @Override
    public int getPriority() {
        return 50;
    }
}

