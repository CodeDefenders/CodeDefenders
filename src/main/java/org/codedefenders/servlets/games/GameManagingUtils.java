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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.execution.BackendExecutorService;
import org.codedefenders.execution.ClassCompilerService;
import org.codedefenders.execution.IMutationTester;
import org.codedefenders.execution.KillMap;
import org.codedefenders.execution.KillMap.KillMapEntry;
import org.codedefenders.execution.KillMapService;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Mutant.Equivalence;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.tcs.ITestCaseSelector;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.Player;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.equivalence.EquivalenceDuelAttackerWonEvent;
import org.codedefenders.notification.events.server.equivalence.EquivalenceDuelDefenderWonEvent;
import org.codedefenders.notification.events.server.equivalence.EquivalenceDuelWonEvent;
import org.codedefenders.notification.events.server.mutant.MutantCompiledEvent;
import org.codedefenders.notification.events.server.mutant.MutantDuplicateCheckedEvent;
import org.codedefenders.notification.events.server.mutant.MutantSubmittedEvent;
import org.codedefenders.notification.events.server.mutant.MutantTestedEvent;
import org.codedefenders.notification.events.server.mutant.MutantValidatedEvent;
import org.codedefenders.notification.events.server.test.TestCompiledEvent;
import org.codedefenders.notification.events.server.test.TestSubmittedEvent;
import org.codedefenders.notification.events.server.test.TestTestedMutantsEvent;
import org.codedefenders.notification.events.server.test.TestTestedOriginalEvent;
import org.codedefenders.notification.events.server.test.TestValidatedEvent;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.persistence.database.TestRepository;
import org.codedefenders.persistence.database.TestSmellRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.Constants;
import org.codedefenders.util.FileUtils;
import org.codedefenders.util.MutantUtils;
import org.codedefenders.util.URLUtils;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.codedefenders.validation.code.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import testsmell.TestFile;
import testsmell.TestSmellDetector;

