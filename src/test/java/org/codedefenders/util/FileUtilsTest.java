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

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Testing {@link FileUtils}.
 */
public class FileUtilsTest {

    @Test
    public void testCreateJavaTestFile() {
        String name = "Printer";
        String sourceCode = String.join("\n",
                "public class Printer {",
                "   public static void main(String[] args) {",
                "       if (2 == 2) {",
                "           System.out.println(\"Hello World\");",
                "       }",
                "   }",
                "}");
        File dir;
        try {
            dir = Files.createTempDirectory("codedefenders-testdir-").toFile();
        } catch (IOException e) {
            Assume.assumeNoException(e);
            return;
        }


        try {
            final String filePath = FileUtils.createJavaTestFile(dir, name, sourceCode);
            Path path = Paths.get(filePath);

            final String fileContent = new String(Files.readAllBytes(path));
            Assert.assertEquals(sourceCode, fileContent);

            final String expectedName = "Test" + name + ".java";
            Assert.assertEquals(expectedName, path.getFileName().toString());

            Assume.assumeTrue(path.toFile().delete());
            Assume.assumeTrue(dir.delete());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
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
            Assume.assumeNoException(e);
            return;
        }
        File nextSubDir = FileUtils.getNextSubDir(dummyDirectory);

        String expected = dir.toString();
        Assert.assertNotEquals(expected, nextSubDir.toPath());
        expected = expected.replaceAll("000000021", "00000022");
        Assert.assertEquals(expected, nextSubDir.toString());

        Assume.assumeTrue(dir.toFile().delete());
        Assume.assumeTrue(nextSubDir.delete());
        Assume.assumeTrue(dummyDirectory.toFile().delete());
    }

    @Test
    public void testReadEmptyLines() {
        final List<String> strings = FileUtils.readLines(Paths.get("doesnotexist/Test.java"));
        Assert.assertTrue(strings.isEmpty());
    }

    @Test
    public void testReadLines() {
        String name = "Printer";
        String sourceCode = String.join("\n",
                "public class Printer {",
                "   public static void main(String[] args) {",
                "       if (2 == 2) {",
                "           System.out.println(\"Hello World\");",
                "       }",
                "   }",
                "}");
        File file;
        try {
            file = Files.createTempFile(name, ".java").toFile();
            Files.write(file.toPath(), sourceCode.getBytes());
        } catch (IOException e) {
            Assume.assumeNoException(e);
            return;
        }

        final List<String> strings = FileUtils.readLines(file.toPath());
        Assert.assertFalse(strings.isEmpty());
        Assert.assertArrayEquals(sourceCode.split("\n"), strings.toArray(new String[0]));

        Assume.assumeTrue(file.delete());
    }

    @Test
    public void testExtractFileNameWithoutExtension() {
        String name = "TestName";

        Path tempDirectory;
        try {
            tempDirectory = Files.createTempDirectory("extension-test");
        } catch (IOException e) {
            Assume.assumeNoException(e);
            return;
        }
        File file = new File(tempDirectory.toFile(), name);
        String result = FileUtils.extractFileNameNoExtension(file.toPath());

        Assert.assertEquals(name, result);

        Assume.assumeTrue(tempDirectory.toFile().delete());
    }

    @Test
    public void testExtractFileNameWithExtension() {
        String name = "TestName";
        String fileName = name + Constants.JAVA_SOURCE_EXT;

        Path tempDirectory;
        try {
            tempDirectory = Files.createTempDirectory("extension-test");
        } catch (IOException e) {
            Assume.assumeNoException(e);
            return;
        }
        File file = new File(tempDirectory.toFile(), fileName);
        String result = FileUtils.extractFileNameNoExtension(file.toPath());

        Assert.assertEquals(name, result);

        Assume.assumeTrue(tempDirectory.toFile().delete());
    }

    @Test
    public void testGetFullyQualifiedNameValid() {
        final String classFilePath = "src/test/resources/itests/util/fileutils/Qualified.class";
        try {
            final String fullyQualifiedName = FileUtils.getFullyQualifiedName(classFilePath);

            final String expected = "org.codedefenders.util.Qualified";
            Assert.assertEquals(expected, fullyQualifiedName);

        } catch (IOException e) {
            Assume.assumeNoException("Qualified.class file should exist.", e);
        }
    }

    @Test
    public void testGetFullyQualifiedNameFailing() {
        final String classFilePath = "src/test/resources/itests/util/fileutils/NoExists.class";
        String fullyQualifiedName = null;

        try {
            fullyQualifiedName = FileUtils.getFullyQualifiedName(classFilePath);
            Assert.fail("Shouldn't be able to read non existing file.");
        } catch (IOException e) {
            Assert.assertNull(fullyQualifiedName);
        }
    }

    @Test
    public void testStoreFile() {
        String fileName = "Printer.java";
        String sourceCode = String.join("\n",
                "public class Printer {",
                "   public static void main(String[] args) {",
                "       if (2 == 2) {",
                "           System.out.println(\"Hello World\");",
                "       }",
                "   }",
                "}");
        Path dir;
        try {
            dir = Files.createTempDirectory("codedefenders-testdir-");
        } catch (IOException e) {
            Assume.assumeNoException(e);
            return;
        }


        try {
            final Path path = FileUtils.storeFile(dir, fileName, sourceCode);

            final String fileContent = new String(Files.readAllBytes(path));
            Assert.assertEquals(sourceCode, fileContent);

            Assert.assertEquals(fileName, path.getFileName().toString());

            Assume.assumeTrue(path.toFile().delete());
            Assume.assumeTrue(dir.toFile().delete());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testStoreDuplicateFiles() {
        String fileName = "Printer.java";
        String sourceCode = String.join("\n",
                "public class Printer {",
                "   public static void main(String[] args) {",
                "       if (2 == 2) {",
                "           System.out.println(\"Hello World\");",
                "       }",
                "   }",
                "}");
        Path dir;
        try {
            dir = Files.createTempDirectory("codedefenders-testdir-");
        } catch (IOException e) {
            Assume.assumeNoException(e);
            return;
        }

        final Path path;
        try {
            path = FileUtils.storeFile(dir, fileName, sourceCode);
        } catch (IOException e) {
            Assume.assumeNoException(e);
            Assume.assumeTrue(dir.toFile().delete());
            return;
        }
        Path path2 = null;
        try {
            path2 = FileUtils.storeFile(dir, fileName, sourceCode);
            Assert.fail("Shouldn't be able to create duplicate file.");
        } catch (IOException e) {
            Assert.assertNull(path2);
        }
        // storeFiles() shouldn't have deleted the directory since another file exists.
        Assert.assertTrue(dir.toFile().exists());

        Assume.assumeTrue(path.toFile().delete());
        Assume.assumeTrue(dir.toFile().delete());
    }
}