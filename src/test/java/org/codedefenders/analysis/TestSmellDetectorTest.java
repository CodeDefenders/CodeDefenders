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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import testsmell.AbstractSmell;
import testsmell.TestFile;
import testsmell.TestSmellDetector;
import testsmell.smell.UnknownTest;

public class TestSmellDetectorTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();
	
	@Test
	public void exploratoryTest() throws IOException {
		File testFile = temporaryFolder.newFile("TestLift.java");
		// Detect test smell
		String testContent = ""
				+ "import org.junit.*;" + "\n"
				+ "import static org.junit.Assert.*;" + "\n"
				+ " " + "\n"
				+ "public class TestLift { " + "\n"
				+ "    @Test(timeout = 4000)" + "\n"
				+ "    public void test() throws Throwable {" + "\n"
				+ "        Lift l = new Lift(5);" + "\n"
				+ "        l.getTopFloor(); // This cover the mutant" + "\n"
				+ "        assertEquals(0, l.getCurrentFloor());" + "\n"
				+ "    }" + "\n"
				+ "}"
				+ "";
		
		Files.write(testFile.toPath(), testContent.getBytes());
		String testFilePath = testFile.getAbsolutePath();
		
		String productionFilePath = "src/test/resources/itests/sources/Lift/Lift.java";

		
		TestSmellDetector testSmellDetector = TestSmellDetector.createTestSmellDetector();
		TestFile testSmellFile = new TestFile("", testFilePath, productionFilePath);
		testSmellDetector.detectSmells(testSmellFile);

	}
	
	@Test
    public void testUnknownSmellDetector() throws IOException {
        File testFile = temporaryFolder.newFile("TestLift.java");
        // Detect UnknownTest test smell: there are no assertions
        String testContent = ""
                + "import org.junit.*;" + "\n"
                + "import static org.junit.Assert.*;" + "\n"
                + " " + "\n"
                + "public class TestLift { " + "\n"
                + "    @Test(timeout = 4000)" + "\n"
                + "    public void test() throws Throwable {" + "\n"
                + "        Lift l = new Lift(5);" + "\n"
                + "        l.getTopFloor(); // This cover the mutant" + "\n"
                + "    }" + "\n"
                + "}"
                + "";
        
        Files.write(testFile.toPath(), testContent.getBytes());
        String testFilePath = testFile.getAbsolutePath();
        
        String productionFilePath = "src/test/resources/itests/sources/Lift/Lift.java";

        
        TestSmellDetector testSmellDetector = TestSmellDetector.createTestSmellDetector();
        TestFile testSmellFile = new TestFile("", testFilePath, productionFilePath);
        testSmellDetector.detectSmells(testSmellFile);
        
        for( AbstractSmell smell : testSmellFile.getTestSmells()){
            if( smell instanceof UnknownTest ){
                Assert.assertTrue( smell.getHasSmell() );
            }
            
        }

    }
}
