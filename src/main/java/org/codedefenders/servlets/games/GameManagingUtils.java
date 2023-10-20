/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.execution.BackendExecutorService;
import org.codedefenders.execution.ClassCompilerService;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.test.TestCompiledEvent;
import org.codedefenders.notification.events.server.test.TestTestedOriginalEvent;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.FileUtils;
import org.codedefenders.util.MutantUtils;
import org.codedefenders.util.URLUtils;
import org.codedefenders.validation.code.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import testsmell.TestFile;
import testsmell.TestSmellDetector;

import static org.codedefenders.util.Constants.DUMMY_ATTACKER_USER_ID;
import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;
import static org.codedefenders.util.Constants.JAVA_SOURCE_EXT;

/**
 * This class offers utility methods used by servlets managing active
 * games. Since each request handles a different submission this class has the
 * {@link RequestScoped} annotation.
 *
 * @see org.codedefenders.servlets.games.battleground.MultiplayerGameManager
 * @see org.codedefenders.servlets.games.puzzle.PuzzleGameManager
 */
// TODO Probably a better name for those class and interface would not harm..
@RequestScoped
public class GameManagingUtils implements IGameManagingUtils {
    private static final Logger logger = LoggerFactory.getLogger(GameManagingUtils.class);

    public static final Histogram automaticEquivalenceDuelTrigger = Histogram.build()
            .name("codedefenders_automaticEquivalenceDuelTrigger")
            .help("How long the execution of automaticEquivalenceDuelTrigger took")
            .unit("seconds")
            .labelNames("gameType")
            .register();

    public static final Counter automaticEquivalenceDuelsTriggered = Counter.build()
            .name("codedefenders_automaticEquivalenceDuels_mutants_flagged")
            .help("How many mutants where marked as equivalent through automatic equivalence duel validation")
            .labelNames("gameType")
            .register();

    @Inject
    private ClassCompilerService classCompiler;

    @Inject
    private BackendExecutorService backend;

    @Inject
    private TestSmellDetector testSmellDetector;

    @Inject
    private TestSmellsDAO testSmellsDAO;

    @Inject
    private INotificationService notificationService;

