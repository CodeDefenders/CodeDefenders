package org.codedefenders.multiplayer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.*;
import org.codedefenders.*;
import org.codedefenders.validation.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class MultiplayerGameManager extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(MultiplayerGameManager.class);

	// Based on the data provided, update information for the game
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		ArrayList<String> messages = new ArrayList<String>();
		HttpSession session = request.getSession();
		int uid = (Integer) session.getAttribute("uid");
		int gameId = (Integer) session.getAttribute("mpGameId");
		session.setAttribute("messages", messages);

		MultiplayerGame activeGame = DatabaseAccess.getMultiplayerGame(gameId);

		switch (request.getParameter("formType")) {
			case "startGame": {
				if(activeGame.getState().equals(AbstractGame.State.CREATED)) {
					System.out.println("Starting party game " + activeGame.getId() + " (Setting state to ACTIVE)");
					activeGame.setState(AbstractGame.State.ACTIVE);
					activeGame.update();
				}
				break;
			}
			case "endGame": {
				if(activeGame.getState().equals(AbstractGame.State.ACTIVE)) {
					System.out.println("Ending party game " + activeGame.getId() + " (Setting state to FINISHED)");
					activeGame.setState(AbstractGame.State.FINISHED);
					activeGame.update();
					response.sendRedirect("games");
					return;
				} else {
					break;
				}
			}
			case "resolveEquivalence": {
				logger.debug("Executing Action resolveEquivalence");
				System.out.println("MultiplayerGame Manager Executing Action resolveEquivalence");
				int currentEquivMutantID = Integer.parseInt(request.getParameter("currentEquivMutant"));
				Mutant mutant = activeGame.getMutantByID(currentEquivMutantID);
				System.out.println("CurrentEquivMutant ID = " + currentEquivMutantID);

				if (activeGame.getState().equals(AbstractGame.State.FINISHED)){
					response.sendRedirect("games");
				}

				// Check type of equivalence response.
				//logger.debug("Equivalence rejected, going to process killing test form mutant " + currentEquivMutantID);
				//System.out.println("Equivalence rejected, going to process killing test for mutant " + currentEquivMutantID);

				// Get the text submitted by the user.
				String testText = request.getParameter("test");

				// If it can be written to file and compiled, end turn. Otherwise, dont.
				Test newTest = GameManager.createTest(activeGame.getId(), activeGame.getClassId(), testText, uid, "mp");

				if (newTest == null) {
					messages.add(TEST_INVALID_MESSAGE);
					session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
					response.sendRedirect("multiplayer/play");
					return;
				}

				TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

				if (compileTestTarget.status.equals("SUCCESS")) {
					TargetExecution testOriginalTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
					if (testOriginalTarget.status.equals("SUCCESS")) {
						System.out.println(TEST_PASSED_ON_CUT_MESSAGE);
						if (mutant.isAlive() && mutant.getEquivalent().equals(Mutant.Equivalence.PENDING_TEST)) {
							// Doesnt differentiate between failing because the test didnt run and failing because it detected the mutant

							MutationTester.runEquivalenceTest(newTest, mutant);
							activeGame.update();
							Mutant mutantAfterTest = activeGame.getMutantByID(currentEquivMutantID);
							if (mutantAfterTest.getEquivalent().equals(ASSUMED_YES)) {
								logger.info("Test failed to kill the mutant, hence assumed equivalent");
								messages.add(TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE);
							} else { // PROVEN_NO
								logger.info("Mutant was killed, hence tagged not equivalent");
								messages.add(TEST_KILLED_CLAIMED_MUTANT_MESSAGE);

								ArrayList<Mutant> mm = new ArrayList<Mutant>();
								mm.add(mutantAfterTest);
								newTest.setScore(2);
								newTest.update();
							}

							MutationTester.runTestOnAllMultiplayerMutants(activeGame, newTest, messages);
							activeGame.update();

							response.sendRedirect("play");
							return;
						} else {
							activeGame.update();
							messages.add(TEST_KILLED_CLAIMED_MUTANT_MESSAGE);
							response.sendRedirect("play");
							return;
						}
					} else {
						//  (testOriginalTarget.state.equals("FAIL") || testOriginalTarget.state.equals("ERROR")
						System.out.println("testOriginalTarget: " + testOriginalTarget);
						messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
						messages.add(testOriginalTarget.message);
						session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
					}
				} else {
					System.out.println("compileTestTarget: " + compileTestTarget);
					messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
					messages.add(compileTestTarget.message);
					session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
				}
			}
				break;

			case "createMutant":

				if (activeGame.getState().equals(AbstractGame.State.ACTIVE)) {

					// Get the text submitted by the user.
					String mutantText = request.getParameter("mutant");

					Mutant newMutant = GameManager.createMutant(activeGame.getId(), activeGame.getClassId(), mutantText, uid, "mp");
					if (newMutant != null) {
						TargetExecution compileMutantTarget = DatabaseAccess.getTargetExecutionForMutant(newMutant, TargetExecution.Target.COMPILE_MUTANT);
						if (compileMutantTarget != null && compileMutantTarget.status.equals("SUCCESS")) {
							messages.add(MUTANT_COMPILED_MESSAGE);
							MutationTester.runAllTestsOnMultiplayerMutant(activeGame, newMutant, messages);
							activeGame.update();
						} else {
							messages.add(MUTANT_UNCOMPILABLE_MESSAGE);
							if (compileMutantTarget != null && compileMutantTarget.message != null && !compileMutantTarget.message.isEmpty())
								messages.add(compileMutantTarget.message);
							session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, mutantText);
						}
					} else {
						// Create Mutant failed because there were no differences between mutant and original, returning -1
						messages.add(MUTANT_INVALID_MESSAGE);
					}
				} else {
					messages.add(GRACE_PERIOD_MESSAGE);
				}
				break;

			case "createTest":
				if (activeGame.getState().equals(AbstractGame.State.ACTIVE)) {
					// Get the text submitted by the user.
					String testText = request.getParameter("test");

					// If it can be written to file and compiled, end turn. Otherwise, dont.
					Test newTest = GameManager.createTest(activeGame.getId(), activeGame.getClassId(), testText, uid, "mp");
					if (newTest == null) {
						messages.add(TEST_INVALID_MESSAGE);
						session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
						response.sendRedirect("play");
						return;
					}
					logger.info("New Test " + newTest.getId() + " by user " + uid);
					TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

					if (compileTestTarget.status.equals("SUCCESS")) {
						TargetExecution testOriginalTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
						if (testOriginalTarget.status.equals("SUCCESS")) {
							messages.add(TEST_PASSED_ON_CUT_MESSAGE);
							MutationTester.runTestOnAllMultiplayerMutants(activeGame, newTest, messages);
							activeGame.update();
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
				} else {
					messages.add(GRACE_PERIOD_MESSAGE);
				}
				break;
		}
		response.sendRedirect("play");//doGet(request, response);
	}

}
