/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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
package org.codedefenders.servlets.games;

import org.apache.commons.lang.StringEscapeUtils;
import org.codedefenders.database.DuelGameDAO;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.execution.AntRunner;
import org.codedefenders.execution.MutationTester;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.game.singleplayer.SinglePlayerGame;
import org.codedefenders.util.Constants;
import org.codedefenders.util.FileUtils;
import org.codedefenders.util.MutantUtils;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorException;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.codedefenders.validation.code.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import testsmell.TestFile;
import testsmell.TestSmellDetector;

import static org.codedefenders.util.Constants.F_SEP;
import static org.codedefenders.util.Constants.JAVA_SOURCE_EXT;
import static org.codedefenders.util.Constants.MODE_DUEL_DIR;
import static org.codedefenders.util.Constants.MUTANT_ACCEPTED_EQUIVALENT_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_CLAIMED_EQUIVALENT_ERROR_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_CLAIMED_EQUIVALENT_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_COMPILED_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_CREATION_ERROR_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_DUPLICATED_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_UNCOMPILABLE_MESSAGE;
import static org.codedefenders.util.Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT;
import static org.codedefenders.util.Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST;
import static org.codedefenders.util.Constants.TESTS_DIR;
import static org.codedefenders.util.Constants.TEST_DID_NOT_COMPILE_MESSAGE;
import static org.codedefenders.util.Constants.TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE;
import static org.codedefenders.util.Constants.TEST_DID_NOT_PASS_ON_CUT_MESSAGE;
import static org.codedefenders.util.Constants.TEST_GENERIC_ERROR_MESSAGE;
import static org.codedefenders.util.Constants.TEST_INVALID_MESSAGE;
import static org.codedefenders.util.Constants.TEST_KILLED_CLAIMED_MUTANT_MESSAGE;
import static org.codedefenders.util.Constants.TEST_PASSED_ON_CUT_MESSAGE;
import static org.codedefenders.validation.code.CodeValidator.DEFAULT_NB_ASSERTIONS;

public class GameManager extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(GameManager.class);

	// Based on info provided, navigate to the correct view for the user
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// Get the session information specific to the current user.
		HttpSession session = request.getSession();
		int uid = (Integer) session.getAttribute("uid");
		Object ogid = session.getAttribute("gid");
		String contextPath = request.getContextPath();

		if (ogid == null) {
			response.sendRedirect(contextPath + org.codedefenders.util.Paths.GAMES_OVERVIEW);
			return;
		}

		int gameId = (Integer) ogid;

		logger.debug("Getting game " + gameId + " for " + uid);

		DuelGame activeGame = DuelGameDAO.getDuelGameForId(gameId);
		session.setAttribute("game", activeGame);

		// If the game is finished, redirect to the score page.
		if (activeGame.getAttackerId() == uid) {
			List<Mutant> equivMutants = activeGame.getMutantsMarkedEquivalentPending();
			if (equivMutants.isEmpty()) {
				logger.info("Redirecting to attacker page");
				List<Mutant> aliveMutants = activeGame.getAliveMutants();
				if (aliveMutants.isEmpty()) {
					logger.info("No Mutants Alive, only attacker can play.");
					activeGame.setActiveRole(Role.ATTACKER);
					activeGame.update();
				}
				// If no mutants needed to be proved non-equivalent, direct to the Attacker Page.
				RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.DUEL_ATTACKER_VIEW_JSP);
				dispatcher.forward(request, response);
			} else {
				request.setAttribute("equivMutant", equivMutants.get(0));
				RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.DUEL_RESOLVE_EQUIVALENCE_JSP);
				dispatcher.forward(request, response);
			}
		} else {
			// Direct to the Defender Page.
			RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.DUEL_DEFENDER_VIEW_JSP);
			dispatcher.forward(request, response);
		}// else
