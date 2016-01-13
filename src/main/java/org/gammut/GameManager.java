package org.gammut;

import static org.gammut.Constants.ATTACKER_VIEW_JSP;
import static org.gammut.Constants.DEFENDER_VIEW_JSP;
import static org.gammut.Constants.Equivalence.DECLARED_YES;
import static org.gammut.Constants.Equivalence.PENDING_TEST;
import static org.gammut.Constants.JAVA_CLASS_EXT;
import static org.gammut.Constants.JAVA_SOURCE_EXT;
import static org.gammut.Constants.MUTANTS_DIR;
import static org.gammut.Constants.RESOLVE_EQUIVALENCE_JSP;
import static org.gammut.Constants.SCORE_VIEW_JSP;
import static org.gammut.Constants.SEPARATOR;
import static org.gammut.Constants.TESTS_DIR;
import static org.gammut.Constants.TEST_PREFIX;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class GameManager extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(GameManager.class);

	// Based on info provided, navigate to the correct view for the user
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// Get the session information specific to the current user.
		HttpSession session = request.getSession();
		int uid = (Integer) session.getAttribute("uid");
		int gid = (Integer) session.getAttribute("gid");

		System.out.println("Getting game " + gid + " for " + uid);

		Game activeGame = DatabaseAccess.getGameForKey("Game_ID", gid);
		session.setAttribute("game", activeGame);

		// If the game is finished, redirect to the score page.
		if (activeGame.getState().equals("FINISHED")) {
			RequestDispatcher dispatcher = request.getRequestDispatcher(SCORE_VIEW_JSP);
			dispatcher.forward(request, response);
		} else if (activeGame.getAttackerId() == uid) {
			System.out.println("user is attacker");
			ArrayList<Mutant> equivMutants = activeGame.getMutantsMarkedEquivalent();
			if (equivMutants.isEmpty()) {
				System.out.println("Redirecting to attacker page");
				ArrayList<Mutant> aliveMutants = activeGame.getAliveMutants();
				if (aliveMutants.isEmpty()) {
					System.out.println("No Mutants Alive, only attacker can play.");
					activeGame.setActivePlayer("ATTACKER");
					activeGame.update();
				}
				// If no mutants needed to be proved non-equivalent, direct to the Attacker Page.
				RequestDispatcher dispatcher = request.getRequestDispatcher(ATTACKER_VIEW_JSP);
				dispatcher.forward(request, response);
			} else {
				RequestDispatcher dispatcher = request.getRequestDispatcher(RESOLVE_EQUIVALENCE_JSP);
				dispatcher.forward(request, response);
			}
		} else {
			// Direct to the Defender Page.
			RequestDispatcher dispatcher = request.getRequestDispatcher(DEFENDER_VIEW_JSP);
			dispatcher.forward(request, response);
		}// else
			// response.sendRedirect(request.getHeader("referer"));
	}

	// Based on the data provided, update information for the game
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		logger.debug("Executing GameManager.doPost");
		System.out.println("Executing GameManager.doPost");

		ArrayList<String> messages = new ArrayList<String>();
		request.setAttribute("messages", messages);

		Game activeGame = (Game) request.getSession().getAttribute("game");

		boolean responseCommitted = false;
		switch (request.getParameter("formType")) {

			case "resolveEquivalence":
				logger.debug("Executing Action resolveEquivalence");
				System.out.println("Game Manager Executing Action resolveEquivalence");
				int currentEquivMutantID = Integer.parseInt(request.getParameter("currentEquivMutant"));
				Mutant mutant = activeGame.getMutantByID(currentEquivMutantID);
				System.out.println("CurrentEquivMutant ID = " + currentEquivMutantID);

				// Check type of equivalence response.
				if (request.getParameter("rejectEquivalent") != null) { // If user wanted to supply a test
					logger.debug("Equivalence rejected, going to process killing test form mutant " + currentEquivMutantID);
					System.out.println("Equivalence rejected, going to process killing test for mutant " + currentEquivMutantID);

					// Get the text submitted by the user.
					String testText = request.getParameter("test");

					// If it can be written to file and compiled, end turn. Otherwise, dont.
					Object[] testExecutions = createTest(activeGame.getId(), activeGame.getClassId(), testText);
					TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionsForKey("TargetExecution_ID", (Integer)testExecutions[0]).get(0);

					if (compileTestTarget.status.equals("SUCCESS")) {
						TargetExecution testOriginalTarget = DatabaseAccess.getTargetExecutionsForKey("TargetExecution_ID", (Integer)testExecutions[1]).get(0);
						if (testOriginalTarget.status.equals("SUCCESS")) {
							System.out.println("Test compiled and executed correctly.");
							if (mutant.isAlive() && mutant.getEquivalent().equals(PENDING_TEST.name())) {
								// Doesnt differentiate between failing because the test didnt run and failing because it detected the mutant
								MutationTester.runEquivalenceTest(getServletContext(), (Test) testExecutions[2], mutant);
								activeGame.passPriority();
								activeGame.update();
								Mutant mutantAfterTest = activeGame.getMutantByID(currentEquivMutantID);
								if (mutantAfterTest.isAlive()) {
									messages.add("Your test failed to kill the mutant!");
								}
								response.sendRedirect("play");
								responseCommitted = true;
							} else {
								System.out.println("Not running EquivalenceTest, mutant already dead?");
								activeGame.passPriority();
								activeGame.update();
								messages.add("Yay, your test killed the muntant!");
								response.sendRedirect("play");
								responseCommitted = true;
							}
						} else {
							System.out.println("testOriginalTarget: " + testOriginalTarget);
							messages.add("An error occured while executing your test against the CUT.");
						}
					} else {
						System.out.println("compileTestTarget: " + compileTestTarget);
						messages.add("An error occured while compiling your test.");
					}
				} else if (request.getParameter("acceptEquivalent") != null) { // If the user didnt want to supply a test
					logger.debug("Equivalence accepted");
					System.out.println("Equivalence accepted");
					if (mutant.isAlive() && mutant.getEquivalent().equals(PENDING_TEST.name())) {
						mutant.kill();
						mutant.setEquivalent(DECLARED_YES.name());
						mutant.update();

						messages.add("The mutant was marked Equivalent and killed");

						if (! activeGame.getAliveMutants().isEmpty())
							activeGame.passPriority();

						activeGame.update();
						response.sendRedirect("play");
						responseCommitted = true;
					}
				}
				break;
			case "markEquivalences":

				boolean changeMade = false;
				for (Mutant m : DatabaseAccess.getMutantsForGame(activeGame.getId())) {
					if (request.getParameter("mutant" + m.getId()) != null) {
						changeMade = true;
						m.setEquivalent(PENDING_TEST.name());
						m.update();
					}
				}
				if (changeMade) {
					messages.add("Waiting For Attacker To Respond To Marked Equivalencies");
					activeGame.passPriority();
					activeGame.update();
				} else {
					messages.add("You Didn't Mark Any Equivalencies");
				}
				break;

			case "createMutant":

				// Get the text submitted by the user.
				String mutantText = request.getParameter("mutant");

				int compileExecution = createMutant(activeGame.getId(), activeGame.getClassId(), mutantText);
				if (compileExecution != -1) {
					TargetExecution compileMutantTarget = DatabaseAccess.getTargetExecutionsForKey("TargetExecution_ID", compileExecution).get(0);
					if (compileMutantTarget.status.equals("SUCCESS")) {
						messages.add("Your Mutant Was Compiled Successfully");
						activeGame.endTurn();
						activeGame.update();
					} else {
						messages.add("Your Mutant Failed To Compile");
						messages.add(compileMutantTarget.message);
					}
				} else {
					// Create Mutant failed because there were no differences between mutant and original, returning -1
					messages.add("Your Mutant Was The Same As The Original");
				}
				break;

			case "createTest":

				// Get the text submitted by the user.
				String testText = request.getParameter("test");

				// If it can be written to file and compiled, end turn. Otherwise, dont.
				Object[] testExecutions = createTest(activeGame.getId(), activeGame.getClassId(), testText);
				System.out.println("Result of test execution:");
				System.out.println(Arrays.toString(testExecutions));
				TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionsForKey("TargetExecution_ID", (int)testExecutions[0]).get(0);

				if (compileTestTarget.status.equals("SUCCESS")) {
					TargetExecution testOriginalTarget = DatabaseAccess.getTargetExecutionsForKey("TargetExecution_ID", (int)testExecutions[1]).get(0);
					if (testOriginalTarget.status.equals("SUCCESS")) {
						messages.add("Your Test Was Compiled Successfully");
						MutationTester.runMutationTests(getServletContext(), activeGame.getId());
						activeGame.endTurn();
						activeGame.update();
					} else {
						messages.add("Your Tests Failed For The Original Code");
					}
				} else {
					messages.add("Your Test Failed To Compile");
					messages.add(compileTestTarget.message);
					// Need to display error messages to user
				}
				break;
		}
		if (! responseCommitted)
			response.sendRedirect("play");//doGet(request, response);
	}

	// Writes text as a Mutant to the appropriate place in the file system.
	public int createMutant(int gid, int cid, String mutantText) throws IOException {

		GameClass classMutated = DatabaseAccess.getClassForKey("Class_ID", cid);

		File sourceFile = new File(classMutated.javaFile);
		String sourceCode = new String(Files.readAllBytes(sourceFile.toPath()));

		// Runs diff match patch between the two Strings to see if there are any differences.
		DiffMatchPatch dmp = new DiffMatchPatch();
		LinkedList<DiffMatchPatch.Diff> changes = dmp.diffMain(sourceCode.trim().replace("\n", "").replace("\r", ""), mutantText.trim().replace("\n", "").replace("\r", ""), true);
		boolean noChange = true;
		for (DiffMatchPatch.Diff d : changes) {
			if (d.operation != DiffMatchPatch.Operation.EQUAL) {
				noChange = false;
			}
		}

		// If there were no differences, return, as the mutant is the same as original.
		if (noChange) {
			return -1;
		}

		// Setup folder the files will go in
		File newMutantDir = getNextSubDir(getServletContext().getRealPath(MUTANTS_DIR + SEPARATOR + gid));

		System.out.println("NewMutantDir: " + newMutantDir.getAbsolutePath());
		System.out.println("classMutated.name: " + classMutated.name);
		// Write the Mutant String into a java file
		File mutant = new File(newMutantDir + SEPARATOR + classMutated.name + JAVA_SOURCE_EXT);
		FileWriter fw = new FileWriter(mutant);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(mutantText);
		bw.close();

		// Try and compile the mutant - if you can, add it to the Game State, otherwise, delete these files created.
		String jFile = newMutantDir + SEPARATOR + classMutated.name + JAVA_SOURCE_EXT;
		String cFile = newMutantDir + SEPARATOR + classMutated.name + JAVA_CLASS_EXT;

		Mutant newMutant = new Mutant(gid, jFile, cFile);
		newMutant.insert();

		int compileMutantId = MutationTester.compileMutant(getServletContext(), newMutant);

		return compileMutantId;
	}

	public File getNextSubDir(String path) {
		File folder = new File(path);
		folder.mkdirs();
		String[] directories = folder.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory() && (isParsable(name));
			}
		});
		Arrays.sort(directories);
		String newPath;
		if (directories.length == 0)
			newPath = folder.getAbsolutePath() + SEPARATOR + "1";
		else {
			File lastDir = new File(directories[directories.length - 1]);
			int newIndex = Integer.parseInt(lastDir.getName()) + 1;
			newPath = path + SEPARATOR + newIndex;
		}
		File newDir = new File(newPath);
		newDir.mkdirs();
		return newDir;
	}

	public static boolean isParsable(String input){
		boolean parsable = true;
		try{
			Integer.parseInt(input);
		}catch(NumberFormatException e){
			parsable = false;
		}
		return parsable;
	}

	public Object[] createTest(int gid, int cid, String testText) throws IOException {
		// TODO: this needs to change, returning array of objects is awful

		GameClass classUnderTest = DatabaseAccess.getClassForKey("Class_ID", cid);

		File sourceFile = new File(classUnderTest.javaFile);
		String sourceCode = new String(Files.readAllBytes(sourceFile.toPath()));

		File newTestDir = getNextSubDir(getServletContext().getRealPath(TESTS_DIR + SEPARATOR + gid));

		String javaFile = newTestDir + SEPARATOR + TEST_PREFIX + classUnderTest.name + JAVA_SOURCE_EXT;
		String classFile = newTestDir + SEPARATOR + TEST_PREFIX + classUnderTest.name + JAVA_CLASS_EXT;

		File test = new File(javaFile);
		FileWriter testWriter = new FileWriter(test);
		BufferedWriter bufferedTestWriter = new BufferedWriter(testWriter);
		bufferedTestWriter.write(testText);
		bufferedTestWriter.close();

		// Check the test actually passes when applied to the original code.
		Test newTest = new Test(gid, javaFile, classFile);
		newTest.insert();

		int compileTestId = MutationTester.compileTest(getServletContext(), newTest);
		TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionsForKey("TargetExecution_ID", compileTestId).get(0);

		if (compileTestTarget.status.equals("SUCCESS")) {
			int testOriginalId = MutationTester.testOriginal(getServletContext(), newTest);
			return new Object[]{compileTestId, testOriginalId, newTest};
		} else {
			return new Object[]{compileTestId, -1, newTest};
		}

	}
}
