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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.google.common.truth.TruthJUnit.assume;

public class ResourceUtils {

    public static String loadResource(String resourceDir, String filename) {
        return loadResource(resourceDir + "/" + filename);
    }

    public static String loadResource(String filename) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(filename);
        assume().withMessage("File with name '%s' was not found in the class path.", filename)
                .that(url).isNotNull();
        Path path = Paths.get(url.getPath());

        try {
            return Files.readString(path);
        } catch (IOException e) {
            assume().withMessage("IOException while reading file '%s'.", path.toString()).fail();
            return null;
        }
    }
}
