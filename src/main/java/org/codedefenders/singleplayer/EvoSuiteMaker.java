package org.codedefenders.singleplayer;

import org.codedefenders.FileManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.codedefenders.Constants.*;

public class EvoSuiteMaker {

	private String cutTitle;

	public EvoSuiteMaker(String cutName) {
		cutTitle = cutName;
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
						System.out.println(t);
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
