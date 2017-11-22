package org.codedefenders.multiplayer;

import org.codedefenders.*;
import org.codedefenders.events.Event;
import org.codedefenders.events.EventStatus;
import org.codedefenders.events.EventType;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.validation.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.codedefenders.Constants.*;
import static org.codedefenders.Mutant.Equivalence.ASSUMED_YES;
import static org.codedefenders.Mutant.Equivalence.PROVEN_NO;

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
				if(activeGame.getState().equals(GameState.CREATED)) {
					logger.info("Starting multiplayer game {} (Setting state to ACTIVE)", activeGame.getId());
					activeGame.setState(GameState.ACTIVE);
					activeGame.update();

				}
				break;
			}
			case "endGame": {
				if(activeGame.getState().equals(GameState.ACTIVE)) {
					logger.info("Ending multiplayer game {} (Setting state to FINISHED)", activeGame.getId());
					activeGame.setState(GameState.FINISHED);
					activeGame.update();

					response.sendRedirect("games");
					return;
				} else {
					break;
				}
			}
			case "resolveEquivalence": {
				int currentEquivMutantID = Integer.parseInt(request.getParameter("currentEquivMutant"));

				if (activeGame.getState().equals(GameState.FINISHED)){
					messages.add(String.format("Game %d has finished.", activeGame.getId()));
					response.sendRedirect("games");
				}

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

				logger.info("Executing Action resolveEquivalence for mutant {} and test {}", currentEquivMutantID, newTest.getId());
				TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

				if (compileTestTarget.status.equals("SUCCESS")) {
					TargetExecution testOriginalTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
					if (testOriginalTarget.status.equals("SUCCESS")) {
						logger.info("Test {} passed on the CUT", newTest.getId());

						// Instead of running equivalence on only one mutant, let's try with all mutants pending resolution
						List<Mutant> mutantsPendingTests = activeGame.getMutantsMarkedEquivalentPending();
						boolean killedClaimed = false;
						int killedOthers = 0;
						for (Mutant mPending : mutantsPendingTests) {
							// TODO: Doesnt distinguish between failing because the test didnt run at all and failing because it detected the mutant
							MutationTester.runEquivalenceTest(newTest, mPending); // updates mPending
							if (mPending.getEquivalent().equals(PROVEN_NO)) {
								logger.info("Test {} killed mutant {} and proved it non-equivalent", newTest.getId(), mPending.getId());
								newTest.setScore(0); // score 2 points for proving a mutant non-equivalent
								Event notif = new Event(-1, activeGame.getId(),
										uid,
										DatabaseAccess.getUser(uid)
												.getUsername() +
										" killed a mutant in an equivalence " +
												"duel.",
										EventType.ATTACKER_MUTANT_KILLED_EQUIVALENT, EventStatus.GAME,
										new Timestamp(System.currentTimeMillis()));
								notif.insert();
								if (mPending.getId() == currentEquivMutantID)
									killedClaimed = true;
								else
									killedOthers++;
							} else { // ASSUMED_YES
								if (mPending.getId() == currentEquivMutantID) {
									// only kill the one mutant that was claimed
									mPending.kill(ASSUMED_YES);
									Event notif = new Event(-1, activeGame.getId(),
											uid,
											DatabaseAccess.getUser(uid)
													.getUsername() + " lost " +
													"an equivalence duel. " +
													"Mutant is assumed " +
													"equivalent.",
											EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.GAME,
											new Timestamp(System.currentTimeMillis()));
									notif.insert();
								}
								logger.info("Test {} failed to kill mutant {}, hence mutant is assumed equivalent", newTest.getId(), mPending.getId());
							}
						}
						if (killedClaimed) {
							messages.add(TEST_KILLED_CLAIMED_MUTANT_MESSAGE);
							if (killedOthers==1)
								messages.add("...and it also killed another claimed mutant!");
							else if (killedOthers>1)
								messages.add(String.format("...and it also killed other %d claimed mutants!", killedOthers));
						} else {
							messages.add(TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE);
							if (killedOthers==1)
								messages.add("...however, your test did kill another claimed mutant!");
							else if (killedOthers>1)
								messages.add(String.format("...however, your test killed other %d claimed mutants!", killedOthers));
						}
						newTest.update();
						activeGame.update();
						response.sendRedirect("play");
						return;
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
			}
				break;

			case "createMutant":

				if (activeGame.getState().equals(GameState.ACTIVE)) {

					// Get the text submitted by the user.
					String mutantText = request.getParameter("mutant");

					if (! GameManager.isMutantValid(activeGame.getClassId(), mutantText)) {
						// Mutant is either the same as the CUT or it contains invalid code
						// Do not restore mutated code
						messages.add(MUTANT_INVALID_MESSAGE);
						break;
					}
					Mutant existingMutant = GameManager.existingMutant(activeGame.getId(), mutantText);
					if (existingMutant != null) {
						messages.add(MUTANT_DUPLICATED_MESSAGE);
						TargetExecution existingMutantTarget = DatabaseAccess.getTargetExecutionForMutant(existingMutant, TargetExecution.Target.COMPILE_MUTANT);
						if (existingMutantTarget != null
								&& !existingMutantTarget.status.equals("SUCCESS")
								&& existingMutantTarget.message != null && !existingMutantTarget .message.isEmpty()) {
							messages.add(existingMutantTarget.message);
						}
						session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, mutantText);
						break;
					}
					Mutant newMutant = GameManager.createMutant(activeGame.getId(), activeGame.getClassId(), mutantText, uid, "mp");
					if (newMutant != null) {
						TargetExecution compileMutantTarget = DatabaseAccess.getTargetExecutionForMutant(newMutant, TargetExecution.Target.COMPILE_MUTANT);
						if (compileMutantTarget != null && compileMutantTarget.status.equals("SUCCESS")) {
							Event notif = new Event(-1, activeGame.getId(),
									uid,
									DatabaseAccess.getUser(uid)
											.getUsername() + " created a " +
											"mutant.",
									EventType.ATTACKER_MUTANT_CREATED, EventStatus
									.GAME,
									new Timestamp(System.currentTimeMillis()
											- 1000));
							notif.insert();
							messages.add(MUTANT_COMPILED_MESSAGE);
							MutationTester.runAllTestsOnMutant(activeGame, newMutant, messages);
							activeGame.update();
						} else {
							messages.add(MUTANT_UNCOMPILABLE_MESSAGE);
							if (compileMutantTarget != null && compileMutantTarget.message != null && !compileMutantTarget.message.isEmpty())
								messages.add(compileMutantTarget.message);
							session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, mutantText);
						}
					} else {
						messages.add(MUTANT_CREATION_ERROR_MESSAGE);
						session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, mutantText);
						logger.error("Error creating mutant. Game: {}, Class: {}, User: {}", activeGame.getId(), activeGame.getClassId(), uid, mutantText);
					}
				} else {
					messages.add(GRACE_PERIOD_MESSAGE);
				}
				break;

			case "createTest":
				if (activeGame.getState().equals(GameState.ACTIVE)) {
					// Get the text submitted by the user.
					String testText = request.getParameter("test");

					// If it can be written to file and compiled, end turn. Otherwise, dont.
					Test newTest = GameManager.createTest(activeGame.getId(), activeGame.getClassId(), testText, uid, "mp");

					logger.info("New Test " + newTest.getId() + " by user " + uid);
					TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

					if (compileTestTarget != null && compileTestTarget.status.equals("SUCCESS")) {
						if (! CodeValidator.validTestCode(newTest.getJavaFile())) {
							messages.add(TEST_INVALID_MESSAGE);
							session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
							response.sendRedirect("play");
							return;
						}
						// the test is valid, but does it pass on the original class?
						TargetExecution testOriginalTarget = AntRunner.testOriginal(new File(newTest.getDirectory()), newTest);
						if (testOriginalTarget.status.equals("SUCCESS")) {
							messages.add(TEST_PASSED_ON_CUT_MESSAGE);

							Event notif = new Event(-1, activeGame.getId(),
									uid,
									DatabaseAccess.getUser(uid)
											.getUsername() + " created a test",
									EventType.DEFENDER_TEST_CREATED, EventStatus.GAME,
									new Timestamp(System.currentTimeMillis()));
							notif.insert();

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
