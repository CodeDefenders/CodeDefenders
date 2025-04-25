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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ConfigFileResolver is the base class for a strategy pattern with the purpose of abstracting the process of
 * finding and accessing a file with a specified name.
 *
 * @author degenhart
 */
public abstract class ConfigFileResolver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public abstract Reader getConfigFile(String filename);

    protected Reader getConfigFileImpl(String path, String filename) {
        Reader result = null;
        if (path != null) {
            File file = new File(path);
            result = getReader(file);
            if (result == null) {
                result = getReader(new File(path, filename));
            }
        }
        return result;
    }

    private Reader getReader(File file) {
        if (file.isFile()) {
            try {
                return new InputStreamReader(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                logger.info("Couldn't open file at " + file.getAbsolutePath());
                return null;
            }
        }
        return null;
    }
}
