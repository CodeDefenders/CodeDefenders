package org.codedefenders.servlets.games;

import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.execution.AntRunner;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.util.Constants;
import org.codedefenders.util.FileUtils;
import org.codedefenders.util.MutantUtils;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import testsmell.TestFile;
import testsmell.TestSmellDetector;

import static org.codedefenders.util.Constants.JAVA_SOURCE_EXT;
import static org.codedefenders.util.Constants.TESTS_DIR;

/**
 * This class offers static utility methods used by servlets managing active games.
 *
 * @see org.codedefenders.servlets.games.duel.DuelGameManager
 * @see org.codedefenders.servlets.games.battleground.MultiplayerGameManager
 * @see org.codedefenders.servlets.games.puzzle.PuzzleGameManager
 */
public class GameManagingUtils {
    private static final Logger logger = LoggerFactory.getLogger(GameManagingUtils.class);

    /**
     * Returns a {@link Mutant} instance if for a given game and mutated code an mutant exists already,
     * returns {@code null} otherwise.
     *
     * @param gameId      the identifier of the given game.
     * @param mutatedCode the mutated source code.
     * @return a {@link Mutant} for the game and mutated code, or if not found {@code null}.
     */
    public static Mutant existingMutant(int gameId, String mutatedCode) {
        String md5Mutant = CodeValidator.getMD5FromText(mutatedCode);

        // return the mutant in the game with same MD5 if it exists; return null otherwise
        return MutantDAO.getMutantByGameAndMd5(gameId, md5Mutant);
    }

    /**
     * Returns {@code true} a given player has mutants with unresolved equivalence
     * allegations in a game against him, {@code false} otherwise.
     *
     * @param gameId     the identifier of the given game.
     * @param attackerId the player identifier of the attacker.
     * @return {@code true} if a given player has pending equivalences, {@code false} otherwise.
     */
    public static boolean hasAttackerPendingMutantsInGame(int gameId, int attackerId) {
        for (Mutant m : MutantDAO.getValidMutantsForGame(gameId)) {
            if (m.getPlayerId() == attackerId && m.getEquivalent() == Mutant.Equivalence.PENDING_TEST) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stores a given mutant and calls {@link AntRunner#compileMutant(File, String, int, GameClass, int)
     * AntRunner#compileMutant()}.
     * <p>
     * <b>Here, the mutated code is assumed to be valid.</b>
     *
     * @param gameId       the identifier of the game the mutant is created in.
     * @param classId      the identifier of the class the mutant is mutated from.
     * @param mutatedCode  the source code of the mutant.
     * @param ownerUserId  the identifier of the user who created the mutant.
     * @param subDirectory the directory used for '/mutants/subDirectory/gameId/userId'
     * @return a {@link Mutant}, but never {@code null}.
     * @throws IOException when storing the mutant was not successful.
     */
    public static Mutant createMutant(int gameId, int classId, String mutatedCode, int ownerUserId, String subDirectory) throws IOException {
        // Mutant is assumed valid here

        GameClass classMutated = GameClassDAO.getClassForId(classId);
        String classMutatedBaseName = classMutated.getBaseName();

        Path path = Paths.get(Constants.MUTANTS_DIR, subDirectory, String.valueOf(gameId), String.valueOf(ownerUserId));
        File newMutantDir = FileUtils.getNextSubDir(path.toString());

        logger.debug("NewMutantDir: {}", newMutantDir.getAbsolutePath());
        logger.debug("Class Mutated: {} (basename: {})", classMutated.getName(), classMutatedBaseName);

        Path mutantFilePath = newMutantDir.toPath().resolve(classMutatedBaseName + JAVA_SOURCE_EXT);

        List<String> originalCode = FileUtils.readLines(Paths.get(classMutated.getJavaFile()));
        // Remove invalid diffs, like inserting blank lines

        String cleanedMutatedCode = new MutantUtils().cleanUpMutatedCode(String.join("\n", originalCode), mutatedCode);
        Files.write(mutantFilePath, cleanedMutatedCode.getBytes());

        // sanity check TODO Phil: Why tho?
        assert CodeValidator.getMD5FromText(cleanedMutatedCode).equals(CodeValidator.getMD5FromFile(mutantFilePath.toString()))
                : "MD5 hashes differ between code as text and code from new file";

        // Compile the mutant and add it to the game if possible; otherwise, TODO: delete these files created?
        return AntRunner.compileMutant(newMutantDir, mutantFilePath.toString(), gameId, classMutated, ownerUserId);
    }

    /**
     * Calls {@link #createTest(int, int, String, int, String, int)},but with the default number
     * of assertions specified in {@link CodeValidator#DEFAULT_NB_ASSERTIONS}.
     */
    public static Test createTest(int gameId, int classId, String testText, int ownerUserId, String subDirectory) throws IOException, CodeValidatorException {
        return createTest(gameId, classId, testText, ownerUserId, subDirectory, CodeValidator.DEFAULT_NB_ASSERTIONS);
    }


    /**
     * Stores and validates a given test.
     * Calls {@link AntRunner#compileTest(File, String, int, GameClass, int)} AntRunner#compileTest()} after.
     *
     * @param gameId       the identifier of the game the test is created in.
     * @param classId      the identifier of the class the test is created for.
     * @param testText the source code of the test.
     * @param ownerUserId  the identifier of the user who created the mutant.
     * @param subDirectory the directory used for '/tests/subDirectory/gameId/userId/original'
     * @return a {@link Test} is valid, {@code null} otherwise.
     * @throws IOException when storing the test was not successful.
     * @throws CodeValidatorException when code validation resulted in an error.
     */
    public static Test createTest(int gameId, int classId, String testText, int ownerUserId, String subDirectory, int maxNumberOfAssertions) throws IOException, CodeValidatorException {
        GameClass cut = GameClassDAO.getClassForId(classId);

        Path path = Paths.get(TESTS_DIR, subDirectory, String.valueOf(gameId), String.valueOf(ownerUserId), "original");
        File newTestDir = FileUtils.getNextSubDir(path.toString());

        String javaFile = FileUtils.createJavaTestFile(newTestDir, cut.getBaseName(), testText);

        Test newTest = AntRunner.compileTest(newTestDir, javaFile, gameId, cut, ownerUserId);

        TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest,
                TargetExecution.Target.COMPILE_TEST);

        // If the test did not compile we short circuit here. We shall not return null
        if (compileTestTarget == null || !compileTestTarget.status.equals(TargetExecution.Status.SUCCESS)) {
            return newTest;
        }

        // Validate code or short circuit here
        String testCode = new String(Files.readAllBytes(Paths.get(javaFile)));
        if (!CodeValidator.validateTestCode(testCode, maxNumberOfAssertions)) {
            return null;
        }

        // Eventually check the test actually passes when applied to the original code.
        if (compileTestTarget.status == TargetExecution.Status.SUCCESS) {
            AntRunner.testOriginal(newTestDir, newTest);
            try {
                // Detect test smell and store them to DB
                TestSmellDetector testSmellDetector = TestSmellDetector.createTestSmellDetector();
                TestFile testFile = new TestFile("", newTest.getJavaFile(), cut.getJavaFile());
                testSmellDetector.detectSmells(testFile);
                TestSmellsDAO.storeSmell(newTest, testFile);
            } catch (Exception e) {
                logger.error("Failed to generate or store test smell.", e);
            }
        }

        return newTest;
    }
}
