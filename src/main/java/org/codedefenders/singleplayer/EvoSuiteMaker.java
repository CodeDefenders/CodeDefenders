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

	public EvoSuiteMaker(int classId) {
		cId = classId;
		cut = DatabaseAccess.getClassForKey("Class_ID", cId);
		cutTitle = cut.getBaseName();
	}

	public boolean makeSuite() {
		AntRunner.generateTestsFromCUT(cutTitle);
		//Need a dummy game to add test to.
		Game dummyGame = new Game(cId, 1, 3, Game.Role.ATTACKER, Game.Level.EASY);
		dummyGame.insert();
		dummyGame.setDefenderId(1);
		dummyGame.setState(Game.State.ACTIVE);
		dummyGame.update();

		ArrayList<String> testStrings = getTestStrings();
		ArrayList<Integer> testIds = new ArrayList<Integer>();

		try {
			for (String t : testStrings) {
				File newTestDir = FileManager.getNextSubDir(AI_DIR + F_SEP + "tests" +
						F_SEP + cutTitle + F_SEP);
				String jFile = FileManager.createJavaFile(newTestDir, cutTitle, t);
				Test newTest = AntRunner.compileTest(newTestDir, jFile, dummyGame.getId(), cut, 1);
				TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

				if (compileTestTarget != null && compileTestTarget.status.equals("SUCCESS")) {
					AntRunner.testOriginal(newTestDir, newTest);
					testIds.add(newTest.getId());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		makeInfoFile(testIds);

		return true; //Success
	}

	private boolean makeInfoFile(ArrayList<Integer> testIds) {
		File dir = new File(AI_DIR + F_SEP + "tests" + F_SEP + cutTitle);
		String contents = "";
		//Original test ids.
		contents += "<tests> \n";
		for (int n : testIds) {
			contents += "\t<test>" + n + "</test> \n";
		}
		contents += "</tests> \n";

		//Number of tests.
		contents += "<quantity>" + testIds.size() + "</quantity> \n";

		try {
			FileManager.createTestInfoFile(dir, cutTitle, contents);
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
				if(l.contains("import ")) {
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
		return tests;
	}
}
