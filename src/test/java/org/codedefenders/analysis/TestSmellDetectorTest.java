package org.codedefenders.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import testsmell.TestFile;
import testsmell.TestSmellDetector;

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
}
