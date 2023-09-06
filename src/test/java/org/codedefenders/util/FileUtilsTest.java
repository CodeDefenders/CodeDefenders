/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.enterprise.inject.Produces;

import org.codedefenders.configuration.Configuration;
import org.jboss.weld.junit4.WeldInitiator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Testing {@link FileUtils}.
 */
@ExtendWith(WeldExtension.class)
public class FileUtilsTest {

    // Required for mocking Configuration, which is loaded into a static field of FileUtils, required by GameClass.
    @WeldSetup
    public WeldInitiator weld =WeldInitiator.of(FileUtilsTest.class);

    @Produces
    public Configuration produceConfiguration() {
        return new Configuration() {};
    }


    @Test
    public void testCreateJavaTestFile() {
        String name = "Printer";
        String sourceCode = """
                public class Printer {
                   public static void main(String[] args) {
                       if (2 == 2) {
                           System.out.println("Hello World");
                       }
                   }
                }""".stripIndent();
        File dir;
        try {
            dir = Files.createTempDirectory("codedefenders-testdir-").toFile();
        } catch (IOException e) {
            assumeTrue(false, e.getMessage());
            return;
        }


        try {
            final String filePath = FileUtils.createJavaTestFile(dir, name, sourceCode);
            Path path = Paths.get(filePath);

            final String fileContent = new String(Files.readAllBytes(path));
            assertEquals(sourceCode, fileContent);

            final String expectedName = "Test" + name + ".java";
            Assertions.assertEquals(expectedName, path.getFileName().toString());

            assumeTrue(path.toFile().delete());
            Assumptions.assumeTrue(dir.delete());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetFirstSubDir() {
        Path dummyDirectory;
        Path dir;
        try {
            dummyDirectory = Files.createTempDirectory("dummyDirectory");
            dir = Files.createDirectory(dummyDirectory.resolve("000000021"));
        } catch (IOException e) {
            assumeTrue(false, e.getMessage());
            return;
        }
        File nextSubDir = FileUtils.getNextSubDir(dummyDirectory);

        String expected = dir.toString();
        assertNotEquals(expected, nextSubDir.toString());
        expected = expected.replaceAll("000000021", "00000022");
        assertEquals(expected, nextSubDir.toString());

        assumeTrue(dir.toFile().delete());
        assumeTrue(nextSubDir.delete());
        assumeTrue(dummyDirectory.toFile().delete());
    }

    @Test
    public void testReadEmptyLines() {
        final List<String> strings = FileUtils.readLines(Paths.get("doesnotexist/Test.java"));
        assertTrue(strings.isEmpty());
    }

    @Test
    public void testReadLines() {
        String name = "Printer";
        String sourceCode = """
                public class Printer {
                   public static void main(String[] args) {
                       if (2 == 2) {
                           System.out.println("Hello World");
                       }
                   }
                }""".stripIndent();
        File file;
        try {
            file = Files.createTempFile(name, ".java").toFile();
            Files.write(file.toPath(), sourceCode.getBytes());
        } catch (IOException e) {
            assumeTrue(false, e.getMessage());
            return;
        }

        final List<String> strings = FileUtils.readLines(file.toPath());
        assertFalse(strings.isEmpty());
        assertArrayEquals(sourceCode.split("\n"), strings.toArray(new String[0]));

        assumeTrue(file.delete());
    }

    @Test
    public void testExtractFileNameWithoutExtension() {
        String name = "TestName";

        Path tempDirectory;
        try {
            tempDirectory = Files.createTempDirectory("extension-test");
        } catch (IOException e) {
            assumeTrue(false, e.getMessage());
            return;
        }
        File file = new File(tempDirectory.toFile(), name);
        String result = FileUtils.extractFileNameNoExtension(file.toPath());

        assertEquals(name, result);

        assumeTrue(tempDirectory.toFile().delete());
    }

    @Test
    public void testExtractFileNameWithExtension() {
        String name = "TestName";
        String fileName = name + Constants.JAVA_SOURCE_EXT;

        Path tempDirectory;
        try {
            tempDirectory = Files.createTempDirectory("extension-test");
        } catch (IOException e) {
            assumeTrue(false, e.getMessage());
            return;
        }
        File file = new File(tempDirectory.toFile(), fileName);
        String result = FileUtils.extractFileNameNoExtension(file.toPath());

        assertEquals(name, result);

        assumeTrue(tempDirectory.toFile().delete());
    }

    @Test
    public void testGetFullyQualifiedNameValid() {
        final String classFilePath = "src/test/resources/itests/util/fileutils/Qualified.class";
        try {
            final String fullyQualifiedName = FileUtils.getFullyQualifiedName(classFilePath);

            final String expected = "org.codedefenders.util.Qualified";
            assertEquals(expected, fullyQualifiedName);

        } catch (IOException e) {
            assumeTrue(false, e.getMessage());
        }
    }

    @Test
    public void testGetFullyQualifiedNameFailing() {
        final String classFilePath = "src/test/resources/itests/util/fileutils/NoExists.class";
        String fullyQualifiedName = null;

        try {
            fullyQualifiedName = FileUtils.getFullyQualifiedName(classFilePath);
            fail("Shouldn't be able to read non existing file.");
        } catch (IOException e) {
            assertNull(fullyQualifiedName);
        }
    }

    @Test
    public void testStoreFile() {
        String fileName = "Printer.java";
        String sourceCode = """
                public class Printer {
                   public static void main(String[] args) {
                       if (2 == 2) {
                           System.out.println("Hello World");
                       }
                   }
                }""".stripIndent();
        Path dir;
        try {
            dir = Files.createTempDirectory("codedefenders-testdir-");
        } catch (IOException e) {
            assumeTrue(false, e.getMessage());
            return;
        }


        try {
            final Path path = FileUtils.storeFile(dir, fileName, sourceCode);

            final String fileContent = new String(Files.readAllBytes(path));
            assertEquals(sourceCode, fileContent);

            assertEquals(fileName, path.getFileName().toString());

            assumeTrue(path.toFile().delete());
            assumeTrue(dir.toFile().delete());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testStoreDuplicateFiles() {
        String fileName = "Printer.java";
        String sourceCode = """
                public class Printer {
                   public static void main(String[] args) {
                       if (2 == 2) {
                           System.out.println("Hello World");
                       }

                }""".stripIndent();
        Path dir;
        try {
            dir = Files.createTempDirectory("codedefenders-testdir-");
        } catch (IOException e) {
            assumeTrue(false, e.getMessage());
            return;
        }

        final Path path;
        try {
            path = FileUtils.storeFile(dir, fileName, sourceCode);
        } catch (IOException e) {
            assumeTrue(false, e.getMessage());
            assumeTrue(dir.toFile().delete());
            return;
        }
        Path path2 = null;
        try {
            path2 = FileUtils.storeFile(dir, fileName, sourceCode);
            fail("Shouldn't be able to create duplicate file.");
        } catch (IOException e) {
            assertNull(path2);
        }
        // storeFiles() shouldn't have deleted the directory since another file exists.
        assertTrue(dir.toFile().exists());

        assumeTrue(path.toFile().delete());
        assumeTrue(dir.toFile().delete());
    }
}
