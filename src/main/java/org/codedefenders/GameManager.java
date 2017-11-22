package org.codedefenders;

import org.codedefenders.duel.DuelGame;
import org.codedefenders.singleplayer.SinglePlayerGame;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.util.FileManager;
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
import java.util.List;

import static org.codedefenders.Constants.*;

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

        DuelGame activeGame = DatabaseAccess.getGameForKey("ID", gid);
        session.setAttribute("game", activeGame);

        // If the game is finished, redirect to the score page.
        if (activeGame.getAttackerId() == uid) {
            List<Mutant> equivMutants = activeGame.getMutantsMarkedEquivalent();
            if (equivMutants.isEmpty()) {
                logger.info("Redirecting to attacker page");
                List<Mutant> aliveMutants = activeGame.getAliveMutants();
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
                                if (mutantAfterTest.getEquivalent().equals(Mutant.Equivalence.PROVEN_NO)) {
                                    logger.info("Test {} killed mutant {}, hence NOT equivalent", newTest.getId(), mutant.getId());
                                    messages.add(TEST_KILLED_CLAIMED_MUTANT_MESSAGE);
                                } else {
                                    // test did not kill the mutant, lost duel, kill mutant
                                    mutantAfterTest.kill(Mutant.Equivalence.ASSUMED_YES);

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

                if (!isMutantValid(activeGame.getClassId(), mutantText)) {
                    // Mutant is either the same as the CUT or it contains invalid code
                    // Do not restore mutated code
                    messages.add(MUTANT_INVALID_MESSAGE);
                    break;
                }
                Mutant existingMutant = existingMutant(activeGame.getId(), mutantText);
                if (existingMutant != null) {
                    messages.add(MUTANT_DUPLICATED_MESSAGE);
                    TargetExecution existingMutantTarget = DatabaseAccess.getTargetExecutionForMutant(existingMutant, TargetExecution.Target.COMPILE_MUTANT);
                    if (existingMutantTarget != null
                            && !existingMutantTarget.status.equals("SUCCESS")
                            && existingMutantTarget.message != null && !existingMutantTarget.message.isEmpty()) {
                        messages.add(existingMutantTarget.message);
                    }
                    session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, mutantText);
                    break;
                }
                Mutant newMutant = createMutant(activeGame.getId(), activeGame.getClassId(), mutantText, uid, "sp");
                if (newMutant != null) {
                    TargetExecution compileMutantTarget = DatabaseAccess.getTargetExecutionForMutant(newMutant, TargetExecution.Target.COMPILE_MUTANT);
                    if (compileMutantTarget != null && compileMutantTarget.status.equals("SUCCESS")) {
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
                        session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, mutantText);
                    }
                } else {
                    messages.add(MUTANT_CREATION_ERROR_MESSAGE);
                    session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, mutantText);
                    logger.error("Error creating mutant. Game: {}, Class: {}, User: {}", activeGame.getId(), activeGame.getClassId(), uid, mutantText);
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

    public static boolean isMutantValid(int cid, String mutatedCode) throws IOException {
        GameClass classMutated = DatabaseAccess.getClassForKey("Class_ID", cid);

        File sourceFile = new File(classMutated.getJavaFile());
        String sourceCode = new String(Files.readAllBytes(sourceFile.toPath()));

        // is it an actual mutation?
        String md5CUT = CodeValidator.getMD5(sourceCode);
        String md5Mutant = CodeValidator.getMD5(mutatedCode);

        // mutant is valid only if it differs from CUT and does not contain forbidden constructs
        return (!md5CUT.equals(md5Mutant)) && CodeValidator.validMutant(sourceCode, mutatedCode);
    }

    public static Mutant existingMutant(int gid, String mutatedCode) throws IOException {
        String md5Mutant = CodeValidator.getMD5(mutatedCode);

        // return the mutant in the game with same MD5 if it exists; return null otherwise
        return DatabaseAccess.getMutant(gid, md5Mutant);
    }

    public static Mutant createMutant(int gid, int cid, String mutatedCode, int ownerId, String subDirectory) throws IOException {
        // Mutant is assumed valid here

        GameClass classMutated = DatabaseAccess.getClassForKey("Class_ID", cid);
        String classMutatedBaseName = classMutated.getBaseName();

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

        // sanity check
        String md5Mutant = CodeValidator.getMD5(mutatedCode);
        String md5FromMutantFile = CodeValidator.getMD5FromFile(mutantFileName);
        assert md5Mutant.equals(md5FromMutantFile) : "MD5 hashes differ between code as text and code from new file";

        // Compile the mutant and add it to the game if possible; otherwise, TODO: delete these files created?
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