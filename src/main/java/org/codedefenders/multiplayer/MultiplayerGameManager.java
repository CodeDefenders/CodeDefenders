package org.codedefenders.multiplayer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.*;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.codedefenders.*;
import org.codedefenders.scoring.Scorer;
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
import java.util.Arrays;
import java.util.LinkedList;

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

				// Check type of equivalence response.
				//logger.debug("Equivalence rejected, going to process killing test form mutant " + currentEquivMutantID);
				//System.out.println("Equivalence rejected, going to process killing test for mutant " + currentEquivMutantID);

				// Get the text submitted by the user.
				String testText = request.getParameter("test");

				// If it can be written to file and compiled, end turn. Otherwise, dont.
				Test newTest = createTest(activeGame.getId(), activeGame.getClassId(), testText, uid, "mp");

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
								newTest.setScore(Scorer.score(activeGame, newTest, mm));
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

					int attackerId = DatabaseAccess.getPlayerIdForMultiplayerGame(uid, gameId);

					Mutant newMutant = createMultiplayerMutant(activeGame.getId(), activeGame.getClassId(), mutantText, attackerId, "mp");
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
						messages.add(MUTANT_IDENTICAL_MESSAGE);
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
					Test newTest = createTest(activeGame.getId(), activeGame.getClassId(), testText, uid, "mp");
					logger.info("New Test " + newTest.getId() + " by user " + uid);

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

	public Mutant createMultiplayerMutant(int gid, int cid, String mutantText, int ownerId, String subDirectory) throws IOException {

		GameClass classMutated = DatabaseAccess.getClassForKey("Class_ID", cid);
		String classMutatedBaseName = classMutated.getBaseName();

		File sourceFile = new File(classMutated.getJavaFile());
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
		File newMutantDir = FileManager.getNextSubDir(Constants.MUTANTS_DIR + F_SEP + subDirectory + F_SEP + gid + F_SEP + ownerId);

		System.out.println("NewMutantDir: " + newMutantDir.getAbsolutePath());
		System.out.println("Class Mutated: " + classMutated.getName() + "(basename: " + classMutatedBaseName +")");

		// Write the Mutant String into a java file
		String mutantFileName = newMutantDir + F_SEP + classMutatedBaseName + JAVA_SOURCE_EXT;
		File mutantFile = new File(mutantFileName);
		FileWriter fw = new FileWriter(mutantFile);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(mutantText);
		bw.close();

		// Compile the mutant - if you can, add it to the MultiplayerGame State, otherwise, delete these files created.
		return AntRunner.compileMultiplayerMutant(newMutantDir, mutantFileName, gid, classMutated, ownerId);
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

		File newTestDir = FileManager.getNextSubDir(TESTS_DIR+ F_SEP + subDirectory + F_SEP + gid + F_SEP + ownerId);

		String javaFile = createJavaFile(newTestDir, classUnderTest.getBaseName(), testText);

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

	private String createJavaFile(File dir, String classBaseName, String testCode) throws IOException {
		String javaFile = dir.getAbsolutePath() + F_SEP + TEST_PREFIX + classBaseName + JAVA_SOURCE_EXT;
		File testFile = new File(javaFile);
		FileWriter testWriter = new FileWriter(testFile);
		BufferedWriter bufferedTestWriter = new BufferedWriter(testWriter);
		bufferedTestWriter.write(testCode);
		bufferedTestWriter.close();
		return javaFile;
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

			for (Node node : testBody.getChildrenNodes()) {
				if (node instanceof ForeachStmt
						|| node instanceof IfStmt
						|| node instanceof ForStmt
						|| node instanceof WhileStmt
						|| node instanceof DoStmt) {
					System.out.println("Invalid test contains " + node.getClass().getSimpleName() + " statement");
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
}
