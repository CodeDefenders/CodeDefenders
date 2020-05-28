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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;

public abstract class ConfigFileResolver {
    private static final Logger logger = LoggerFactory.getLogger(ConfigFileResolver.class);

    public abstract Reader getConfigFile(String filename);

    protected Reader getConfigFileImpl(String folder, String filename) {
        if (folder == null || folder.length() == 0) {
            return null;
        }
        File file = new File(folder, filename);

        try {
            return new InputStreamReader(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            logger.info("Couldn't open file at " + file.getAbsolutePath());
            return null;
        }
    }
}
