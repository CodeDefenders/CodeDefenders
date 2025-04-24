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
import java.io.Reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The TomcatConfigFileResolver tries to read the named configuration file from the tomcat config directory which
 * resides in {@code $CATALINA_BASE/config}.
 *
 * @author degenhart
 */
class TomcatConfigFileResolver extends ConfigFileResolver {
    private static final Logger logger = LoggerFactory.getLogger(TomcatConfigFileResolver.class);

    @Override
    public Reader getConfigFile(String filename) {
        String tomcatRoot = System.getProperty("catalina.base");
        if (tomcatRoot == null || tomcatRoot.length() == 0) {
            return null;
        }
        File folder = new File(tomcatRoot, "conf");
        // Prohibit loading of configuration from a potentially `conf` file in the `catalina.base directory.
        if (!folder.isDirectory()) {
            return null;
        }
        return getConfigFileImpl(folder.getAbsolutePath(), filename);
    }
}
