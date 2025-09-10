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
import java.io.StringReader;

import jakarta.inject.Singleton;

/**
 * The StubConfigFileResolver is used for testing the
 * {@link org.codedefenders.configuration.implementation.PropertiesFileConfiguration}.
 *
 * <p>As the returned content (the reader for a config file) can be set by the developer.
 *
 * @author degenhart
 */
@Singleton
public class StubConfigFileResolver extends ConfigFileResolver {
    private String configFileContent;

    @Override
    public Reader getConfigFile(String filename) {
        return new StringReader(configFileContent);
    }

    public void setConfigFileContent(String configFileContent) {
        this.configFileContent = configFileContent;
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
