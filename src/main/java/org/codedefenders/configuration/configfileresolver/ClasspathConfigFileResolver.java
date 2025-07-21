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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ClassPathConfigFileResolver searches the named config file with a classpath search.
 *
 * @author degenhart
 */
@Singleton
class ClasspathConfigFileResolver extends ConfigFileResolver {
    private static final Logger logger = LoggerFactory.getLogger(ClasspathConfigFileResolver.class);

    @Override
    public Reader getConfigFile(String file) {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);

        if (inputStream == null) {
            return null;
        }

        return new InputStreamReader(inputStream);
    }
}
