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
package org.codedefenders.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import testsmell.AbstractSmell;
import testsmell.TestFile;
import testsmell.TestSmellDetector;
import testsmell.smell.UnknownTest;
import thresholds.DefaultThresholds;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSmellDetectorTest {

    @TempDir
    public Path tempFolder;

    @Test
    public void exploratoryTest() throws IOException {
        File testFile = tempFolder.resolve("TestLift.java").toFile();
        // Detect test smell
        String testContent = """
                import org.junit.*;
                import static org.junit.Assert.*;

                public class TestLift {
                    @Test(timeout = 4000)
                    public void test() throws Throwable {
                        Lift l = new Lift(5);
                        l.getTopFloor(); // This cover the mutant
                        assertEquals(0, l.getCurrentFloor());
                    }
                }""".stripIndent();

        Files.write(testFile.toPath(), testContent.getBytes());
        String testFilePath = testFile.getAbsolutePath();

        String productionFilePath = "src/test/resources/itests/sources/Lift/Lift.java";


        TestSmellDetector testSmellDetector = new TestSmellDetector(new DefaultThresholds());
        TestFile testSmellFile = new TestFile("", testFilePath, productionFilePath);
        testSmellDetector.detectSmells(testSmellFile);

    }

    @Test
    public void testUnknownSmellDetector() throws IOException {
        File testFile = tempFolder.resolve("TestLift.java").toFile();
        // Detect UnknownTest test smell: there are no assertions
        String testContent = """
                import org.junit.*;
                import static org.junit.Assert.*;

                public class TestLift {
                    @Test(timeout = 4000)
                    public void test() throws Throwable {
                        Lift l = new Lift(5);
                        l.getTopFloor(); // This cover the mutant
                    }
                }""";

        Files.write(testFile.toPath(), testContent.getBytes());
        String testFilePath = testFile.getAbsolutePath();

        String productionFilePath = "src/test/resources/itests/sources/Lift/Lift.java";


        TestSmellDetector testSmellDetector = new TestSmellDetector(new DefaultThresholds());
        TestFile testSmellFile = new TestFile("", testFilePath, productionFilePath);
        testSmellDetector.detectSmells(testSmellFile);

        for (AbstractSmell smell : testSmellFile.getTestSmells()) {
            if (smell instanceof UnknownTest) {
                assertTrue(smell.hasSmell());
            }

        }

    }
}
