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
    void replaceTest() {
        try (MockedStatic<FileUtils> mockedStatic = Mockito.mockStatic(FileUtils.class)) {
            //Unix
            mockedStatic.when(FileUtils::getFileSeparator).thenReturn("/");
            String path = "org/example/Dep.java";
            String expected = "org/example/Dep.java";
            String actual = replaceDependencyPathString(path);
            assertEquals(expected, actual);

            path = "Foo/bar";
            expected = "Foo/bar";
            actual = replaceDependencyPathString(path);
            assertEquals(expected, actual);

            path = "sources/Cut/dependencies/org/example/Dep.java";
            expected = "sources/Cut/classes/org/example/Dep.java";
            actual = replaceDependencyPathString(path);
            assertEquals(expected, actual);

            path = "Foobar";
            expected = "Foobar";
            actual = replaceDependencyPathString(path);
            assertEquals(expected, actual);

            //Windows
            mockedStatic.when(FileUtils::getFileSeparator).thenReturn("\\");
            actual = replaceDependencyPathString(path);
            assertEquals(expected, actual);

            path = "org\\example\\Dep.java";
            expected = "org\\example\\Dep.java";
            actual = replaceDependencyPathString(path);
            assertEquals(expected, actual);


            path = "sources\\Cut\\dependencies\\org\\example\\Dep.java";
            expected = "sources\\Cut\\classes\\org\\example\\Dep.java";
            actual = replaceDependencyPathString(path);
            assertEquals(expected, actual);
        }
    }

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
