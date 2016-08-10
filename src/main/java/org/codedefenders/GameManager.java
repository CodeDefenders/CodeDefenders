package org.codedefenders;

import org.codedefenders.singleplayer.SinglePlayerGame;
import org.codedefenders.validation.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;

import static org.codedefenders.Constants.*;
import static org.codedefenders.Mutant.Equivalence.ASSUMED_YES;
import static org.codedefenders.Mutant.Equivalence.PROVEN_NO;
import static org.codedefenders.validation.CodeValidator.getMD5;

public class GameManager extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(GameManager.class);

	// Based on info provided, navigate to the correct view for the user
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// Get the session information specific to the current user.
		HttpSession session = request.getSession();
		int uid = (Integer) session.getAttribute("uid");
		Object ogid = session.getAttribute("gid");
		if (ogid == null) {
			response.sendRedirect("games/user");
			return;
		}

		int gid = (Integer) ogid;

		logger.debug("Getting game " + gid + " for " + uid);

		Game activeGame = DatabaseAccess.getGameForKey("ID", gid);
		session.setAttribute("game", activeGame);

		// If the game is finished, redirect to the score page.
		if (activeGame.getAttackerId() == uid) {
			ArrayList<Mutant> equivMutants = activeGame.getMutantsMarkedEquivalent();
			if (equivMutants.isEmpty()) {
				logger.info("Redirecting to attacker page");
				ArrayList<Mutant> aliveMutants = activeGame.getAliveMutants();
				if (aliveMutants.isEmpty()) {
					logger.info("No Mutants Alive, only attacker can play.");
					activeGame.setActiveRole(Role.ATTACKER);
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

		ArrayList<String> messages = new ArrayList<String>();
		HttpSession session = request.getSession();
		int uid = (Integer) session.getAttribute("uid");

		session.setAttribute("messages", messages);

		Game activeGame = (Game) session.getAttribute("game");

		switch (request.getParameter("formType")) {

			case "resolveEquivalence":
				logger.info("Executing Action resolveEquivalence");
				int currentEquivMutantID = Integer.parseInt(request.getParameter("currentEquivMutant"));
				Mutant mutant = activeGame.getMutantByID(currentEquivMutantID);

				// Check type of equivalence response.
				if (request.getParameter("rejectEquivalent") != null) { // If user wanted to supply a test
					logger.info("Equivalence rejected for mutant {}, processing killing test", currentEquivMutantID);

					// Get the text submitted by the user.
					String testText = request.getParameter("test");

					// If it can be written to file and compiled, end turn. Otherwise, dont.
					Test newTest = createTest(activeGame.getId(), activeGame.getClassId(), testText, uid, "sp");
					if (newTest == null) {
						messages.add(TEST_INVALID_MESSAGE);
						session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
						response.sendRedirect("play");
						return;
					}

					TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

					if (compileTestTarget.status.equals("SUCCESS")) {
						TargetExecution testOriginalTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
						if (testOriginalTarget.status.equals("SUCCESS")) {
							logger.info(TEST_PASSED_ON_CUT_MESSAGE);
							if (mutant.isAlive() && mutant.getEquivalent().equals(Mutant.Equivalence.PENDING_TEST)) {
								// TODO: Allow multiple trials?
								// TODO: Doesnt differentiate between failing because the test didnt run and failing because it detected the mutant
								MutationTester.runEquivalenceTest(newTest, mutant);
								activeGame.endRound();
								activeGame.update();
								Mutant mutantAfterTest = activeGame.getMutantByID(currentEquivMutantID);
								if (mutantAfterTest.getEquivalent().equals(PROVEN_NO)) {
									logger.info("Test {} killed mutant {}, hence NOT equivalent", newTest.getId(), mutant.getId());
									messages.add(TEST_KILLED_CLAIMED_MUTANT_MESSAGE);
								} else {
									// test did not kill the mutant, lost duel, kill mutant
									mutantAfterTest.kill(ASSUMED_YES);

									logger.info("Test {} failed to kill mutant {}", newTest.getId(), mutant.getId());
									messages.add(TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE);
								}
								response.sendRedirect("play");
								return;
							} else {
								activeGame.endRound();
								activeGame.update();
								messages.add(TEST_KILLED_CLAIMED_MUTANT_MESSAGE);
								response.sendRedirect("play");
								return;
							}
						} else {
							//  (testOriginalTarget.state.equals("FAIL") || testOriginalTarget.state.equals("ERROR")
							logger.debug("testOriginalTarget: " + testOriginalTarget);
							messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
							messages.add(testOriginalTarget.message);
							session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
						}
					} else {
						logger.debug("compileTestTarget: " + compileTestTarget);
						messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
						messages.add(compileTestTarget.message);
						session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
					}
				} else if (request.getParameter("acceptEquivalent") != null) { // If the user didnt want to supply a test
					logger.info("Equivalence accepted for mutant {}", mutant.getId());
					if (mutant.isAlive() && mutant.getEquivalent().equals(Mutant.Equivalence.PENDING_TEST)) {
						mutant.kill(Mutant.Equivalence.DECLARED_YES);

						messages.add(MUTANT_ACCEPTED_EQUIVALENT_MESSAGE);
						activeGame.endRound();
						activeGame.update();
						response.sendRedirect("play");
						return;
					}
				}
				break;
			case "claimEquivalent":
				if (request.getParameter("mutantId") != null) {
					int mutantId = Integer.parseInt(request.getParameter("mutantId"));
					Mutant mutantClaimed = DatabaseAccess.getMutant(activeGame, mutantId);
					if(activeGame.getMode().equals(Game.Mode.SINGLE)) {
						//Singleplayer - use automatic system.
						if(AntRunner.potentialEquivalent(mutantClaimed)) {
							//Is potentially equiv - accept as equivalent
							mutantClaimed.kill(Mutant.Equivalence.DECLARED_YES);
						} else {
							mutantClaimed.setEquivalent(PROVEN_NO);
							mutantClaimed.update();
						}
						activeGame.endTurn();
					} else {
						mutantClaimed.setEquivalent(Mutant.Equivalence.PENDING_TEST);
						mutantClaimed.update();
						messages.add(MUTANT_CLAIMED_EQUIVALENT_MESSAGE);
						activeGame.passPriority();
					}
					activeGame.update();
				} else
					messages.add(MUTANT_CLAIMED_EQUIVALENT_ERROR_MESSAGE);
				break;

			case "whoseTurn":
				int gid = Integer.parseInt(request.getParameter("gameID"));
				activeGame = DatabaseAccess.getGameForKey("ID", gid);
				String turn = activeGame.getActiveRole().equals(Role.ATTACKER) ? "attacker" : "defender";
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
				response.getWriter().write(turn);
				return;

			case "createMutant":

				// Get the text submitted by the user.
				String mutantText = request.getParameter("mutant");

				Mutant newMutant = createMutant(activeGame.getId(), activeGame.getClassId(), mutantText, uid, "sp");
				if (newMutant != null) {
					TargetExecution compileMutantTarget = DatabaseAccess.getTargetExecutionForMutant(newMutant, TargetExecution.Target.COMPILE_MUTANT);
					if (compileMutantTarget != null && compileMutantTarget.status.equals("SUCCESS")) {
						messages.add(MUTANT_COMPILED_MESSAGE);
						MutationTester.runAllTestsOnMutant(activeGame, newMutant, messages);

						if(activeGame.getMode().equals(Game.Mode.SINGLE)) {
							//Singleplayer - check for potential equivalent.
							if(AntRunner.potentialEquivalent(newMutant)) {
								//Is potentially equiv - mark as equivalent and update.
								newMutant.setEquivalent(Mutant.Equivalence.PENDING_TEST);
								newMutant.update();
								activeGame.update();
							} else {
								activeGame.endTurn();
								SinglePlayerGame g = (SinglePlayerGame) activeGame;
								if (g.getAi().makeTurn()) {
									messages.add("The AI has created a test!");
								}
							}
						} else {
							activeGame.endTurn();
						}
						activeGame.update();
					} else {
						messages.add(MUTANT_UNCOMPILABLE_MESSAGE);
						if (compileMutantTarget != null && compileMutantTarget.message != null && ! compileMutantTarget.message.isEmpty())
							messages.add(compileMutantTarget.message);
						session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, mutantText);
					}
				} else {
					// Create Mutant failed because there were no differences between mutant and original, returning -1
					messages.add(MUTANT_INVALID_MESSAGE);
				}
				break;

			case "createTest":

				// Get the text submitted by the user.
				String testText = request.getParameter("test");

				// If it can be written to file and compiled, end turn. Otherwise, dont.
				Test newTest = createTest(activeGame.getId(), activeGame.getClassId(), testText, uid, "sp");
				if (newTest == null) {
					messages.add(TEST_INVALID_MESSAGE);
					session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
					response.sendRedirect("play");
					return;
				}
				logger.debug("New Test " + newTest.getId());
				TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

				if (compileTestTarget.status.equals("SUCCESS")) {
					TargetExecution testOriginalTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
					if (testOriginalTarget.status.equals("SUCCESS")) {
						messages.add(TEST_PASSED_ON_CUT_MESSAGE);
						MutationTester.runTestOnAllMutants(activeGame, newTest, messages);
						activeGame.endTurn();
						activeGame.update();
						if(activeGame.getMode().equals(AbstractGame.Mode.SINGLE)) {
							SinglePlayerGame g = (SinglePlayerGame) activeGame;
							if (g.getAi().makeTurn()) {
								messages.add("The AI has created a mutant!");
							}
						}
					} else {
						// testOriginalTarget.state.equals("FAIL") || testOriginalTarget.state.equals("ERROR")
						messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
						messages.add(testOriginalTarget.message);
						session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
					}
				} else {
					messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
					messages.add(compileTestTarget.message);
					session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
				}
				break;
		}

		response.sendRedirect("play");//doGet(request, response);
	}

	public static Mutant createMutant(int gid, int cid, String mutatedCode, int ownerId, String subDirectory) throws IOException {

		GameClass classMutated = DatabaseAccess.getClassForKey("Class_ID", cid);
		String classMutatedBaseName = classMutated.getBaseName();

		File sourceFile = new File(classMutated.getJavaFile());
		String sourceCode = new String(Files.readAllBytes(sourceFile.toPath()));

		// If there were no differences, return, as the mutant is the same as original.
		if (! CodeValidator.validMutant(sourceCode, mutatedCode))
			return null;

		// If another mutant with same md5 exists
		String md5CUT = CodeValidator.getMD5(sourceCode);
		String md5Mutant = CodeValidator.getMD5(mutatedCode);
		if (md5CUT.equals(md5Mutant))
			return null;

		// The insertion of a mutant will check (game_id,md5) unique later after compilation,
		// however I am assuming querying the DB now (before compiling) is cheaper
		Mutant mutantWithSameMD5 = DatabaseAccess.getMutant(gid, md5Mutant);
		if (mutantWithSameMD5 != null)
			return null; // a mutant with same MD5 already exists in the game

		// Setup folder the files will go in
		File newMutantDir = FileManager.getNextSubDir(Constants.MUTANTS_DIR + F_SEP + subDirectory + F_SEP + gid + F_SEP + ownerId);

		logger.info("NewMutantDir: {}", newMutantDir.getAbsolutePath());
		logger.info("Class Mutated: {} (basename: {})", classMutated.getName(), classMutatedBaseName);

		// Write the Mutant String into a java file
		String mutantFileName = newMutantDir + F_SEP + classMutatedBaseName + JAVA_SOURCE_EXT;
		File mutantFile = new File(mutantFileName);
		FileWriter fw = new FileWriter(mutantFile);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(mutatedCode);
		bw.close();

		String md5FromMutantFile = CodeValidator.getMD5FromFile(mutantFileName);
		logger.info("md5CUT: {}\nmd5Mutant:{}\nmd5FromMutantFile: {}", md5CUT, md5Mutant, md5FromMutantFile);
		assert (md5Mutant.equals(md5FromMutantFile)); // sanity check

		// Compile the mutant - if you can, add it to the MultiplayerGame State, otherwise, delete these files created.
		return AntRunner.compileMutant(newMutantDir, mutantFileName, gid, classMutated, ownerId);
	}

	/**
	 *
	 * @param gid
	 * @param cid
	 * @param testText
	 * @param ownerId
	 * @param subDirectory - Directory inside data for test to go
	 * @return {@code null} if test is not valid
	 * @throws IOException
	 */
	public static Test createTest(int gid, int cid, String testText, int ownerId, String subDirectory) throws IOException {

		GameClass classUnderTest = DatabaseAccess.getClassForKey("Class_ID", cid);

		File newTestDir = FileManager.getNextSubDir(TESTS_DIR + F_SEP + subDirectory + F_SEP + gid + F_SEP + ownerId);

		String javaFile = FileManager.createJavaFile(newTestDir, classUnderTest.getBaseName(), testText);

		if (!CodeValidator.validTestCode(javaFile)) {
			return null;
		}

		// Check the test actually passes when applied to the original code.
		Test newTest = AntRunner.compileTest(newTestDir, javaFile, gid, classUnderTest, ownerId);
		TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

		if (compileTestTarget != null && compileTestTarget.status.equals("SUCCESS")) {
			AntRunner.testOriginal(newTestDir, newTest);
		}
		return newTest;
	}

}