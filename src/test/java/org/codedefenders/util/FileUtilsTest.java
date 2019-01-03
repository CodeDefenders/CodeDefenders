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
        File nextSubDir = FileUtils.getNextSubDir(dummyDirectory.toString());

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
}