//		Redirect.redirectBack(request, response);
	}

	// Based on the data provided, update information for the game
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		ArrayList<String> messages = new ArrayList<>();
		HttpSession session = request.getSession();
		int uid = (Integer) session.getAttribute("uid");

		session.setAttribute("messages", messages);

		DuelGame activeGame = (DuelGame) session.getAttribute("game");

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

					Test newTest = null;

					try {
						newTest = createTest(activeGame.getId(), activeGame.getClassId(), testText, uid, MODE_DUEL_DIR);
					} catch (CodeValidatorException e) {
						logger.warn("Swallow Exception", e);
						messages.add(TEST_GENERIC_ERROR_MESSAGE);
						session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
						response.sendRedirect(request.getContextPath()+"/"+activeGame.getClass().getSimpleName().toLowerCase());
						return;
					}
					if (newTest == null) {
						messages.add(String.format(TEST_INVALID_MESSAGE, DEFAULT_NB_ASSERTIONS));
						session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
						response.sendRedirect(request.getContextPath()+"/"+activeGame.getClass().getSimpleName().toLowerCase());
						return;
					}

					TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

					if (compileTestTarget != null && compileTestTarget.status.equals(TargetExecution.Status.SUCCESS)) {
						TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
						if (testOriginalTarget.status.equals(TargetExecution.Status.SUCCESS)) {
							logger.info(TEST_PASSED_ON_CUT_MESSAGE);
							if (mutant.isAlive() && mutant.getEquivalent().equals(Mutant.Equivalence.PENDING_TEST)) {
								// TODO: Allow multiple trials?
								// TODO: Doesnt differentiate between failing because the test didnt run and failing because it detected the mutant
								MutationTester.runEquivalenceTest(newTest, mutant);
								activeGame.endRound();
								activeGame.update();
								Mutant mutantAfterTest = activeGame.getMutantByID(currentEquivMutantID);
								if (mutantAfterTest.getEquivalent().equals(Mutant.Equivalence.PROVEN_NO)) {
									logger.info("Test {} killed mutant {}, hence NOT equivalent", newTest.getId(), mutant.getId());
									messages.add(TEST_KILLED_CLAIMED_MUTANT_MESSAGE);
								} else {
									// test did not kill the mutant, lost duel, kill mutant
									mutantAfterTest.kill(Mutant.Equivalence.ASSUMED_YES);

									logger.info("Test {} failed to kill mutant {}", newTest.getId(), mutant.getId());
									messages.add(TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE);
								}
								response.sendRedirect(request.getContextPath()+"/"+activeGame.getClass().getSimpleName().toLowerCase());
								return;
							} else {
								activeGame.endRound();
								activeGame.update();
								messages.add(TEST_KILLED_CLAIMED_MUTANT_MESSAGE);
								response.sendRedirect(request.getContextPath()+"/"+activeGame.getClass().getSimpleName().toLowerCase());
								return;
							}
						} else {
							//  (testOriginalTarget.state.equals(TargetExecution.Status.FAIL) || testOriginalTarget.state.equals(TargetExecution.Status.ERROR)
							logger.debug("testOriginalTarget: " + testOriginalTarget);
							messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
							messages.add(testOriginalTarget.message);
							session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
						}
					} else {
						logger.debug("compileTestTarget: " + compileTestTarget);
						messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
						messages.add(compileTestTarget.message);
						session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
					}
				} else if (request.getParameter("acceptEquivalent") != null) { // If the user didnt want to supply a test
					logger.info("Equivalence accepted for mutant {}", mutant.getId());
					if (mutant.isAlive() && mutant.getEquivalent().equals(Mutant.Equivalence.PENDING_TEST)) {
						mutant.kill(Mutant.Equivalence.DECLARED_YES);

						messages.add(MUTANT_ACCEPTED_EQUIVALENT_MESSAGE);
						activeGame.endRound();
						activeGame.update();
						response.sendRedirect(request.getContextPath()+"/"+activeGame.getClass().getSimpleName().toLowerCase());
						return;
					}
				}
				break;
			case "claimEquivalent":
				if (request.getParameter("mutantId") != null) {
					int mutantId = Integer.parseInt(request.getParameter("mutantId"));
					Mutant mutantClaimed = MutantDAO.getMutantById(mutantId);
					if (activeGame.getMode().equals(GameMode.SINGLE)) {
						// TODO: Why is this not handled in the single player game but here?
						//Singleplayer - use automatic system.
						if (AntRunner.potentialEquivalent(mutantClaimed)) {
							//Is potentially equiv - accept as equivalent
							mutantClaimed.kill(Mutant.Equivalence.DECLARED_YES);
							messages.add("The AI has accepted the mutant as equivalent.");
						} else {
							mutantClaimed.kill(Mutant.Equivalence.PROVEN_NO);
							messages.add("The AI has submitted a test that kills the mutant and proves it non-equivalent!");
						}
						activeGame.endTurn();
						if (!activeGame.getState().equals(GameState.FINISHED)) {
							//The ai should make another move if the game isn't over
							SinglePlayerGame spg = (SinglePlayerGame) activeGame;
							if (spg.getAi().makeTurn()) {
								messages.addAll(spg.getAi().getMessagesLastTurn());
							}
						}

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
				int gameId = Integer.parseInt(request.getParameter("gameID"));
				activeGame = DuelGameDAO.getDuelGameForId(gameId);
				String turn = activeGame.getActiveRole().equals(Role.ATTACKER) ? "attacker" : "defender";
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
				response.getWriter().write(turn);
				return;

			case "createMutant":

				// Get the text submitted by the user.
				String mutantText = request.getParameter("mutant");

				// Duels are always 'strict'
				ValidationMessage validationMessage = CodeValidator.validateMutantGetMessage(activeGame.getCUT().getSourceCode(), mutantText, CodeValidatorLevel.STRICT);
				if (validationMessage != ValidationMessage.MUTANT_VALIDATION_SUCCESS) {
					// Mutant is either the same as the CUT or it contains invalid code
					// Do not restore mutated code
					messages.add(validationMessage.get());
					break;
				}
				Mutant existingMutant = existingMutant(activeGame.getId(), mutantText);
				if (existingMutant != null) {
					messages.add(MUTANT_DUPLICATED_MESSAGE);
					TargetExecution existingMutantTarget = TargetExecutionDAO.getTargetExecutionForMutant(existingMutant, TargetExecution.Target.COMPILE_MUTANT);
					if (existingMutantTarget != null
							&& !existingMutantTarget.status.equals(TargetExecution.Status.SUCCESS)
							&& existingMutantTarget.message != null && !existingMutantTarget.message.isEmpty()) {
						messages.add(existingMutantTarget.message);
					}
					session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, StringEscapeUtils.escapeHtml(mutantText));
					break;
				}
				Mutant newMutant = createMutant(activeGame.getId(), activeGame.getClassId(), mutantText, uid, MODE_DUEL_DIR);
				if (newMutant != null) {
					TargetExecution compileMutantTarget = TargetExecutionDAO.getTargetExecutionForMutant(newMutant, TargetExecution.Target.COMPILE_MUTANT);
					if (compileMutantTarget != null && compileMutantTarget.status.equals(TargetExecution.Status.SUCCESS)) {
						messages.add(MUTANT_COMPILED_MESSAGE);
						MutationTester.runAllTestsOnMutant(activeGame, newMutant, messages);

						// TODO: Why doesnt that happen in SinglePlayerGame.endTurn()?
						if (activeGame.getMode().equals(GameMode.SINGLE)) {
							//Singleplayer - check for potential equivalent.
							if (AntRunner.potentialEquivalent(newMutant)) {
								//Is potentially equiv - mark as equivalent and update.
								messages.add("The AI has started an equivalence challenge on your last mutant.");
								newMutant.setEquivalent(Mutant.Equivalence.PENDING_TEST);
								newMutant.update();
								activeGame.update();
							} else {
								activeGame.endTurn();
								SinglePlayerGame g = (SinglePlayerGame) activeGame;
								if (g.getAi().makeTurn()) {
									messages.addAll(g.getAi().getMessagesLastTurn());
								}
							}
						} else {
							activeGame.endTurn();
						}
						activeGame.update();
					} else {
						messages.add(MUTANT_UNCOMPILABLE_MESSAGE);
						if (compileMutantTarget != null && compileMutantTarget.message != null && !compileMutantTarget.message.isEmpty())
							messages.add(compileMutantTarget.message);
						session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, StringEscapeUtils.escapeHtml(mutantText));
					}
				} else {
					messages.add(MUTANT_CREATION_ERROR_MESSAGE);
					session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, StringEscapeUtils.escapeHtml(mutantText));
					logger.error("Error creating mutant. Game: {}, Class: {}, User: {}", activeGame.getId(), activeGame.getClassId(), uid, mutantText);
				}
				break;

			case "createTest":

				// Get the text submitted by the user.
				String testText = request.getParameter("test");

				// If it can be written to file and compiled, end turn. Otherwise, dont.
				Test newTest = null;

				try {
					newTest = createTest(activeGame.getId(), activeGame.getClassId(), testText, uid, MODE_DUEL_DIR);
				} catch (CodeValidatorException e) {
					logger.warn("Swallow Exception", e);
					messages.add(TEST_GENERIC_ERROR_MESSAGE);
					session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
					response.sendRedirect(request.getContextPath()+"/"+activeGame.getClass().getSimpleName().toLowerCase());
					return;
				}


				if (newTest == null) {
					messages.add(String.format(TEST_INVALID_MESSAGE, DEFAULT_NB_ASSERTIONS));
					session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
					response.sendRedirect(request.getContextPath()+"/"+activeGame.getClass().getSimpleName().toLowerCase());
					return;
				}
				logger.debug("New Test " + newTest.getId());
				TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

				if (compileTestTarget != null && compileTestTarget.status.equals(TargetExecution.Status.SUCCESS)) {
					TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
					if (testOriginalTarget.status.equals(TargetExecution.Status.SUCCESS)) {
						messages.add(TEST_PASSED_ON_CUT_MESSAGE);
						MutationTester.runTestOnAllMutants(activeGame, newTest, messages);
						activeGame.endTurn();
						activeGame.update();
						// TODO: Why doesn't that simply happen in SinglePlayerGame.endTurn?
						// if single-player game is not finished, make a move
						if (!activeGame.getState().equals(GameState.FINISHED)
								&& activeGame.getMode().equals(GameMode.SINGLE)) {
							SinglePlayerGame g = (SinglePlayerGame) activeGame;
							if (g.getAi().makeTurn()) {
								messages.addAll(g.getAi().getMessagesLastTurn());
							}
						}
					} else {
						// testOriginalTarget.state.equals(TargetExecution.Status.FAIL) || testOriginalTarget.state.equals(TargetExecution.Status.ERROR)
						messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
						messages.add(testOriginalTarget.message);
						session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
					}
				} else {
					messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
					messages.add(compileTestTarget.message);
					session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
				}
				break;
		}

		response.sendRedirect(request.getContextPath()+"/"+activeGame.getClass().getSimpleName().toLowerCase());//doGet(request, response);
	}

	// TODO Phil 13/12/18: extract to utility class
	public static Mutant existingMutant(int gid, String mutatedCode) throws IOException {
		String md5Mutant = CodeValidator.getMD5FromText(mutatedCode);

		// return the mutant in the game with same MD5 if it exists; return null otherwise
		return MutantDAO.getMutantByGameAndMd5(gid, md5Mutant);
	}

	static boolean hasAttackerPendingMutantsInGame(int gid, int attackerId){
		for (Mutant m : MutantDAO.getValidMutantsForGame(gid)) {
			if (m.getPlayerId() == attackerId &&  m.getEquivalent() == Mutant.Equivalence.PENDING_TEST){
				return true;
			}
		}
		return false;
	}

    public static Mutant createMutant(int gid, int cid, String mutatedCode, int ownerId, String subDirectory) throws IOException {
        // Mutant is assumed valid here

        GameClass classMutated = GameClassDAO.getClassForId(cid);
        String classMutatedBaseName = classMutated.getBaseName();

        // Setup folder the files will go in
        File newMutantDir = FileUtils
                .getNextSubDir(Constants.MUTANTS_DIR + F_SEP + subDirectory + F_SEP + gid + F_SEP + ownerId);

        logger.info("NewMutantDir: {}", newMutantDir.getAbsolutePath());
        logger.info("Class Mutated: {} (basename: {})", classMutated.getName(), classMutatedBaseName);

        String mutantFileName = newMutantDir + F_SEP + classMutatedBaseName + JAVA_SOURCE_EXT;

        // We do not use a class with static methods to favor parallelism...
        MutantUtils mutantUtils = new MutantUtils();
        // Read from FS
		List<String> originalCode = FileUtils.readLines(Paths.get(classMutated.getJavaFile()));
        // Remove invalid diffs, like inserting blank lines
        String cleanedMutatedCode = mutantUtils.cleanUpMutatedCode( String.join("\n", originalCode), mutatedCode);
        // Write the Mutant String into a java file
        mutantUtils.storeMutantToFile(mutantFileName, cleanedMutatedCode);

        // sanity check
        assert CodeValidator.getMD5FromText(cleanedMutatedCode).equals(CodeValidator
                .getMD5FromFile(mutantFileName)) : "MD5 hashes differ between code as text and code from new file";

        // Compile the mutant and add it to the game if possible; otherwise,
        // TODO: delete these files created?
        return AntRunner.compileMutant(newMutantDir, mutantFileName, gid, classMutated, ownerId);
    }

	/**
	 * @param gid
	 * @param cid
	 * @param testText
	 * @param ownerId
	 * @param subDirectory - Directory inside data for test to go
	 * @return {@code null} if test is not valid
	 * @throws IOException
	 * @throws CodeValidatorException
	 */
	public static Test createTest(int gid, int cid, String testText, int ownerId, String subDirectory, int maxNumberOfAssertions) throws IOException, CodeValidatorException {
		GameClass classUnderTest = GameClassDAO.getClassForId(cid);

		File newTestDir = FileUtils.getNextSubDir(TESTS_DIR + F_SEP + subDirectory + F_SEP + gid + F_SEP + ownerId + F_SEP + "original");

		String javaFile = FileUtils.createJavaTestFile(newTestDir, classUnderTest.getBaseName(), testText);

		Test newTest = AntRunner.compileTest(newTestDir, javaFile, gid, classUnderTest, ownerId);

		TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest,
				TargetExecution.Target.COMPILE_TEST);

		// If the test did not compile we short circuit here. We shall not return null
		if (compileTestTarget == null || !compileTestTarget.status.equals(TargetExecution.Status.SUCCESS)) {
			return newTest;
		}

		// Validate code or short circuit here
		final String testCode = new String(Files.readAllBytes(Paths.get(javaFile)));
		if (!CodeValidator.validateTestCode(testCode, maxNumberOfAssertions)) {
			return null;
		}

		// Eventually check the test actually passes when applied to the original code.
		if (compileTestTarget.status.equals(TargetExecution.Status.SUCCESS)) {
			AntRunner.testOriginal(newTestDir, newTest);
			try {
				// Detect test smell and store them to DB
				TestSmellDetector testSmellDetector = TestSmellDetector.createTestSmellDetector();
				TestFile testFile = new TestFile("", newTest.getJavaFile(), classUnderTest.getJavaFile());
				testSmellDetector.detectSmells(testFile);
				TestSmellsDAO.storeSmell(newTest, testFile);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return newTest;
	}

	public static Test createTest(int gid, int cid, String testText, int ownerId, String subDirectory) throws IOException, CodeValidatorException {
		return createTest(gid, cid, testText, ownerId, subDirectory, DEFAULT_NB_ASSERTIONS);
	}
}