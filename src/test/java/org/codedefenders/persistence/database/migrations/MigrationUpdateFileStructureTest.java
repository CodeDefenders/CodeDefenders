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
package org.codedefenders.persistence.database.migrations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.codedefenders.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.codedefenders.persistence.database.migrations.V34__UpdateFileStructure.*;
import static org.junit.jupiter.api.Assertions.*;

class MigrationUpdateFileStructureTest {

    @Test
    void moveClassFilesTest(@TempDir Path root) throws IOException {
        Path sourcePath = root.resolve("Cut$inner").resolve("org");
        Path depPath = sourcePath.resolve("deps");
        Path targetPath = root.resolve("target");
        Files.createDirectories(sourcePath);
        Files.createDirectories(depPath);
        Files.createDirectories(targetPath);

        Files.createFile(sourcePath.resolve("Cut$inner.class"));
        Files.createFile(sourcePath.resolve("Cut$inner$inner.class"));
        Files.createFile(sourcePath.resolve("Cut.class"));

        Files.createFile(sourcePath.resolve("NotCut$inner.class"));
        Files.createFile(depPath.resolve("Cut$inner.class"));
        Files.createFile(depPath.resolve("Cut.class"));

        moveClassFiles(sourcePath, targetPath, "Cut");

        assertTrue(Files.exists(targetPath.resolve("Cut$inner.class")));
        assertTrue(Files.exists(targetPath.resolve("Cut$inner$inner.class")));
        assertTrue(Files.exists(targetPath.resolve("Cut.class")));

        assertFalse(Files.exists(sourcePath.resolve("Cut$inner.class")));
        assertFalse(Files.exists(sourcePath.resolve("Cut$inner$inner.class")));
        assertFalse(Files.exists(sourcePath.resolve("Cut.class")));

        assertFalse(Files.exists(targetPath.resolve("NotCut$inner.class")));

        assertTrue(Files.exists(depPath.resolve("Cut$inner.class")));
        assertTrue(Files.exists(depPath.resolve("Cut.class")));
        assertTrue(Files.exists(sourcePath.resolve("NotCut$inner.class")));
    }

}
