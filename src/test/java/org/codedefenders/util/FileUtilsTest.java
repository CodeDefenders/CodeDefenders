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

import jakarta.enterprise.inject.Produces;

import org.codedefenders.configuration.Configuration;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Testing {@link FileUtils}.
 */
@ExtendWith(WeldJunit5Extension.class)
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

    @Test
    public void testGetPackagePathFromJavaFile() {
        Path emptyPath = Paths.get("");
        Path expected = Paths.get("top");
        final String legalFile = "package top;" + System.lineSeparator() + "public class Test {}";
        assertEquals(expected, FileUtils.getPackagePathFromJavaFile(legalFile));

        final String validFileWithLicenseText = """
                /*
                 * License
                 */
                package top;
                public class Test {}
                """;
        assertEquals(expected, FileUtils.getPackagePathFromJavaFile(validFileWithLicenseText));

        expected = Paths.get("top", "middle", "bottom");
        final String legalFileWithManyWhitespaces = "  package    \ttop .middle.  bottom  ;\t" + System.lineSeparator() + "public class Test {}"
                + System.lineSeparator();
        assertEquals(expected, FileUtils.getPackagePathFromJavaFile(legalFileWithManyWhitespaces));

        final String missingDeclaration = "public class Test {}";
        assertEquals(emptyPath, FileUtils.getPackagePathFromJavaFile(missingDeclaration));

        final String missingSemicolon = "package top" + System.lineSeparator() + "public class Test {}";
        assertEquals(emptyPath, FileUtils.getPackagePathFromJavaFile(missingSemicolon));
    }

    @Test
    public void testInvalidPackageDeclarations() {
        Path emptyPath = Paths.get("");
        final String packageInString =
                """
                        public class Test {
                            private String s = "package top;";
                        }

                        """;
        assertEquals(emptyPath, FileUtils.getPackagePathFromJavaFile(packageInString));

        final String packageInComment =
                """
                        public class Test {
                            //package top;
                        }
                        """;
        assertEquals(emptyPath, FileUtils.getPackagePathFromJavaFile(packageInComment));

        final String packageJustSlash =
                """
                        package /;
                        public class Test {
                        }
                        """;
        assertEquals(emptyPath, FileUtils.getPackagePathFromJavaFile(packageJustSlash));
    }

    @Test
    public void testGetAllFilesFromTypeInDirectory() throws IOException {
        Path dir = Files.createTempDirectory("codedefenders-testdir-");
        Path dir2 = dir.resolve("dir2");
        Path dir3 = dir2.resolve("dir3");
        Files.createDirectories(dir2);
        Files.createDirectories(dir3);
        Path java1 = dir.resolve("test1.java");
        Path java2 = dir2.resolve("test2.java");
        Path java3 = dir3.resolve("test3.java");

        Path class1 = dir.resolve("test1.class");
        Path class2 = dir2.resolve("test2.class");
        Path class3 = dir3.resolve("test3.class");

        Files.createFile(java1);
        Files.createFile(java2);
        Files.createFile(java3);
        Files.createFile(class1);
        Files.createFile(class2);
        Files.createFile(class3);
        List<Path> javaFiles = FileUtils.getAllFilesOfTypeInDirectory(dir, ".java");

        assertTrue(javaFiles.contains(java1));
        assertTrue(javaFiles.contains(java2));
        assertTrue(javaFiles.contains(java3));
        assertEquals(3, javaFiles.size());

    }

    @Test
    public void testDeleteEmptySubdirectories() throws IOException {
        Path root = Files.createTempDirectory("codedefenders-testdir-");
        Path tree1 = root.resolve("tree1"); //not empty
        Path tree2 = tree1.resolve("tree2"); //empty
        Path tree3 = tree2.resolve("tree3"); //empty
        Path subDir2 = root.resolve("subDir2"); //Not empty
        Path subDir3 = tree2.resolve("subDir3"); //empty

        Files.createDirectories(tree3);
        Files.createDirectories(subDir2);
        Files.createDirectories(subDir3);

        Path file1 = Files.createFile(tree1.resolve("file1.txt"));
        Path file2 = Files.createFile(subDir2.resolve("file2.txt"));

        FileUtils.deleteEmptySubdirectories(root.toFile());
        assertTrue(Files.exists(tree1));
        assertTrue(Files.exists(subDir2));
        assertTrue(Files.exists(file1));
        assertTrue(Files.exists(file2));

        assertFalse(Files.exists(tree2));
        assertFalse(Files.exists(tree3));
        assertFalse(Files.exists(subDir3));
    }
}
