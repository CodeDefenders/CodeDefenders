package org.gammut;

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
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import static org.gammut.Constants.ATTACKER_VIEW_JSP;
import static org.gammut.Constants.DEFENDER_VIEW_JSP;
import static org.gammut.Constants.JAVA_CLASS_EXT;
import static org.gammut.Constants.JAVA_SOURCE_EXT;
import static org.gammut.Constants.MUTANTS_DIR;
import static org.gammut.Constants.RESOLVE_EQUIVALENCE_JSP;
import static org.gammut.Constants.SCORE_VIEW_JSP;
import static org.gammut.Constants.SEPARATOR;
import static org.gammut.Constants.TESTS_DIR;
import static org.gammut.Constants.TEST_PREFIX;

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

		// If the game is finished, redirect to the score page.
		if (activeGame.getState().equals("FINISHED")) {
			session.setAttribute("game", activeGame);
			RequestDispatcher dispatcher = request.getRequestDispatcher(SCORE_VIEW_JSP);
			dispatcher.forward(request, response);
		} else if (activeGame.getAttackerId() == uid) {
			System.out.println("user is attacker");
			session.setAttribute("game", activeGame);

			boolean equivMutants = false;
			for (Mutant m : DatabaseAccess.getMutantsForGame(activeGame.getId())) {
				// If at least one mutant needs to be proved non-equivalent, go to the Resolve Equivalence page.
				System.out.println("about to check if a mutant is equiv");
				if (m.getEquivalent().equals("PENDING_TEST") && m.isAlive()) {
					equivMutants = true;
					break;
				}
			}
			if (equivMutants) {
				RequestDispatcher dispatcher = request.getRequestDispatcher(RESOLVE_EQUIVALENCE_JSP);
				dispatcher.forward(request, response);
			} else {
				System.out.println("Should be going to attacker page");
				// If no mutants needed to be proved non-equivalent, direct to the Attacker Page.
				RequestDispatcher dispatcher = request.getRequestDispatcher(ATTACKER_VIEW_JSP);
				dispatcher.forward(request, response);
			}
		} else if (activeGame.getDefenderId() == uid) {
			session.setAttribute("game", activeGame);
			// Direct to the Defender Page.
			RequestDispatcher dispatcher = request.getRequestDispatcher(DEFENDER_VIEW_JSP);
			dispatcher.forward(request, response);
		} else
			response.sendRedirect(request.getHeader("referer"));
	}

	// Based on the data provided, update information for the game
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		logger.debug("Executing GameManager.doPost");
		System.out.println("Executing GameManager.doPost");

		ArrayList<String> messages = new ArrayList<String>();
		request.setAttribute("messages", messages);

		Game activeGame = (Game) request.getSession().getAttribute("game");
		Map<String, String[]> map = request.getParameterMap();
		Set s = map.entrySet();
		Iterator it = s.iterator();
		while(it.hasNext()){

			Map.Entry<String,String[]> entry = (Map.Entry<String,String[]>)it.next();
			String key             = entry.getKey();
			String[] value         = entry.getValue();

			System.out.println("Key is "+key+"<br>");

			if(value.length>1){
				for (int i = 0; i < value.length; i++) {
					System.out.println("<li>" + value[i].toString() + "</li><br>");
				}
			}else
				System.out.println("Value is "+value[0].toString()+"<br>");
		}

		switch (request.getParameter("formType")) {

			case "resolveEquivalence":
				logger.debug("Executing Action resolveEquivalence");
				System.out.println("Game Manager Executing Action resolveEquivalence");
				// Check type of equivalence response.
				// If user wanted to supply a test
				if (request.getParameter("rejectEquivalent") != null) {
					logger.debug("Equivalence rejected, going to process killing test");
					System.out.println("Equivalence rejected, going to process killing test");
					Test test = null;
					Mutant mutant = null;
					// Get the text submitted by the user.
					String testText = request.getParameter("test");

					// If it can be written to file and compiled, end turn. Otherwise, dont.
					int[] testExecutions = createTest(activeGame.getId(), activeGame.getClassId(), testText);
					TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionsForKey("TargetExecution_ID", testExecutions[0]).get(0);

					if (compileTestTarget.status.equals("SUCCESS")) {
						TargetExecution testOriginalTarget = DatabaseAccess.getTargetExecutionsForKey("TargetExecution_ID", testExecutions[1]).get(0);
						if (testOriginalTarget.equals("SUCCESS")) {
							for (Mutant m : activeGame.getMutants()) {
								if (m.getEquivalent().equals("PENDING_TEST") && m.isAlive()) {
									mutant = m;
									break;
								}
							}

							// Doesnt differentiate between failing because the test didnt run and failing because it detected the mutant
							MutationTester.runEquivalenceTest(getServletContext(), test, mutant);
							activeGame.passPriority();
							activeGame.update();
						} else {
							messages.add("An error occured while executing your test against the CUT.");
						}
					} else {
						messages.add("An error occured while compiling your test.");
					}

				} else if (request.getParameter("acceptEquivalent") != null) { // If the user didnt want to supply a test
					logger.debug("Equivalence accepted");
					System.out.println("Equivalence accepted");
					for (Mutant m : DatabaseAccess.getMutantsForGame(activeGame.getId())) {
						if (m.getEquivalent().equals("PENDING_TEST") && m.isAlive()) {
							m.kill();
							m.setEquivalent("DECLARED_YES");
							m.update();

							messages.add("The mutant was marked Equivalent and killed");

							activeGame.passPriority();
							activeGame.update();
							response.sendRedirect("play");
							break;
						}
					}
				}
				break;

			case "markEquivalences":

				boolean changeMade = false;
				for (Mutant m : DatabaseAccess.getMutantsForGame(activeGame.getId())) {
					if (request.getParameter("mutant" + m.getId()) != null) {
						changeMade = true;
						m.setEquivalent("PENDING_TEST");
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
				int[] testExecutions = createTest(activeGame.getId(), activeGame.getClassId(), testText);
				System.out.println("Result of test execution:");
				System.out.println(Arrays.toString(testExecutions));
				TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionsForKey("TargetExecution_ID", testExecutions[0]).get(0);

				if (compileTestTarget.status.equals("SUCCESS")) {
					TargetExecution testOriginalTarget = DatabaseAccess.getTargetExecutionsForKey("TargetExecution_ID", testExecutions[1]).get(0);
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

		doGet(request, response);
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
		File folder = new File(getServletContext().getRealPath(MUTANTS_DIR + gid));
		folder.mkdir();

		// Write the Mutant String into a java file
		File mutant = new File(getServletContext().getRealPath(MUTANTS_DIR + gid + SEPARATOR + classMutated.name + JAVA_SOURCE_EXT));
		FileWriter fw = new FileWriter(mutant);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(mutantText);
		bw.close();

		// Try and compile the mutant - if you can, add it to the Game State, otherwise, delete these files created.
		String jFile = getServletContext().getRealPath(MUTANTS_DIR + gid + SEPARATOR + classMutated.name + JAVA_SOURCE_EXT);
		String cFile = getServletContext().getRealPath(MUTANTS_DIR + gid + SEPARATOR + classMutated.name + JAVA_CLASS_EXT);

		Mutant newMutant = new Mutant(gid, jFile, cFile);
		newMutant.insert();

		int compileMutantId = MutationTester.compileMutant(getServletContext(), newMutant);

		return compileMutantId;
	}

	public int[] createTest(int gid, int cid, String testText) throws IOException {

		GameClass classUnderTest = DatabaseAccess.getClassForKey("Class_ID", cid);

		File sourceFile = new File(classUnderTest.javaFile);
		String sourceCode = new String(Files.readAllBytes(sourceFile.toPath()));

		File folder = new File(getServletContext().getRealPath(TESTS_DIR + gid));
		folder.mkdir();
		String testSourceFileName = TESTS_DIR + gid + SEPARATOR + TEST_PREFIX + classUnderTest.name + JAVA_SOURCE_EXT;
		String testClassFileName = TESTS_DIR + gid + SEPARATOR + TEST_PREFIX + classUnderTest.name + JAVA_CLASS_EXT;
		File test = new File(getServletContext().getRealPath(testSourceFileName));
		FileWriter testWriter = new FileWriter(test);
		BufferedWriter bufferedTestWriter = new BufferedWriter(testWriter);
		bufferedTestWriter.write(testText);
		bufferedTestWriter.close();

		// Check the test actually passes when applied to the original code.

		String jFile = getServletContext().getRealPath(testSourceFileName);
		String cFile = getServletContext().getRealPath(testClassFileName);

		Test newTest = new Test(gid, jFile, cFile);
		newTest.insert();

		int compileTestId = MutationTester.compileTest(getServletContext(), newTest);
		TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionsForKey("TargetExecution_ID", compileTestId).get(0);

		if (compileTestTarget.status.equals("SUCCESS")) {
			int testOriginalId = MutationTester.testOriginal(getServletContext(), newTest);
			return new int[]{compileTestId, testOriginalId};
		} else {
			return new int[]{compileTestId, -1};
		}

	}
}