    @Inject
    private Configuration config;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mutant existingMutant(int gameId, String mutatedCode) {
        String md5Mutant = CodeValidator.getMD5FromText(mutatedCode);
        return MutantDAO.getMutantByGameAndMd5(gameId, md5Mutant);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAttackerPendingMutantsInGame(int gameId, int attackerId) {
        for (Mutant m : MutantDAO.getValidMutantsForGame(gameId)) {
            if (m.getPlayerId() == attackerId && m.getEquivalent() == Mutant.Equivalence.PENDING_TEST) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mutant createMutant(int gameId, int classId, String mutatedCode, int ownerUserId, String subDirectory)
            throws IOException {
        // Mutant is assumed valid here

        GameClass classMutated = GameClassDAO.getClassForId(classId);
        String classMutatedBaseName = classMutated.getBaseName();

        Path path = Paths.get(config.getMutantDir().getAbsolutePath(), subDirectory, String.valueOf(gameId), String.valueOf(ownerUserId));
        File newMutantDir = FileUtils.getNextSubDir(path);

        logger.debug("NewMutantDir: {}", newMutantDir.getAbsolutePath());
        logger.debug("Class Mutated: {} (basename: {})", classMutated.getName(), classMutatedBaseName);

        Path mutantFilePath = newMutantDir.toPath().resolve(classMutatedBaseName + JAVA_SOURCE_EXT);

        List<String> originalCode = FileUtils.readLines(Paths.get(classMutated.getJavaFile()));
        // Remove invalid diffs, like inserting blank lines

        String cleanedMutatedCode = new MutantUtils().cleanUpMutatedCode(String.join("\n", originalCode), mutatedCode);
        Files.write(mutantFilePath, cleanedMutatedCode.getBytes());

        // sanity check TODO Phil: Why tho?
        assert CodeValidator.getMD5FromText(cleanedMutatedCode).equals(CodeValidator.getMD5FromFile(
                mutantFilePath.toString())) : "MD5 hashes differ between code as text and code from new file";

        // Compile the mutant and add it to the game if possible; otherwise,
        // TODO: delete these files created?
        return classCompiler.compileMutant(newMutantDir, mutantFilePath.toString(), gameId, classMutated, ownerUserId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Test createTest(int gameId, int classId, String testText, int ownerUserId, String subDirectory)
            throws IOException {
        GameClass cut = GameClassDAO.getClassForId(classId);

        Path path = Paths.get(config.getTestsDir().getAbsolutePath(), subDirectory, String.valueOf(gameId), String.valueOf(ownerUserId), "original");
        File newTestDir = FileUtils.getNextSubDir(path);

        String javaFile = FileUtils.createJavaTestFile(newTestDir, cut.getBaseName(), testText);

        Test newTest = classCompiler.compileTest(newTestDir, javaFile, gameId, cut, ownerUserId);

        TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest,
                TargetExecution.Target.COMPILE_TEST);
        boolean compileSuccess = compileTestTarget.status == TargetExecution.Status.SUCCESS;

        TestCompiledEvent tce = new TestCompiledEvent();
        tce.setGameId(gameId);
        tce.setUserId(ownerUserId);
        tce.setTestId(newTest.getId());
        tce.setSuccess(compileSuccess);
        tce.setErrorMessage(compileSuccess ? null : compileTestTarget.message);
        notificationService.post(tce);

        // If the test did not compile we short circuit here. We shall not return null
        if (!compileSuccess) {
            return newTest;
        }

        // Eventually check the test actually passes when applied to the
        // original code.
        if (compileTestTarget.status == TargetExecution.Status.SUCCESS) {
            TargetExecution testOriginalTarget = backend.testOriginal(newTestDir, newTest);
            boolean testOriginalSuccess = testOriginalTarget.status == TargetExecution.Status.SUCCESS;

            TestTestedOriginalEvent ttoe = new TestTestedOriginalEvent();
            ttoe.setGameId(gameId);
            ttoe.setUserId(ownerUserId);
            ttoe.setTestId(newTest.getId());
            ttoe.setSuccess(testOriginalSuccess);
            ttoe.setErrorMessage(testOriginalSuccess ? null : compileTestTarget.message);
            notificationService.post(ttoe);

            detectTestSmells(newTest, cut);
        }

        return newTest;
    }

    // Enable testability. it can be declared also protected
    public void detectTestSmells(Test newTest, GameClass cut) {
        try {
            // Detect test smell
            TestFile testFile = new TestFile("", newTest.getJavaFile(), cut.getJavaFile());
            testSmellDetector.detectSmells(testFile);
            // TODO Post Process Smells. See #500
            testSmellsDAO.storeSmell(newTest, testFile);
        } catch (Exception e) {
            logger.error("Failed to generate or store test smell.", e);
        }
    }

    public boolean addPredefinedMutantsAndTests(AbstractGame game, boolean withMutants, boolean withTests) {
        List<Mutant> uploadedMutants = GameClassDAO.getMappedMutantsForClassId(game.getClassId());
        List<Test> uploadedTests = GameClassDAO.getMappedTestsForClassId(game.getClassId());
        int dummyAttackerPlayerId = PlayerDAO.getPlayerIdForUserAndGame(DUMMY_ATTACKER_USER_ID, game.getId());
        int dummyDefenderPlayerId = PlayerDAO.getPlayerIdForUserAndGame(DUMMY_DEFENDER_USER_ID, game.getId());
        int currentRound = GameDAO.getCurrentRound(game.getId());

        if (dummyAttackerPlayerId == -1) {
            logger.error("System attacker was not added to the game " + game.getId() + ".");
            return false;
        }
        if (dummyDefenderPlayerId == -1) {
            logger.error("System defender was not added to the game " + game.getId() + ".");
            return false;
        }

        /* Link original predefined mutants/tests to their copied counterparts. */
        Map<Integer, Mutant> mutantMap = new HashMap<>();
        Map<Integer, Test> testMap = new HashMap<>();

        /* Register predefined mutants. */
        if (withMutants) {
            // TODO: Validate uploaded mutants from the list
            for (Mutant mutant : uploadedMutants) {
                Mutant newMutant = new Mutant(game.getId(), game.getClassId(),
                        mutant.getJavaFile(),
                        mutant.getClassFile(),
                        true, // Alive be default
                        dummyAttackerPlayerId,
                        currentRound);
                newMutant.insert();
                mutantMap.put(mutant.getId(), newMutant);
            }
        }

        /* Register predefined tests. */
        if (withTests) {
            for (Test test : uploadedTests) {
                Test newTest = new Test(-1, game.getClassId(), game.getId(), test.getJavaFile(),
                        test.getClassFile(), 0, 0, dummyDefenderPlayerId, test.getLineCoverage().getLinesCovered(),
                        test.getLineCoverage().getLinesUncovered(), 0);
                newTest.insert();
                testMap.put(test.getId(), newTest);
            }
        }

        /* Kill predefined mutants that are dead from predefined tests.
           (Implementation from MultiplayerGameSelectionManager) */
        if (withMutants && withTests) {
            for (TargetExecution targetExecution : TargetExecutionDAO.getTargetExecutionsForUploadedWithClass(game.getClassId())) {
                Mutant mutant = mutantMap.get(targetExecution.mutantId);
                Test test = testMap.get(targetExecution.testId);

                targetExecution.testId = test.getId();
                targetExecution.mutantId = mutant.getId();

                if (targetExecution.status == TargetExecution.Status.FAIL) {
                    TestDAO.killMutant(test);
                    mutant.kill();
                    mutant.setKillMessage(targetExecution.message);
                    MutantDAO.updateMutantKillMessageForMutant(mutant);
                }

                targetExecution.insert();
            }
        }

        return true;

        /* Kill predefined mutants that are dead from predefined tests.
           (Alternative implementation from AdminCreateGames) */
        /*
        if (withMutants && withTests) {
            List<KillMap.KillMapEntry> killmap = KillmapDAO.getKillMapEntriesForClass(classId);
            // Filter the killmap and keep only the one created during the upload ...

            for (Mutant uploadedMutant : uploadedMutants) {
                boolean alive = true;
                for (Test uploadedTest : uploadedTests) {
                    // Does the test kill the mutant?
                    for (KillMap.KillMapEntry entry : killmap) {
                        if (entry.mutant.getId() == uploadedMutant.getId()
                                && entry.test.getId() == uploadedTest.getId()
                                && entry.status.equals(KillMap.KillMapEntry.Status.KILL)) {
                            // This also update the DB
                            if (mutantMap.get(uploadedMutant).isAlive()) {
                                testMap.get(uploadedTest).killMutant();
                                mutantMap.get(uploadedMutant).kill();
                            }
                            alive = false;
                            break;
                        }
                    }
                    if (!alive) {
                        break;
                    }
                }
            }
        }
        */
    }

    public boolean hasPredefinedMutants(AbstractGame game) {
        int dummyAttackerPlayerId = PlayerDAO.getPlayerIdForUserAndGame(DUMMY_ATTACKER_USER_ID, game.getId());
        return game.getMutants().stream()
                .anyMatch(mutant -> mutant.getPlayerId() == dummyAttackerPlayerId);
    }

    public boolean hasPredefinedTests(AbstractGame game) {
        int dummyDefenderPlayerId = PlayerDAO.getPlayerIdForUserAndGame(DUMMY_DEFENDER_USER_ID, game.getId());
        return game.getMutants().stream()
                .anyMatch(mutant -> mutant.getPlayerId() == dummyDefenderPlayerId);
    }

    /**
     * Returns the line numbers mentioned in the error message of the compiler.
     *
     * @param compilerOutput The compiler output.
     * @return A list of (1-indexed) line numbers.
     */
    public static List<Integer> extractErrorLines(String compilerOutput) {
        Set<Integer> errorLines = new TreeSet<>(); // Use TreeSet for the ordering
        Pattern p = Pattern.compile("\\[javac].*\\.java:([0-9]+): error:.*");
        for (String line : compilerOutput.split("\n")) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                // TODO may be not robust
                errorLines.add(Integer.parseInt(m.group(1)));
            }
        }
        return new ArrayList<>(errorLines);
    }

    /**
     * Add links that points to line for errors. Not sure that invoking a JS
     * function suing a link in this way is 100% safe ! XXX Consider to move the
     * decoration utility, and possibly the sanitize methods to some other
     * components.
     */
    public static String decorateWithLinksToCode(String compilerOutput, boolean forTest, boolean forMutant) {
        String editor = "";
        if (forTest) {
            editor = "testEditor";
        } else if (forMutant) {
            editor = "mutantEditor";
        }

        StringBuilder decorated = new StringBuilder();
        Pattern p = Pattern.compile("\\[javac].*\\.java:([0-9]+): error:.*");
        for (String line : compilerOutput.split("\n")) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                // Replace the entire line with a link to the source code
                String replacedLine = String.format(
                        "<a onclick=\"import('%s').then(module => module.objects.await('%s').then(editor => editor.jumpToLine(%s)));\" href=\"javascript:void(0);\">%s</a>",
                        CDIUtil.getBeanFromCDI(URLUtils.class).forPath("/js/codedefenders_main.mjs"),
                        editor, m.group(1), line);
                decorated.append(replacedLine).append("\n");
            } else {
                decorated.append(line).append("\n");
            }
        }
        return decorated.toString();
    }
}
