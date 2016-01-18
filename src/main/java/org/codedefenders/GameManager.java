package org.codedefenders;

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

import static org.codedefenders.Constants.FILE_SEPARATOR;
import static org.codedefenders.Constants.JAVA_SOURCE_EXT;
import static org.codedefenders.Constants.TESTS_DIR;
import static org.codedefenders.Constants.TEST_PREFIX;

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
		if (activeGame.getState().equals(Game.State.FINISHED)) {
			RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.SCORE_VIEW_JSP);
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
				RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.ATTACKER_VIEW_JSP);
				dispatcher.forward(request, response);
			} else {
				RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.RESOLVE_EQUIVALENCE_JSP);
				dispatcher.forward(request, response);
			}
		} else {
			// Direct to the Defender Page.
			RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.DEFENDER_VIEW_JSP);
			dispatcher.forward(request, response);
		}// else
			// response.sendRedirect(request.getHeader("referer"));
	}

	// Based on the data provided, update information for the game
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		logger.debug("Executing GameManager.doPost");
		System.out.println("Executing GameManager.doPost");

		ArrayList<String> messages = new ArrayList<String>();
		request.getSession().setAttribute("messages", messages);

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
					Test newTest = createTest(activeGame.getId(), activeGame.getClassId(), testText);

					TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

					if (compileTestTarget.status.equals("SUCCESS")) {
						TargetExecution testOriginalTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
						if (testOriginalTarget.status.equals("SUCCESS")) {
							System.out.println("Test compiled and executed correctly.");
							if (mutant.isAlive() && mutant.getEquivalent().equals(Mutant.Equivalence.PENDING_TEST)) {
								// Doesnt differentiate between failing because the test didnt run and failing because it detected the mutant
								MutationTester.runEquivalenceTest(getServletContext(), newTest, mutant);
								activeGame.passPriority();
								activeGame.update();
								Mutant mutantAfterTest = activeGame.getMutantByID(currentEquivMutantID);
								if (mutantAfterTest.isAlive()) {
									messages.add("Your test did not kill the mutant!");
								}
								response.sendRedirect("play");
								responseCommitted = true;
							} else {
								activeGame.passPriority();
								activeGame.update();
								messages.add("Yay, your test killed the muntant!");
								response.sendRedirect("play");
								responseCommitted = true;
							}
						} else {
							System.out.println("testOriginalTarget: " + testOriginalTarget);
							messages.add("An error occured while executing your test against the CUT.");
							messages.add(testOriginalTarget.message);
						}
					} else {
						System.out.println("compileTestTarget: " + compileTestTarget);
						messages.add("An error occured while compiling your test.");
						messages.add(compileTestTarget.message);
					}
				} else if (request.getParameter("acceptEquivalent") != null) { // If the user didnt want to supply a test
					logger.debug("Equivalence accepted");
					System.out.println("Equivalence accepted");
					if (mutant.isAlive() && mutant.getEquivalent().equals(Mutant.Equivalence.PENDING_TEST)) {
						mutant.kill();
						mutant.setEquivalent(Mutant.Equivalence.DECLARED_YES);
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
			case "claimEquivalent":
				if (request.getParameter("mutantId") != null) {
					int mutantId = Integer.parseInt(request.getParameter("mutantId"));
					Mutant m = DatabaseAccess.getMutant(activeGame, mutantId);
					m.setEquivalent(Mutant.Equivalence.PENDING_TEST);
					m.update();
					messages.add("Waiting For Attacker To Respond To Marked Equivalencies");
					activeGame.passPriority();
					activeGame.update();
				} else
					messages.add("Something went wrong claiming equivalent mutant");
				break;

			case "createMutant":

				// Get the text submitted by the user.
				String mutantText = request.getParameter("mutant");

				Mutant newMutant = createMutant(activeGame.getId(), activeGame.getClassId(), mutantText);
				if (newMutant != null) {
					TargetExecution compileMutantTarget = DatabaseAccess.getTargetExecutionForMutant(newMutant, TargetExecution.Target.COMPILE_MUTANT);
					if (compileMutantTarget.status.equals("SUCCESS")) {
						messages.add("Your mutant was compiled successfully.");
						MutationTester.runAllTestsOnMutant(getServletContext(), activeGame, newMutant, messages);
						activeGame.endTurn();
						activeGame.update();
					} else {
						messages.add("Your mutant failed to compile. Try again.");
						messages.add(compileMutantTarget.message);
					}
				} else {
					// Create Mutant failed because there were no differences between mutant and original, returning -1
					messages.add("Your mutant is not quite a mutant, it's identical to the class under test!");
				}
				break;

			case "createTest":

				// Get the text submitted by the user.
				String testText = request.getParameter("test");

				// If it can be written to file and compiled, end turn. Otherwise, dont.
				Test newTest = createTest(activeGame.getId(), activeGame.getClassId(), testText);
				System.out.println("New Test " + newTest.getId());
				TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

				if (compileTestTarget.status.equals("SUCCESS")) {
					TargetExecution testOriginalTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
					if (testOriginalTarget.status.equals("SUCCESS")) {
						messages.add("Your Test Was Compiled Successfully");
						MutationTester.runTestOnAllMutants(getServletContext(), activeGame, newTest, messages);
					} else {
						messages.add("Oh no! Your test failed on the original class under test. You lose your turn.");
						messages.add(testOriginalTarget.message);
					}
					activeGame.endTurn();
					activeGame.update();
				} else {
					messages.add("Your test failed to compile. Try again, but with compilable code.");
					messages.add(compileTestTarget.message);
				}
				break;
		}
		if (! responseCommitted)
			response.sendRedirect("play");//doGet(request, response);
	}

	// Writes text as a Mutant to the appropriate place in the file system.
	public Mutant createMutant(int gid, int cid, String mutantText) throws IOException {

		GameClass classMutated = DatabaseAccess.getClassForKey("Class_ID", cid);
		String classMutatedBaseName = classMutated.getBaseName();

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
		if (noChange)
			return null;

		// Setup folder the files will go in
		File newMutantDir = getNextSubDir(getServletContext().getRealPath(Constants.MUTANTS_DIR + FILE_SEPARATOR + gid));

		System.out.println("NewMutantDir: " + newMutantDir.getAbsolutePath());
		System.out.println("Class Mutated: " + classMutated.getName() + "(basename: " + classMutatedBaseName +")");

		// Write the Mutant String into a java file
		String mutantFileName = newMutantDir + FILE_SEPARATOR + classMutatedBaseName + JAVA_SOURCE_EXT;
		File mutantFile = new File(mutantFileName);
		FileWriter fw = new FileWriter(mutantFile);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(mutantText);
		bw.close();

		// Compile the mutant - if you can, add it to the Game State, otherwise, delete these files created.
		return MutationTester.compileMutant(getServletContext(), newMutantDir, mutantFileName, gid, classMutated);
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
			newPath = folder.getAbsolutePath() + FILE_SEPARATOR + "1";
		else {
			File lastDir = new File(directories[directories.length - 1]);
			int newIndex = Integer.parseInt(lastDir.getName()) + 1;
			newPath = path + FILE_SEPARATOR + newIndex;
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

	public Test createTest(int gid, int cid, String testText) throws IOException {

		GameClass classUnderTest = DatabaseAccess.getClassForKey("Class_ID", cid);

		File newTestDir = getNextSubDir(getServletContext().getRealPath(TESTS_DIR + FILE_SEPARATOR + gid));

		String javaFile = newTestDir + FILE_SEPARATOR + TEST_PREFIX + classUnderTest.getBaseName() + JAVA_SOURCE_EXT;
		File testFile = new File(javaFile);
		FileWriter testWriter = new FileWriter(testFile);
		BufferedWriter bufferedTestWriter = new BufferedWriter(testWriter);
		bufferedTestWriter.write(testText);
		bufferedTestWriter.close();

		// Check the test actually passes when applied to the original code.
		Test newTest = MutationTester.compileTest(getServletContext(), newTestDir, javaFile, gid, classUnderTest);
		TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

		if (compileTestTarget != null && compileTestTarget.status.equals("SUCCESS")) {
			MutationTester.testOriginal(getServletContext(), newTestDir, newTest);
		}
		return newTest;
	}
}
