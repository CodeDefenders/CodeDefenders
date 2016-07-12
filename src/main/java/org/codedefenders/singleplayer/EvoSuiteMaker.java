package org.codedefenders.singleplayer;

import org.codedefenders.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.codedefenders.Constants.*;

public class EvoSuiteMaker {

	private String cutTitle;
	private int cId;
	private GameClass cut;
	private Game dGame;
	private ArrayList<Test> validTests;

	public EvoSuiteMaker(int classId, Game dummyGame) {
		cId = classId;
		cut = DatabaseAccess.getClassForKey("Class_ID", cId);
		cutTitle = cut.getBaseName();
		dGame = dummyGame;
	}

	public boolean makeSuite() {
		AntRunner.generateTestsFromCUT(cutTitle);
		AntRunner.compileGenTestSuite(cutTitle);
		//Need a dummy game to add test to.

		ArrayList<String> testStrings = getTestStrings();
		validTests = new ArrayList<Test>();

		try {
			for (String t : testStrings) {
				File newTestDir = FileManager.getNextSubDir(AI_DIR + F_SEP + "tests" +
						F_SEP + cutTitle + F_SEP);
				String jFile = FileManager.createJavaFile(newTestDir, cutTitle, t);
				Test newTest = AntRunner.compileTest(newTestDir, jFile, dGame.getId(), cut, 1);
				TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

				if (compileTestTarget != null && compileTestTarget.status.equals("SUCCESS")) {
					AntRunner.testOriginal(newTestDir, newTest);
					validTests.add(newTest);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true; //Success
	}

	public boolean createTestIndex() {
		File dir = new File(AI_DIR + F_SEP + "tests" + F_SEP + cutTitle);

		int goodTests = 0;

		String xml = "";
		xml += "<?xml version=\"1.0\"?> \n";
		xml += "<testindex> \n";
		//Original test ids.
		xml += "\t<tests> \n";
		for (Test t : validTests) {
			int mKilled = t.getAiMutantsKilled();
			//If the test kills at least one mutant, use it in the future.
			if(mKilled > 0) {
				xml += "\t\t<test ";
				xml += "id=\"" + t.getId() + "\" ";
				xml += "kills=\"" + mKilled + "\" ";
				xml += "/>\n";
				goodTests ++;
			}
		}
		xml += "\t</tests> \n";

		//Number of tests.
		xml += "\t<quantity>" + goodTests + "</quantity> \n";
		//ID of dummy game.
		xml += "\t<dummygame>" + dGame.getId() + "</dummygame> \n";

		xml += "</testindex> \n";

		try {
			FileManager.createIndexXML(dir, "TestsIndex", xml);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Retrieve the lines of the full test suite.
	 * @return test suite
	 */
	private List<String> getTestSuiteLines() {
		String loc = AI_DIR + F_SEP + "tests" + F_SEP + cutTitle +
				F_SEP + cutTitle + "EvoSuiteTest.java";
		File f = new File(loc);
		return FileManager.readLines(f.toPath());
	}

	public ArrayList<String> getTestStrings() {
		List<String> suite = getTestSuiteLines();
		String sharedStart = ""; //Store the beginning of the suite (imports and class).

		int brOpen = 0; //Number of brace opens.
		int brClose = 0; //Number of brace closes.
		boolean inTest = false; //Track if test currently being parsed.

		String t = ""; //Single test buffer.
		ArrayList<String> tests = new ArrayList<String>();

		for (String l : suite) {

			if(!inTest) {
				//Not in test.
				if(l.startsWith("package ")) {
					//Add any line with package.
					sharedStart += l + "\n";
				}
				else if(l.startsWith("import ")) {
					//Add any line with import.
					sharedStart += l + "\n";
				}
				else if(l.contains("public class ")) {
					//Class declaration, write correct one in place.
					sharedStart += "public class Test" + cutTitle + " { \n";
				}
				else if(l.contains("public void test")) {
					//Start of a test.
					inTest = true;
					brOpen ++;
					//t += l + "\n";
					t += "@Test(timeout = 4000) \n"; //Test identifier.
					t += "public void test() throws Throwable { \n";
				}
			}
			else {
				//Write every line and track braces.
				t += l + "\n";
				if(l.endsWith(";"))
				{
					//Normal line, no braces.
				}
				else
				{
					//Can have braces.
					if(l.contains("{")) {
						brOpen ++;
					}
					if(l.contains("}")) {
						brClose ++;
						if(brOpen == brClose) {
							//Every opened bracket has been closed.
							//Finish off the file.
							//Add class start, test buffer and
							//closing brace for class declaration.
							tests.add(sharedStart + t + "} \n");
							//Reset test buffer and brace counters.
							t = "";
							brOpen = 0;
							brClose = 0;
							inTest = false;
						}
					}
				}
			}
		}
		return tests;
	}
}