import static org.codedefenders.execution.TargetExecution.Target.COMPILE_MUTANT;
import static org.codedefenders.execution.TargetExecution.Target.COMPILE_TEST;
import static org.codedefenders.execution.TargetExecution.Target.TEST_ORIGINAL;
import static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.FAILED_DUEL_VALIDATION_THRESHOLD;
import static org.codedefenders.util.Constants.DUMMY_ATTACKER_USER_ID;
import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;
import static org.codedefenders.util.Constants.JAVA_SOURCE_EXT;
import static org.codedefenders.util.Constants.MODE_BATTLEGROUND_DIR;

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

    private static final Histogram.Child isEquivalentMutantKillableValidation = Histogram.build()
            .name("codedefenders_isEquivalentMutantKillableValidation_duration")
            .help("How long the validation whether an as equivalent accepted mutant is killable took")
            .unit("seconds")
            // This can take rather long so add a 25.0 seconds bucket
            .buckets(new double[]{0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0, 25.0})
            .labelNames("gameType")
            .register()
            .labels("multiplayer");

    @Inject
    private ClassCompilerService classCompiler;

    @Inject
    private BackendExecutorService backend;

    @Inject
    private TestSmellDetector testSmellDetector;

    @Inject
    private TestSmellRepository testSmellRepo;

    @Inject
    private INotificationService notificationService;

    @Inject
    private Configuration config;

    @Inject
    private TestRepository testRepo;

    @Inject
    private MutantRepository mutantRepo;

    @Inject
    private GameRepository gameRepo;

    @Inject
    private PlayerRepository playerRepo;

    @Inject
    private GameClassRepository gameClassRepo;

    @Inject
    private UserRepository userRepo;

    @Inject
    private IMutationTester mutationTester;

    @Inject
    private ITestCaseSelector regressionTestCaseSelector;

    @Inject
    private UserService userService;

    @Inject
    private KillMapService killMapService;

    @Inject
    private EventDAO eventDAO;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mutant existingMutant(int gameId, String mutatedCode) {
        String md5Mutant = CodeValidator.getMD5FromText(mutatedCode);
        return mutantRepo.getMutantByGameAndMd5(gameId, md5Mutant);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAttackerPendingMutantsInGame(int gameId, int attackerId) {
        for (Mutant m : mutantRepo.getValidMutantsForGame(gameId)) {
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

        GameClass classMutated = gameClassRepo.getClassForId(classId)
                .orElseThrow();
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

    public enum CanUserSubmitMutantResult {
        USER_NOT_PART_OF_THE_GAME,
        USER_NOT_AN_ATTACKER,
        GAME_NOT_ACTIVE,
        USER_HAS_PENDING_EQUIVALENCE_DUELS,
        YES
    }

    public CanUserSubmitMutantResult canUserSubmitMutant(AbstractGame game, int userId,
                                                         boolean blockOnPendingEquivalences) {
        Player player = playerRepo.getPlayerForUserAndGame(userId, game.getId());
        if (player == null) {
            return CanUserSubmitMutantResult.USER_NOT_PART_OF_THE_GAME;
        }

        Role role = player.getRole();
        if (role != Role.ATTACKER) {
            return CanUserSubmitMutantResult.USER_NOT_AN_ATTACKER;
        }

        if (game.getState() != GameState.ACTIVE) {
            return CanUserSubmitMutantResult.GAME_NOT_ACTIVE;
        }

        // If the user has pending duels we cannot accept the mutant.
        if (blockOnPendingEquivalences && hasAttackerPendingMutantsInGame(game.getId(), player.getId())) {
            return CanUserSubmitMutantResult.USER_HAS_PENDING_EQUIVALENCE_DUELS;
        }

        return CanUserSubmitMutantResult.YES;
    }

    public record CreateBattlegroundMutantResult(
            boolean isSuccess,
            // on success
            Optional<Mutant> mutant,
            Optional<String> mutationTesterMessage,
            // on failure
            Optional<FailureReason> failureReason,
            Optional<ValidationMessage> validationErrorMessage,
            Optional<String> compilationError
    ) {
        public enum FailureReason {
            VALIDATION_FAILED,
            DUPLICATE_MUTANT_FOUND,
            COMPILATION_FAILED,
        }

        public static CreateBattlegroundMutantResult success(Mutant mutant, String mutationTesterMessage) {
            return new CreateBattlegroundMutantResult(
                    true,
                    Optional.of(mutant),
                    Optional.of(mutationTesterMessage),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty());
        }

        public static CreateBattlegroundMutantResult failure(
                FailureReason reason, ValidationMessage validationErrorMessage, String compilationError) {
            return new CreateBattlegroundMutantResult(false, Optional.empty(), Optional.empty(), Optional.of(reason),
                    Optional.ofNullable(validationErrorMessage), Optional.ofNullable(compilationError));
        }
    }

    public static class MutantCreationException extends Exception {
    }

    public CreateBattlegroundMutantResult createBattlegroundMutant(MultiplayerGame game, int userId, String code)
            throws IOException, MutantCreationException {
        MutantSubmittedEvent mse = new MutantSubmittedEvent();
        mse.setGameId(game.getId());
        mse.setUserId(userId);
        notificationService.post(mse);

        // Do the validation even before creating the mutant
        CodeValidatorLevel codeValidatorLevel = game.getMutantValidatorLevel();
        ValidationMessage validationMessage =
                CodeValidator.validateMutantGetMessage(game.getCUT().getSourceCode(), code, codeValidatorLevel);
        boolean validationSuccess = validationMessage == ValidationMessage.MUTANT_VALIDATION_SUCCESS;

        MutantValidatedEvent mve = new MutantValidatedEvent();
        mve.setGameId(game.getId());
        mve.setUserId(userId);
        mve.setSuccess(validationSuccess);
        notificationService.post(mve);

        if (!validationSuccess) {
            return CreateBattlegroundMutantResult.failure(
                    CreateBattlegroundMutantResult.FailureReason.VALIDATION_FAILED, validationMessage, null);
        }

        Mutant existingMutant = existingMutant(game.getId(), code);
        boolean duplicateCheckSuccess = existingMutant == null;

        MutantDuplicateCheckedEvent mdce = new MutantDuplicateCheckedEvent();
        mdce.setGameId(game.getId());
        mdce.setUserId(userId);
        mdce.setSuccess(duplicateCheckSuccess);
        mdce.setDuplicateId(duplicateCheckSuccess ? null : existingMutant.getId());
        notificationService.post(mdce);

        if (!duplicateCheckSuccess) {
            // Check if the duplicate mutant had a compilation error and reuse the error message if it has.
            String compilationError = null;
            TargetExecution existingMutantTarget =
                    TargetExecutionDAO.getTargetExecutionForMutant(existingMutant, COMPILE_MUTANT);
            if (existingMutantTarget != null && existingMutantTarget.status != TargetExecution.Status.SUCCESS
                    && existingMutantTarget.message != null && !existingMutantTarget.message.isEmpty()) {
                compilationError = existingMutantTarget.message;
            }

            return CreateBattlegroundMutantResult.failure(
                    CreateBattlegroundMutantResult.FailureReason.DUPLICATE_MUTANT_FOUND,
                    null, compilationError);
        }

        Mutant newMutant = createMutant(game.getId(), game.getClassId(), code, userId, MODE_BATTLEGROUND_DIR);
        if (newMutant == null) {
            throw new MutantCreationException();
        }

        TargetExecution compileMutantTarget = TargetExecutionDAO.getTargetExecutionForMutant(newMutant,
                COMPILE_MUTANT);
        boolean compileSuccess = compileMutantTarget != null
                && compileMutantTarget.status == TargetExecution.Status.SUCCESS;
        String errorMessage = (compileMutantTarget != null
                && compileMutantTarget.message != null
                && !compileMutantTarget.message.isEmpty())
                ? compileMutantTarget.message : null;

        MutantCompiledEvent mce = new MutantCompiledEvent();
        mce.setGameId(game.getId());
        mce.setUserId(userId);
        mce.setMutantId(newMutant.getId());
        mce.setSuccess(compileSuccess);
        mce.setErrorMessage(errorMessage);
        notificationService.post(mce);

        if (!compileSuccess) {
            return CreateBattlegroundMutantResult.failure(
                    CreateBattlegroundMutantResult.FailureReason.COMPILATION_FAILED, null, errorMessage);
        }

        var user = userService.getSimpleUserById(userId);
        final String notificationMsg = user.map(SimpleUser::getName)
                .orElse("User with the id " + userId) + " created a mutant.";
        Event notif = new Event(-1, game.getId(), userId, notificationMsg, EventType.ATTACKER_MUTANT_CREATED,
                EventStatus.GAME, new Timestamp(System.currentTimeMillis() - 1000));
        eventDAO.insert(notif);

        String mutationTesterMessage = mutationTester.runAllTestsOnMutant(game, newMutant);
        game.update();

        MutantTestedEvent mte = new MutantTestedEvent();
        mte.setGameId(game.getId());
        mte.setUserId(userId);
        mte.setMutantId(newMutant.getId());
        notificationService.post(mte);

        return CreateBattlegroundMutantResult.success(newMutant, mutationTesterMessage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Test createTest(int gameId, int classId, String testText, int ownerUserId, String subDirectory)
            throws IOException {
        GameClass cut = gameClassRepo.getClassForId(classId)
                .orElseThrow();

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

    public enum CanUserSubmitTestResult {
        USER_NOT_PART_OF_THE_GAME,
        USER_NOT_A_DEFENDER,
        GAME_NOT_ACTIVE,
        YES
    }

    public CanUserSubmitTestResult canUserSubmitTest(AbstractGame game, int userId) {
        Player player = playerRepo.getPlayerForUserAndGame(userId, game.getId());
        if (player == null) {
            return CanUserSubmitTestResult.USER_NOT_PART_OF_THE_GAME;
        }

        Role role = player.getRole();
        if (role != Role.DEFENDER) {
            return CanUserSubmitTestResult.USER_NOT_A_DEFENDER;
        }

        if (game.getState() != GameState.ACTIVE) {
            return CanUserSubmitTestResult.GAME_NOT_ACTIVE;
        }

        return CanUserSubmitTestResult.YES;
    }

    public record CreateBattlegroundTestResult(
            boolean isSuccess,
            // on success
            Optional<Test> test,
            Optional<String> mutationTesterMessage,
            // on failure
            Optional<FailureReason> failureReason,
            Optional<List<String>> validationErrorMessages,
            Optional<String> compilationError,
            Optional<String> testCutError
    ) {
        public enum FailureReason {
            VALIDATION_FAILED,
            COMPILATION_FAILED,
            TEST_DID_NOT_PASS_ON_CUT,
        }

        public static CreateBattlegroundTestResult success(Test test, String mutationTesterMessage) {
            return new CreateBattlegroundTestResult(
                    true,
                    Optional.of(test),
                    Optional.of(mutationTesterMessage),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty());
        }

        public static CreateBattlegroundTestResult failure(
                Test test, FailureReason reason, List<String> validationErrorMessages, String compilationError, String testCutError) {
            return new CreateBattlegroundTestResult(
                false,
                Optional.ofNullable(test),
                Optional.empty(),
                Optional.of(reason),
                Optional.ofNullable(validationErrorMessages),
                Optional.ofNullable(compilationError),
                Optional.ofNullable(testCutError)
            );
        }
    }

    public CreateBattlegroundTestResult createBattlegroundTest(MultiplayerGame game, int userId, String code)
            throws IOException {
        TestSubmittedEvent tse = new TestSubmittedEvent();
        tse.setGameId(game.getId());
        tse.setUserId(userId);
        notificationService.post(tse);

        // Do the validation even before creating the mutant
        List<String> validationMessage = CodeValidator.validateTestCodeGetMessage(
                code,
                game.getMaxAssertionsPerTest(),
                game.getCUT().getAssertionLibrary());
        boolean validationSuccess = validationMessage.isEmpty();

        TestValidatedEvent tve = new TestValidatedEvent();
        tve.setGameId(game.getId());
        tve.setUserId(userId);
        tve.setSuccess(validationSuccess);
        tve.setValidationMessage(validationSuccess ? null : String.join("\n", validationMessage));
        notificationService.post(tve);

        if (!validationSuccess) {
            return CreateBattlegroundTestResult.failure(
                    null, CreateBattlegroundTestResult.FailureReason.VALIDATION_FAILED,
                    validationMessage, null, null);
        }

        // From this point on we assume that test is valid according to the rules (but it might still not compile)
        Test newTest = createTest(game.getId(), game.getClassId(), code, userId, MODE_BATTLEGROUND_DIR);
        logger.debug("New Test {} by user {}", newTest.getId(), userId);
        TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, COMPILE_TEST);

        if (compileTestTarget.status != TargetExecution.Status.SUCCESS) {
            return CreateBattlegroundTestResult.failure(
                    newTest, CreateBattlegroundTestResult.FailureReason.COMPILATION_FAILED,
                    null, compileTestTarget.message, null
            );
        }

        TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TEST_ORIGINAL);
        if (testOriginalTarget.status != TargetExecution.Status.SUCCESS) {
            return CreateBattlegroundTestResult.failure(
                    newTest, CreateBattlegroundTestResult.FailureReason.TEST_DID_NOT_PASS_ON_CUT,
                    null, null, testOriginalTarget.message
            );
        }

        var user = userService.getSimpleUserById(userId);
        final String notificationMsg = user.map(SimpleUser::getName)
                .orElse("User with the id " + userId) + " created a mutant.";
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        final Event notif = new Event(-1, game.getId(), userId, notificationMsg, EventType.DEFENDER_TEST_CREATED,
                EventStatus.GAME, timestamp);
        eventDAO.insert(notif);


        String mutationTesterMessage = mutationTester.runTestOnAllMultiplayerMutants(game, newTest);
        game.update();
        logger.info("Successfully created test {} ", newTest.getId());

        TestTestedMutantsEvent ttme = new TestTestedMutantsEvent();
        ttme.setGameId(game.getId());
        ttme.setUserId(userId);
        ttme.setTestId(newTest.getId());
        notificationService.post(ttme);

        return CreateBattlegroundTestResult.success(newTest, mutationTesterMessage);
    }

    public enum CanUserResolveEquivalenceResult {
        USER_NOT_PART_OF_THE_GAME,
        USER_NOT_AN_ATTACKER,
        GAME_NOT_ACTIVE,
        MUTANT_DOES_NOT_EXIST,
        USER_DID_NOT_CREATE_MUTANT,
        MUTANT_IS_NOT_PENDING,
        YES
    }

    public CanUserResolveEquivalenceResult canUserResolveEquivalence(AbstractGame game, int userId, int equivMutantId) {
        Player player = playerRepo.getPlayerForUserAndGame(userId, game.getId());
        if (player == null) {
            return CanUserResolveEquivalenceResult.USER_NOT_PART_OF_THE_GAME;
        }

        Role role = player.getRole();
        if (role != Role.ATTACKER) {
            return CanUserResolveEquivalenceResult.USER_NOT_AN_ATTACKER;
        }

        if (game.getState() != GameState.ACTIVE) {
            return CanUserResolveEquivalenceResult.GAME_NOT_ACTIVE;
        }

        var equivMutant = mutantRepo.getMutantById(equivMutantId);
        if (equivMutant == null) {
            return CanUserResolveEquivalenceResult.MUTANT_DOES_NOT_EXIST;
        }
        if (equivMutant.getCreatorId() != userId) {
            return CanUserResolveEquivalenceResult.USER_DID_NOT_CREATE_MUTANT;
        }
        if (equivMutant.getEquivalent() != Equivalence.PENDING_TEST) {
            return CanUserResolveEquivalenceResult.MUTANT_IS_NOT_PENDING;
        }

        return CanUserResolveEquivalenceResult.YES;
    }

    public record AcceptBattlegroundEquivalenceResult(
        boolean mutantKillable
    ){}
    public enum ResolveBattlegroundEquivalenceAction {
        ACCEPT,
        REJECT
    }

    public AcceptBattlegroundEquivalenceResult acceptBattlegroundEquivalence(
            MultiplayerGame game, int userId, Mutant equivMutant) {
        SimpleUser user = userService.getSimpleUserById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User must exist."));

        // Here we check if the accepted equivalence is "possibly" equivalent
        boolean isMutantKillable = isMutantKillableByOtherTests(equivMutant);

        String message = Constants.MUTANT_ACCEPTED_EQUIVALENT_MESSAGE;
        String notification = String.format("%s accepts that their mutant %d is equivalent",
            user.getName(), equivMutant.getId());
        if (isMutantKillable) {
            logger.warn("Mutant {} was accepted as equivalence but it is killable", equivMutant.getId());
            notification = notification + " " + " However, the mutant was killable!";
        }

        // At this point we where not able to kill the mutant will all the covering
        // tests on the same class from different games
        mutantRepo.killMutant(equivMutant, Mutant.Equivalence.DECLARED_YES);

        int playerIdDefender = mutantRepo.getEquivalentDefenderId(equivMutant);
        playerRepo.increasePlayerPoints(1, playerIdDefender);

        // Notify the attacker
        Event notifEquiv = new Event(-1, game.getId(),
            userId,
            notification,
            EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.GAME,
            new Timestamp(System.currentTimeMillis()));
        eventDAO.insert(notifEquiv);

        EquivalenceDuelWonEvent edwe = new EquivalenceDuelDefenderWonEvent();
        edwe.setGameId(game.getId());
        userService.getSimpleUserByPlayerId(playerIdDefender).map(SimpleUser::getId)
            .ifPresent(edwe::setUserId);
        edwe.setMutantId(equivMutant.getId());
        notificationService.post(edwe);

        // Notify the defender which triggered the duel about it !
        if (isMutantKillable) {
            int defenderId = mutantRepo.getEquivalentDefenderId(equivMutant);
            Optional<Integer> defenderUserId = userRepo.getUserIdForPlayerId(defenderId);
            notification = user.getName() + " accepts that the mutant " + equivMutant.getId()
            + "that you claimed equivalent is equivalent, but that mutant was killable.";
            Event notifDefenderEquiv = new Event(-1, game.getId(), defenderUserId.orElse(0), notification,
                EventType.GAME_MESSAGE_DEFENDER, EventStatus.GAME,
                new Timestamp(System.currentTimeMillis()));
            eventDAO.insert(notifDefenderEquiv);
        }

        return new AcceptBattlegroundEquivalenceResult(isMutantKillable);
    }

    // TODO: Use submit-test result + extra info
    public record RejectBattlegroundEquivalenceResult(
            boolean testValid,
            // on valid test
            Optional<Test> test,
            Optional<Boolean> killedPendingMutant,
            Optional<Integer> numOtherPendingMutantsKilled,
            Optional<Boolean> isMutantKillable,
            // on invalid test
            Optional<FailureReason> failureReason,
            Optional<List<String>> validationErrorMessages,
            Optional<String> compilationError,
            Optional<String> testCutError
    ) {
        public enum FailureReason {
            VALIDATION_FAILED,
            COMPILATION_FAILED,
            TEST_DID_NOT_PASS_ON_CUT,
        }

        public static RejectBattlegroundEquivalenceResult testValid(Test test,
                boolean killedPendingMutant, int numOtherPendingMutantsKilled, Boolean isMutantKillable) {
            return new RejectBattlegroundEquivalenceResult(
                    true,
                    Optional.of(test),
                    Optional.of(killedPendingMutant),
                    Optional.of(numOtherPendingMutantsKilled),
                    Optional.of(isMutantKillable),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty());
        }

        public static RejectBattlegroundEquivalenceResult testInvalid(Test test, FailureReason reason,
            List<String> validationErrorMessages, String compilationError, String testCutError) {
            return new RejectBattlegroundEquivalenceResult(
                    false,
                    Optional.ofNullable(test),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(reason),
                    Optional.ofNullable(validationErrorMessages),
                    Optional.ofNullable(compilationError),
                    Optional.ofNullable(testCutError)
            );
        }
    }

    public RejectBattlegroundEquivalenceResult rejectBattlegroundEquivalence(
            MultiplayerGame game, int userId, Mutant equivMutant, String code) throws IOException {
        SimpleUser user = userService.getSimpleUserById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User must exist."));

        TestSubmittedEvent tse = new TestSubmittedEvent();
        tse.setGameId(game.getId());
        tse.setUserId(userId);
        notificationService.post(tse);

        List<String> validationMessage = CodeValidator.validateTestCodeGetMessage(
            code,
            game.getMaxAssertionsPerTest(),
            game.getCUT().getAssertionLibrary());
        boolean validationSuccess = validationMessage.isEmpty();

        TestValidatedEvent tve = new TestValidatedEvent();
        tve.setGameId(game.getId());
        tve.setUserId(userId);
        tve.setSuccess(validationSuccess);
        tve.setValidationMessage(validationSuccess ? null : String.join("\n", validationMessage));
        notificationService.post(tve);

        if (!validationSuccess) {
            return RejectBattlegroundEquivalenceResult.testInvalid(
                    null, RejectBattlegroundEquivalenceResult.FailureReason.VALIDATION_FAILED,
                    validationMessage, null, null);
        }

        Test newTest = createTest(game.getId(), game.getClassId(), code, userId, MODE_BATTLEGROUND_DIR);

        logger.debug("Executing Action resolveEquivalence for mutant {} and test {}", equivMutant.getId(), newTest.getId());
        TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, COMPILE_TEST);

        if (compileTestTarget.status != TargetExecution.Status.SUCCESS) {
            return RejectBattlegroundEquivalenceResult.testInvalid(
                    newTest, RejectBattlegroundEquivalenceResult.FailureReason.COMPILATION_FAILED,
                    null, compileTestTarget.message, null
            );
        }

        TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TEST_ORIGINAL);
        if (testOriginalTarget.status != TargetExecution.Status.SUCCESS) {
            return RejectBattlegroundEquivalenceResult.testInvalid(
                    newTest, RejectBattlegroundEquivalenceResult.FailureReason.TEST_DID_NOT_PASS_ON_CUT,
                    null, null, testOriginalTarget.message
            );
        }

        // Instead of running equivalence on only one mutant, let's try with all mutants pending resolution
        List<Mutant> mutantsPendingTests = game.getMutantsMarkedEquivalentPending();
        boolean killedClaimed = false;
        int killedOthers = 0;
        boolean isMutantKillable = false;
        String mutationTesterMessage;

        for (Mutant mutPending : mutantsPendingTests) {
            // TODO: (from MultiplayerGameManger) Doesnt distinguish between failing because the test didnt
            //  run at all and failing because it detected the mutant
            mutationTester.runEquivalenceTest(newTest, mutPending); // updates mutPending

            if (mutPending.getEquivalent() == Mutant.Equivalence.PROVEN_NO) {
                logger.debug("Test {} killed mutant {} and proved it non-equivalent",
                    newTest.getId(), mutPending.getId());
                final String message = user.getName() + " killed mutant "
                    + mutPending.getId() + " in an equivalence duel.";
                Event notif = new Event(-1, game.getId(), userId, message,
                    EventType.ATTACKER_MUTANT_KILLED_EQUIVALENT, EventStatus.GAME,
                    new Timestamp(System.currentTimeMillis())
                );
                eventDAO.insert(notif);

                EquivalenceDuelWonEvent edwe = new EquivalenceDuelAttackerWonEvent();
                edwe.setGameId(game.getId());
                edwe.setUserId(userId);
                edwe.setMutantId(mutPending.getId());
                notificationService.post(edwe);

                if (mutPending.getId() == equivMutant.getId()) {
                    killedClaimed = true;
                } else {
                    killedOthers++;
                }
            } else { // ASSUMED_YES
                if (mutPending.getId() == equivMutant.getId()) {
                    // Here we check if the accepted equivalence is "possibly" equivalent
                    isMutantKillable = isMutantKillableByOtherTests(mutPending);
                    String notification = user.getName()
                        + " lost an equivalence duel. Mutant " + mutPending.getId()
                        + " is assumed equivalent.";

                    if (isMutantKillable) {
                        notification = notification + " " + "However, the mutant was killable!";
                    }

                    // only kill the one mutant that was claimed
                    mutantRepo.killMutant(mutPending, Equivalence.ASSUMED_YES);

                    Event notif = new Event(-1, game.getId(), userId, notification,
                        EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.GAME,
                        new Timestamp(System.currentTimeMillis()));
                    eventDAO.insert(notif);
                }
                logger.debug("Test {} failed to kill mutant {}, hence mutant is assumed equivalent",
                    newTest.getId(), mutPending.getId());
            }
        }

        TestTestedMutantsEvent ttme = new TestTestedMutantsEvent();
        ttme.setGameId(game.getId());
        ttme.setUserId(userId);
        ttme.setTestId(newTest.getId());
        notificationService.post(ttme);

        testRepo.updateTest(newTest);
        game.update();
        return RejectBattlegroundEquivalenceResult.testValid(
            newTest,
            killedClaimed,
            killedOthers,
            isMutantKillable
        );
    }

    /**
     * Selects a max of AdminSystemSettings.SETTING_NAME.FAILED_DUEL_VALIDATION_THRESHOLD tests randomly sampled
     * which cover the mutant but belongs to other games and executes them against the mutant.
     *
     * @param mutantToValidate The mutant why try to find a killing test for
     * @return whether the mutant is killable or not/cannot be validated
     */
    public boolean isMutantKillableByOtherTests(Mutant mutantToValidate) {
        int validationThreshold = AdminDAO.getSystemSetting(FAILED_DUEL_VALIDATION_THRESHOLD).getIntValue();
        if (validationThreshold <= 0) {
            return false;
        }

        try (Histogram.Timer ignored = isEquivalentMutantKillableValidation.startTimer()) {
            // Get all the covering tests of this mutant which do not belong to this game
            int classId = mutantToValidate.getClassId();
            List<Test> tests = testRepo.getValidTestsForClass(classId);

            // Remove tests which belong to the same game as the mutant
            tests.removeIf(test -> test.getGameId() == mutantToValidate.getGameId());

            List<Test> selectedTests = regressionTestCaseSelector.select(tests, validationThreshold);
            logger.debug("Validating the mutant with {} selected tests:\n{}", selectedTests.size(), selectedTests);

            // At the moment this is purposely blocking.
            // This is the dumbest, but safest way to deal with it while we design a better solution.
            KillMap killmap = killMapService.forMutantValidation(selectedTests, mutantToValidate, classId);

            if (killmap == null) {
                // There was an error we cannot empirically prove the mutant was killable.
                logger.warn("An error prevents validation of mutant {}", mutantToValidate);
                return false;
            } else {
                for (KillMapEntry killMapEntry : killmap.getEntries()) {
                    if (killMapEntry.status.equals(KillMapEntry.Status.KILL)
                            || killMapEntry.status.equals(KillMapEntry.Status.ERROR)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public enum CanUserClaimEquivalenceResult {
        USER_NOT_PART_OF_THE_GAME,
        USER_NOT_A_DEFENDER,
        GAME_NOT_ACTIVE,
        YES
    }

    public CanUserClaimEquivalenceResult canUserClaimEquivalence(AbstractGame game, int userId) {
        Player player = playerRepo.getPlayerForUserAndGame(userId, game.getId());
        if (player == null) {
            return CanUserClaimEquivalenceResult.USER_NOT_PART_OF_THE_GAME;
        }

        Role role = player.getRole();
        if (role != Role.DEFENDER) {
            return CanUserClaimEquivalenceResult.USER_NOT_A_DEFENDER;
        }

        if (game.getState() != GameState.ACTIVE && game.getState() != GameState.GRACE_ONE) {
            return CanUserClaimEquivalenceResult.GAME_NOT_ACTIVE;
        }

        return CanUserClaimEquivalenceResult.YES;
    }

    public record ClaimEquivalentResult(
        List<Mutant> claimedMutants,
        List<String> messages
    ){}
    public ClaimEquivalentResult claimBattlegroundEquivalence(MultiplayerGame game, int userId, List<Integer> mutantLines) throws IOException {
        var user = userService.getSimpleUserById(userId).orElseThrow();
        int playerId = playerRepo.getPlayerIdForUserAndGame(userId, game.getId());

        List<Mutant> mutantsAlive = game.getAliveMutants();
        List<String> messages = new ArrayList<>();

        mutantLines = mutantLines.stream()
            .filter(game::isLineCovered)
            .toList();

        if (mutantLines.isEmpty()) {
            messages.add(Constants.MUTANT_CANT_BE_CLAIMED_EQUIVALENT_MESSAGE);
            return new ClaimEquivalentResult(List.of(), messages);
        }

        List<Mutant> claimedMutants = new ArrayList<>();
        for (int line : mutantLines) {
            mutantsAlive.stream()
                    .filter(m -> m.getLines().contains(line))
                    .filter(m -> m.getCreatorId() != Constants.DUMMY_ATTACKER_USER_ID)
                    .peek(m -> {
                        m.setEquivalent(Mutant.Equivalence.PENDING_TEST);
                        mutantRepo.updateMutant(m);

                        Optional<SimpleUser> mutantOwner = userService.getSimpleUserByPlayerId(m.getPlayerId());

                        Event event = new Event(-1, game.getId(), mutantOwner.map(SimpleUser::getId).orElse(0),
                                "One or more of your mutants is flagged equivalent.",
                                EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.NEW,
                                new Timestamp(System.currentTimeMillis()));
                        eventDAO.insert(event);

                        mutantRepo.insertEquivalence(m, playerId);
                    })
                    .forEach(claimedMutants::add);
        }

        if (claimedMutants.size() > 0) {
            String flaggingChatMessage = user.getName() + " flagged "
                    + claimedMutants.size() + " mutant" + (claimedMutants.size() == 1 ? "" : "s") + " as equivalent.";
            Event event = new Event(-1, game.getId(), userId, flaggingChatMessage,
                    EventType.DEFENDER_MUTANT_CLAIMED_EQUIVALENT, EventStatus.GAME,
                    new Timestamp(System.currentTimeMillis()));
            eventDAO.insert(event);
        }

        String flaggingMessage = claimedMutants.size() == 0
                ? "Mutant has already been claimed as equivalent or killed!"
                : String.format("Flagged %d mutant%s as equivalent", claimedMutants.size(),
                (claimedMutants.size() == 1 ? "" : 's'));
        messages.add(flaggingMessage);
        return new ClaimEquivalentResult(claimedMutants, messages);
    }

    // Enable testability. it can be declared also protected
    public void detectTestSmells(Test newTest, GameClass cut) {
        try {
            // Detect test smell
            TestFile testFile = new TestFile("", newTest.getJavaFile(), cut.getJavaFile());
            testSmellDetector.detectSmells(testFile);
            // TODO Post Process Smells. See #500
            testSmellRepo.storeSmell(newTest, testFile);
        } catch (Exception e) {
            logger.error("Failed to generate or store test smell.", e);
        }
    }

    public boolean addPredefinedMutantsAndTests(AbstractGame game, boolean withMutants, boolean withTests) {
        List<Mutant> uploadedMutants = gameClassRepo.getMappedMutantsForClassId(game.getClassId());
        List<Test> uploadedTests = gameClassRepo.getMappedTestsForClassId(game.getClassId());
        int dummyAttackerPlayerId = playerRepo.getPlayerIdForUserAndGame(DUMMY_ATTACKER_USER_ID, game.getId());
        int dummyDefenderPlayerId = playerRepo.getPlayerIdForUserAndGame(DUMMY_DEFENDER_USER_ID, game.getId());
        int currentRound = gameRepo.getCurrentRound(game.getId());

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

                int mutantId = mutantRepo.storeMutant(newMutant);
                newMutant.setId(mutantId);

                mutantMap.put(mutant.getId(), newMutant);
            }
        }

        /* Register predefined tests. */
        if (withTests) {
            for (Test test : uploadedTests) {
                Test newTest = new Test(-1, game.getClassId(), game.getId(), test.getJavaFile(),
                        test.getClassFile(), 0, 0, dummyDefenderPlayerId, test.getLineCoverage().getLinesCovered(),
                        test.getLineCoverage().getLinesUncovered(), 0);

                int testId = testRepo.storeTest(newTest);
                newTest.setId(testId);

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
                    testRepo.incrementMutantsKilled(test);
                    mutantRepo.killMutant(mutant, mutant.getEquivalent());
                    mutant.setKillMessage(targetExecution.message);
                    mutantRepo.updateMutantKillMessageForMutant(mutant);
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
        int dummyAttackerPlayerId = playerRepo.getPlayerIdForUserAndGame(DUMMY_ATTACKER_USER_ID, game.getId());
        return game.getMutants().stream()
                .anyMatch(mutant -> mutant.getPlayerId() == dummyAttackerPlayerId);
    }

    public boolean hasPredefinedTests(AbstractGame game) {
        int dummyDefenderPlayerId = playerRepo.getPlayerIdForUserAndGame(DUMMY_DEFENDER_USER_ID, game.getId());
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

    public Optional<String> getTestSmellsMessage(Test test) {
        List<String> detectedTestSmells = testSmellRepo.getDetectedTestSmellsForTest(test.getId());
        if (!detectedTestSmells.isEmpty()) {
            if (detectedTestSmells.size() == 1) {
                return Optional.of("Your test has the following smell: " + detectedTestSmells.get(0));
            } else {
                String join = String.join(", ", detectedTestSmells);
                return Optional.of("Your test has the following smells: " + join);
            }
        }
        return Optional.empty();
    }
}
