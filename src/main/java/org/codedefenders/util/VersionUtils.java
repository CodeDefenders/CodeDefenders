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
package org.codedefenders.util;

import java.io.IOException;
import java.util.Properties;

import org.codedefenders.game.GameClass;

public class VersionUtils {

    /**
     * Returns the abbreviation of the git commit id as located in the git.properties file.
     * @return A seven-character abbreviation of the git commit id. If the git.properties file could not be read,
     * return {@code null}
     */
    public static String getGitCommitId() {
        try {
            Properties prop = gitProperties();
            return prop.getProperty("git.commit.id.abbrev");
        } catch (IOException ignored) {
            // Ignore -- if we have no version, then we show no version section
            return null;
        }
    }

    /**
     * Returns if the current version is dirty.
     * @return {@code true}, if the version is dirty, i.e., there are uncommitted changes,
     * {@code false} if it is not dirty or the git.properties file could not be read.
     */
    public static boolean getGitDirty() {
        try {
            Properties prop = gitProperties();
            return Boolean.parseBoolean(prop.getProperty("git.dirty"));
        } catch (IOException ignored) {
            // Ignore -- if we have no version, then we show no version section
            return false;
        }
    }

    public static String getCodeDefendersVersion() {
        String version = GameClass.class.getPackage().getImplementationVersion();

        // version may now be null: https://stackoverflow.com/questions/21907528/war-manifest-mf-and-version?rq=1
        if (version == null) {
            Properties prop = new Properties();
            try {
                prop.load(GameClass.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
                return prop.getProperty("Implementation-Version");
            } catch (IOException e) {
                // Ignore -- if we have no version, then we show no version section
            }
        }

        return null;
    }

    private static Properties gitProperties() throws IOException {
        Properties prop = new Properties();
        prop.load(VersionUtils.class.getResourceAsStream("/git.properties"));
        return prop;
    }
}
