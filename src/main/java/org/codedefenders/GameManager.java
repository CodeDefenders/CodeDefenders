package org.codedefenders;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import org.apache.commons.lang.ArrayUtils;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.codedefenders.singleplayer.SinglePlayerGame;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;

import static org.codedefenders.Constants.*;
import static org.codedefenders.Mutant.Equivalence.ASSUMED_YES;

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

		System.out.println("Getting game " + gid + " for " + uid);

		Game activeGame = DatabaseAccess.getGameForKey("ID", gid);
		session.setAttribute("game", activeGame);

		// If the game is finished, redirect to the score page.
		if (activeGame.getAttackerId() == uid) {
			ArrayList<Mutant> equivMutants = activeGame.getMutantsMarkedEquivalent();
			if (equivMutants.isEmpty()) {
				System.out.println("Redirecting to attacker page");
				ArrayList<Mutant> aliveMutants = activeGame.getAliveMutants();
				if (aliveMutants.isEmpty()) {
					System.out.println("No Mutants Alive, only attacker can play.");
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
				logger.debug("Executing Action resolveEquivalence");
				System.out.println("MultiplayerGame Manager Executing Action resolveEquivalence");
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
							System.out.println(TEST_PASSED_ON_CUT_MESSAGE);
							if (mutant.isAlive() && mutant.getEquivalent().equals(Mutant.Equivalence.PENDING_TEST)) {
								// Doesnt differentiate between failing because the test didnt run and failing because it detected the mutant
								MutationTester.runEquivalenceTest(newTest, mutant);
								activeGame.endRound();
								activeGame.update();
								Mutant mutantAfterTest = activeGame.getMutantByID(currentEquivMutantID);
								if (mutantAfterTest.getEquivalent().equals(ASSUMED_YES)) {
									logger.info("Test failed to kill the mutant, hence assumed equivalent");
									messages.add(TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE);
								} else { // PROVEN_NO
									logger.info("Mutant was killed, hence tagged not equivalent");
									messages.add(TEST_KILLED_CLAIMED_MUTANT_MESSAGE);
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
				} else if (request.getParameter("acceptEquivalent") != null) { // If the user didnt want to supply a test
					logger.debug("Equivalence accepted");
					System.out.println("Equivalence accepted");
					if (mutant.isAlive() && mutant.getEquivalent().equals(Mutant.Equivalence.PENDING_TEST)) {
						mutant.setEquivalent(Mutant.Equivalence.DECLARED_YES);
						mutant.kill();

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
							//Is potentially equiv - mark as equivalent and update.
							mutantClaimed.setEquivalent(Mutant.Equivalence.DECLARED_YES);
							mutantClaimed.kill();
						} else {
							mutantClaimed.setEquivalent(Mutant.Equivalence.PROVEN_NO);
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
					messages.add(MUTANT_IDENTICAL_MESSAGE);
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
				System.out.println("New Test " + newTest.getId());
				TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

				if (compileTestTarget.status.equals("SUCCESS")) {
					TargetExecution testOriginalTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
					if (testOriginalTarget.status.equals("SUCCESS")) {
						messages.add(TEST_PASSED_ON_CUT_MESSAGE);
						MutationTester.runTestOnAllMutants(activeGame, newTest, messages);
						activeGame.endTurn();
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
				break;
		}
		if(activeGame.getMode().equals(Game.Mode.SINGLE)) {
			//Singleplayer game, show messages depending on state.
			if(activeGame.getAttackerId() == uid) {
				//Player attacker
				messages.add("The AI has created a test!");
			} else {
				//Player defender
				messages.add("The AI has created a mutant!");
			}
		}
		response.sendRedirect("play");//doGet(request, response);
	}

	// Writes text as a Mutant to the appropriate place in the file system.
	public Mutant createMutant(int gid, int cid, String mutatedCode, int ownerId, String subDirectory) throws IOException {

		GameClass classMutated = DatabaseAccess.getClassForKey("Class_ID", cid);
		String classMutatedBaseName = classMutated.getBaseName();

		File sourceFile = new File(classMutated.getJavaFile());
		String sourceCode = new String(Files.readAllBytes(sourceFile.toPath()));

		// If there were no differences, return, as the mutant is the same as original.
		if (! validMutant(sourceCode, mutatedCode))
			return null;

		// Setup folder the files will go in
		File newMutantDir = FileManager.getNextSubDir(Constants.MUTANTS_DIR + F_SEP + subDirectory + F_SEP + gid + F_SEP + ownerId);

		System.out.println("NewMutantDir: " + newMutantDir.getAbsolutePath());
		System.out.println("Class Mutated: " + classMutated.getName() + "(basename: " + classMutatedBaseName +")");

		// Write the Mutant String into a java file
		String mutantFileName = newMutantDir + F_SEP + classMutatedBaseName + JAVA_SOURCE_EXT;
		File mutantFile = new File(mutantFileName);
		FileWriter fw = new FileWriter(mutantFile);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(mutatedCode);
		bw.close();

		// Compile the mutant - if you can, add it to the MultiplayerGame State, otherwise, delete these files created.
		return AntRunner.compileMutant(newMutantDir, mutantFileName, gid, classMutated, ownerId);
	}

	private boolean validMutant(String originalCode, String mutatedCode) {
		// Runs diff match patch between the two Strings to see if there are any differences.
		DiffMatchPatch dmp = new DiffMatchPatch();
		LinkedList<DiffMatchPatch.Diff> changes = dmp.diffMain(originalCode.trim().replace("\n", "").replace("\r", ""), mutatedCode.trim().replace("\n", "").replace("\r", ""), true);
		boolean change = false;
		for (DiffMatchPatch.Diff d : changes) {
			if (d.operation != DiffMatchPatch.Operation.EQUAL) {
				change = true;
			}
		}
		return change;
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
	public Test createTest(int gid, int cid, String testText, int ownerId, String subDirectory) throws IOException {

		GameClass classUnderTest = DatabaseAccess.getClassForKey("Class_ID", cid);

		File newTestDir = FileManager.getNextSubDir(TESTS_DIR + F_SEP + subDirectory + F_SEP + gid + F_SEP + ownerId);

		String javaFile = FileManager.createJavaFile(newTestDir, classUnderTest.getBaseName(), testText);

		if (! validTestCode(javaFile)) {
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


	private boolean validTestCode(String javaFile) throws IOException {

		CompilationUnit cu;
		FileInputStream in = null;
		try {
			in = new FileInputStream(javaFile);
			// parse the file
			cu = JavaParser.parse(in);
			// prints the resulting compilation unit to default system output
			if (cu.getTypes().size() != 1) {
				System.out.println("Invalid test suite contains more than one type declaration.");
				return false;
			}
			TypeDeclaration clazz = cu.getTypes().get(0);
			if (clazz.getMembers().size() != 1) {
				System.out.println("Invalid test suite contains more than one method.");
				return false;
			}
			MethodDeclaration test = (MethodDeclaration)clazz.getMembers().get(0);
			BlockStmt testBody = test.getBody();
			if (testBody.getStmts().isEmpty()) {
				System.out.println("Empty test (no statement).");
				return false;
			}

			int assertionCount = 0;
			for (Node node : testBody.getChildrenNodes()) {
				if (node instanceof ForeachStmt
						|| node instanceof IfStmt
						|| node instanceof ForStmt
						|| node instanceof WhileStmt
						|| node instanceof DoStmt) {
					System.out.println("Invalid test contains " + node.getClass().getSimpleName() + " statement");
					return false;
				}
				if (isAssertion(node)) {
					assertionCount++;
					if (assertionCount > 2)
						return false;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} finally {
			in.close();
		}
		return true;
	}

	private boolean isAssertion(Node node) {
		if (node instanceof AssertStmt)
			return true;
		if (node instanceof ExpressionStmt) {
			ExpressionStmt exprStmt = (ExpressionStmt) node;
			if ((exprStmt.getExpression() instanceof MethodCallExpr)) {
				MethodCallExpr call = (MethodCallExpr)exprStmt.getExpression();
				if (ArrayUtils.contains(new String[]{"assertEquals", "assertTrue", "assertFalse", "assertNull", "assertNotNull", "assertSame", "assertNotSame", "assertArrayEquals"}, call.getName()))
					return true;
			}
		}
		return false;
	}
}
