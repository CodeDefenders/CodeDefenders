package org.codedefenders;

/**
 * Created by joe on 29/03/2017.
 * Loading the puzzle 
 */

import com.sun.org.apache.bcel.internal.classfile.Code;
import javassist.ClassPool;
import javassist.CtClass;
import jdk.nashorn.internal.ir.RuntimeNode;
import org.apache.commons.io.FileUtils;
import org.codedefenders.story.StoryGame;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.codedefenders.Constants.*;

public class PuzzleManager extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(PuzzleManager.class);

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        HttpSession session = req.getSession();
        int uid = (Integer) session.getAttribute("uid");
        Object puzzleId = session.getAttribute("pid");
        //session.setAttribute("pid",puzzleId);

        if (puzzleId==null) {
            res.sendRedirect("/story/view");
            return;
        }

        int pid = (Integer) puzzleId;

        logger.debug("Getting puzzle " + pid + " for " + uid);

        StoryGame thisPuzzle = DatabaseAccess.getActivePuzzle(pid);
        session.setAttribute("puzzle", thisPuzzle);
        // once clicked open, it is no longer considered 'Unattempted', change to 'in progress'
        if (thisPuzzle.getStoryState().equals(StoryState.UNATTEMPTED)) {
            thisPuzzle.updateState(StoryState.IN_PROGRESS);
        }

        if (thisPuzzle.getStoryMode().equals(PuzzleMode.ATTACKER)) { //redirect to attacker view
            logger.info("Redirecting to puzzle attacker page");

            RequestDispatcher dispatcher = req.getRequestDispatcher(STORY_ATT_VIEW_JSP);
            dispatcher.forward(req,res);
        } else {
            logger.info("Redirecting to puzzle defender page");
            RequestDispatcher dispatcher = req.getRequestDispatcher(STORY_DEF_VIEW_JSP);
            dispatcher.forward(req,res);
        }

    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        ArrayList<String> messages = new ArrayList<String>();
        HttpSession session = req.getSession();
        int uid = (Integer) session.getAttribute("uid");

        session.setAttribute("messages",messages);

        StoryGame thisPuzzle = (StoryGame) session.getAttribute("puzzle");

        switch (req.getParameter("formType")) {

            // attacker
            case "createMutant":

                String mutantText = req.getParameter("mutant");

                // compiles the mutant
                PuzzleMutant newMutant = createMutant(thisPuzzle.getPuzzleId(), thisPuzzle.getClassId(), mutantText, uid);

                if (newMutant != null) {
                    TargetExecution compileMutantTarget = DatabaseAccess.getTargetExecutionForPMutant(newMutant, TargetExecution.Target.COMPILE_MUTANT);
                    if (compileMutantTarget != null && compileMutantTarget.status.equals("SUCCESS")) {
                        logger.info("Mutant successfully compiled");
                        // get tests on the created mutant
                        List<PuzzleTest> tests = DatabaseAccess.getTestsforPuzzle(thisPuzzle.getPuzzleId());

                        int testsPassed = 0;
                        int totalTests = 0;
                        for (PuzzleTest test : tests) {
                            totalTests++;
                            if (!AntRunner.testKillsMutantStory(newMutant, test)) { // runs the tests on the mutant
                                testsPassed++; // user submitted mutant passes test
                            }
                        }
                        int score = (testsPassed*100)/totalTests; // percentage a.k.a. score

                        // update puzzle state
                        if (score == 0) {
                            messages.add(PUZZLE_MUTANT_DEAD_MESSAGE);
                            thisPuzzle.updateState(StoryState.IN_PROGRESS);
                        } else if (score > 0 && score < 100) {
                            messages.add(PUZZLE_MUTANT_HALF_MESSAGE);
                            thisPuzzle.updateState(StoryState.IN_PROGRESS);
                        } else if (score == 100){
                            messages.add(PUZZLE_MUTANT_ALIVE_MESSAGE);
                            thisPuzzle.updateState(StoryState.COMPLETED);
                        } else {
                            messages.add("Something went wrong"); // should never get here, really
                        }
                        thisPuzzle.updateScore(score); // update score
                        session.setAttribute("testsPassed", testsPassed);
                        session.setAttribute("score", score);
                        session.setAttribute("puzzle", thisPuzzle);
                        session.setAttribute("messages", messages);
                        res.sendRedirect("/results"); // redirect to ResultManager
                    } else {
                        logger.info("Mutant did not compile");
                        messages.add(MUTANT_UNCOMPILABLE_MESSAGE); // error messages
                        if (compileMutantTarget != null && compileMutantTarget.message != null && !compileMutantTarget.message.isEmpty()) {
                            messages.add(compileMutantTarget.message);
                        }
                        session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, mutantText);
                        res.sendRedirect(req.getHeader("referer"));
                    }
                } else { //invalid mutant
                    messages.add(MUTANT_INVALID_MESSAGE);
                    res.sendRedirect(req.getHeader("referer"));
                }
                break;

            // defender
            case "createTest":

                String testText = req.getParameter("test");

                // creates the test
                PuzzleTest newTest = createTest(thisPuzzle.getPuzzleId(), thisPuzzle.getClassId(), testText, uid, thisPuzzle.getPCUT());

                if (newTest != null) {
                    TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForPTest(newTest, TargetExecution.Target.COMPILE_TEST);
                    if (compileTestTarget != null && compileTestTarget.status.equals("SUCCESS")) {
                        logger.info("Testing original Target");
                        List<PuzzleMutant> mutants = DatabaseAccess.getMutantsforPuzzle(thisPuzzle.getPuzzleId());
                        int mutantsKilled = 0;
                        int totalMutants = 0;
                        for (PuzzleMutant mutant : mutants) {
                            totalMutants ++;
                            if (!AntRunner.testKillsMutantStory(mutant, newTest)) {
                                mutantsKilled++;
                            }
                        }
                        int score = (mutantsKilled*100)/totalMutants; // points user will receive
                        // update puzzle state accordingly
                        if (score == 0) {
                            messages.add(PUZZLE_TEST_FAIL_MESSAGE);
                            thisPuzzle.updateState(StoryState.IN_PROGRESS);
                        } else if (score > 0 && score < 100) {
                            messages.add(PUZZLE_TEST_HALF_MESSAGE);
                            thisPuzzle.updateState(StoryState.IN_PROGRESS);
                        } else if (score == 100) {
                            messages.add(PUZZLE_TEST_SUCCESS_MESSAGE);
                            thisPuzzle.updateState(StoryState.COMPLETED);
                        }
                        thisPuzzle.updateScore(score);
                        Object numberMutants = session.getAttribute("numberMutants");
                        session.setAttribute("mutantsKilled", numberMutants);
                        session.setAttribute("score", score);
                        session.setAttribute("puzzles", thisPuzzle);
                        session.setAttribute("messages", messages);
                        res.sendRedirect("/results");
                    } else { // fail test compilation
                        messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
                        messages.add(compileTestTarget.message);
                        session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
                        res.sendRedirect(req.getHeader("referer"));
                    }

                } else { // invalid test
                    messages.add(TEST_INVALID_MESSAGE);
                    session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
                    res.sendRedirect("puzzles");
                    return;
                }
                break;
        }


    }

    public static PuzzleMutant createMutant(int pid, int cid, String mutatedCode, int userId) throws IOException {

        StoryClass classMutated = DatabaseAccess.getStoryForKey(cid); // gets class details from Class ID
        String classMutatedBaseName = classMutated.getBaseName(); // gets the name

        File sourceFile = new File(classMutated.getJavaFile()); // get directory
        String sourceCode = new String(Files.readAllBytes(sourceFile.toPath())); // read in the code

        if (!CodeValidator.validMutant(sourceCode, mutatedCode)) {
            return null;
        }

        String md5CUT = CodeValidator.getMD5(sourceCode);
        String md5Mutant = CodeValidator.getMD5(mutatedCode);
        if (md5CUT.equals(md5Mutant)) {
            return null;
        }

        File mutantDir = new File(Constants.PUZZLE_MUTANTS_DIR + F_SEP + classMutatedBaseName + F_SEP + userId);
        File targetFile = new File(mutantDir + F_SEP + classMutatedBaseName + JAVA_SOURCE_EXT);
        String mutantFileDir = PUZZLE_MUTANTS_DIR + F_SEP + classMutatedBaseName + F_SEP + userId + F_SEP + classMutatedBaseName;

        FileUtils.writeStringToFile(targetFile, mutatedCode); // write user-submitted mutant to file
        PuzzleMutant compileMutant = AntRunner.compilePMutant(mutantDir, mutantFileDir + JAVA_SOURCE_EXT, pid, classMutated, userId);

        if (compileMutant != null) {

            logger.info("NewMutantDir: {}", mutantDir.getAbsolutePath());
            logger.info("Class Mutated: {} (basename: {})", classMutated.getAlias(), classMutatedBaseName);

            ClassPool classPool = ClassPool.getDefault();
            classPool.makeClass(new FileInputStream(new File(mutantFileDir + JAVA_CLASS_EXT)));

            String mutantFileName = mutantDir + F_SEP + classMutatedBaseName + JAVA_SOURCE_EXT;
            File mutantFile = new File(mutantFileName);
            FileWriter fw = new FileWriter(mutantFile);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(mutatedCode);
            bw.close();

            String md5FromMutantFile = CodeValidator.getMD5FromFile(mutantFileName);
            logger.info("md5CUT: {}\nmd5Mutant:{}\nmd5MutantFile: {}", md5CUT, md5Mutant, md5FromMutantFile);
            assert (md5Mutant.equals(md5FromMutantFile));

            return compileMutant;

        } else {

            return null;

        }

    }

    // trying same method as the mutant check
    public static PuzzleTest createTest(int pid, int cid, String testCode, int userId, StoryClass cut) throws IOException {

        StoryClass classTested = DatabaseAccess.getStoryForKey(cid);
        String classTestedBaseName = "Test" + classTested.getBaseName();

        File sourceFile = new File(classTested.getJavaFile());
        String sourceCode = new String(Files.readAllBytes(sourceFile.toPath()));

        File testDir = new File(PUZZLE_TESTS_DIR + F_SEP + classTestedBaseName + F_SEP + userId);
        File targetFile = new File(testDir + F_SEP + classTestedBaseName + JAVA_SOURCE_EXT);
        File compulsoryFile = new File(testDir + F_SEP + classTested.getBaseName() + JAVA_SOURCE_EXT); // original class to get object
        String testFileDir = PUZZLE_TESTS_DIR + F_SEP + classTestedBaseName + F_SEP + userId + F_SEP + classTestedBaseName;

        FileUtils.writeStringToFile(targetFile, testCode); // write user submitted test to testDir directory
        FileUtils.writeStringToFile(compulsoryFile, cut.getAsString()); // CUT (for compilation issues)
        PuzzleTest compileTest = AntRunner.compilePTest(testDir, testFileDir + JAVA_SOURCE_EXT, pid, classTested, userId);
        String compulFile = AntRunner.compilePCUT(cut);

        if (compileTest != null) {

            logger.info("NewTestDir: {}", testDir.getAbsolutePath());
            logger.info("Class Tested: {} (basename: {}", classTested.getAlias(), classTestedBaseName);

            ClassPool classPool = ClassPool.getDefault();
            classPool.makeClass(new FileInputStream(new File(compulFile)));

            File testFile = new File(testFileDir + JAVA_SOURCE_EXT);
            FileWriter fw = new FileWriter(testFile);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(testCode);
            bw.close();

            return compileTest;

        } else {

            return null;

        }

    }

}
