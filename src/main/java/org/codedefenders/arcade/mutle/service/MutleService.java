/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
package org.codedefenders.arcade.mutle.service;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.arcade.mutle.fixture.MutleClass;
import org.codedefenders.arcade.mutle.fixture.MutleEntry;
import org.codedefenders.arcade.mutle.fixture.MutleMutant;
import org.codedefenders.arcade.mutle.fixture.MutleTest;
import org.codedefenders.arcade.mutle.model.MutleFixtureInstallation;
import org.codedefenders.arcade.mutle.model.MutleGame;
import org.codedefenders.arcade.mutle.persistence.MutleRepository;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.execution.IMutationTester;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.persistence.database.PuzzleRepository;
import org.codedefenders.service.AchievementService;
import org.codedefenders.service.I18nService;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.transaction.Transaction;
import org.codedefenders.transaction.TransactionManager;
import org.codedefenders.transaction.TransactionalSupplier;
import org.codedefenders.util.FileUtils;
import org.codedefenders.validation.code.TestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;

import com.google.common.util.concurrent.Striped;

import static org.codedefenders.arcade.mutle.model.MutleGame.SUBMISSION_TIMEOUT;
import static org.codedefenders.execution.TargetExecution.Target.COMPILE_TEST;
import static org.codedefenders.execution.TargetExecution.Target.TEST_MUTANT;
import static org.codedefenders.execution.TargetExecution.Target.TEST_ORIGINAL;
import static org.codedefenders.util.Constants.DUMMY_ATTACKER_USER_ID;
import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;
import static org.codedefenders.util.Constants.MUTLE_GUEST_USER_ID;
import static org.codedefenders.util.Constants.TEST_DID_NOT_COMPILE_MESSAGE;
import static org.codedefenders.util.Constants.TEST_DID_NOT_PASS_ON_CUT_MESSAGE;

@ApplicationScoped
public class MutleService {
    private static final Logger logger = LoggerFactory.getLogger(MutleService.class);
    public static final int MAX_GUESSES = 6;
    private static final Striped<Lock> SUBMISSION_LOCKS = Striped.lock(256);
    private static final Pattern COMPILER_ERROR = Pattern.compile(
            "(?m)^\\[javac]\\s+.*\\.java:(\\d+): error: (.+)$");

    private final MutleRepository mutleRepository;
    private final MutleCatalog catalog;
    private final PuzzleRepository puzzleRepository;
    private final GameManagingUtils gameManagingUtils;
    private final GameService gameService;
    private final IMutationTester mutationTester;
    private final TestValidator testValidator;
    private final MutleFixtureInstaller installer;
    private final Clock clock;
    private final AchievementService achievementService;
    private final I18nService i18nService;
    private final TransactionManager transactionManager;

    @Inject
    public MutleService(MutleRepository mutleRepository, MutleCatalog catalog,
                        PuzzleRepository puzzleRepository, GameManagingUtils gameManagingUtils, GameService gameService,
                        IMutationTester mutationTester, TestValidator testValidator, MutleFixtureInstaller installer,
                        AchievementService achievementService, I18nService i18nService,
                        TransactionManager transactionManager) {
        this(mutleRepository, catalog, puzzleRepository, gameManagingUtils, gameService,
                mutationTester, testValidator, installer, achievementService, i18nService, transactionManager,
                Clock.systemUTC());
    }

    MutleService(MutleRepository mutleRepository, MutleCatalog catalog,
                 PuzzleRepository puzzleRepository, GameManagingUtils gameManagingUtils, GameService gameService,
                 IMutationTester mutationTester, TestValidator testValidator, MutleFixtureInstaller installer,
                 AchievementService achievementService, I18nService i18nService, TransactionManager transactionManager,
                 Clock clock) {
        this.mutleRepository = mutleRepository;
        this.catalog = catalog;
        this.puzzleRepository = puzzleRepository;
        this.gameManagingUtils = gameManagingUtils;
        this.gameService = gameService;
        this.mutationTester = mutationTester;
        this.testValidator = testValidator;
        this.installer = installer;
        this.achievementService = achievementService;
        this.i18nService = i18nService;
        this.transactionManager = transactionManager;
        this.clock = clock;
    }

    public synchronized MutleStateView start(int userId) {
        MutleEntry entry = currentEntry();
        long rotationPeriod = entry.series() - 1L;
        installer.ensureInstalled(entry);
        String fixtureKey = fixtureKey(entry);
        Optional<MutleGame> existing = mutleRepository.findGame(userId, rotationPeriod);
        if (existing.isPresent() && existing.get().fixtureKey().equals(fixtureKey)) {
            return state(userId, existing.get().gameId());
        }
        existing.ifPresent(game -> {
            logger.info("Retiring Mutle game {} because fixture {} is no longer available",
                    game.gameId(), game.fixtureKey());
            mutleRepository.retireGame(game.gameId(), userId);
        });
        MutleGame game = createGame(userId, fixtureKey, rotationPeriod, false);
        return state(userId, game.gameId());
    }

    public synchronized MutleStateView startPractice(int userId, String fixtureKey) {
        MutleEntry entry = entry(fixtureKey);
        installer.ensureInstalled(entry);
        return state(userId, createGame(userId, fixtureKey, 0, true).gameId());
    }

    /** Guest games have their own period slot and never contribute to account achievements. */
    synchronized MutleStateView startGuest(MutleFixtureView fixture) {
        MutleEntry entry = entry(fixture.fixtureKey());
        if (entry.series() != fixture.series()) {
            throw new NotFoundException();
        }
        installer.ensureInstalled(entry);
        return state(MUTLE_GUEST_USER_ID, createGame(MUTLE_GUEST_USER_ID, fixtureKey(entry), 0, true).gameId());
    }

    public List<MutleFixtureView> fixtures() {
        return catalog.allEntries().stream()
                .map(this::fixtureView)
                .toList();
    }

    /**
     * Returns the fixture selected for the current rotation without creating a game.
     */
    public MutleFixtureView currentFixture() {
        return catalog.currentEntry(clock.instant()).map(this::fixtureView).orElse(null);
    }

    /** Makes the fixture catalog available before the Mutle entry point is exposed. */
    public boolean isReady() {
        try {
            catalog.ensureSeeded();
            return true;
        } catch (RuntimeException exception) {
            logger.error("Mutle is unavailable because its fixture catalog is not ready", exception);
            return false;
        }
    }

    /**
     * Returns the user's current case, if they have started it already.
     * Visiting the overview must not create a game as a side effect.
     */
    public Optional<MutleStateView> current(int userId) {
        Optional<MutleEntry> currentEntry = catalog.currentEntry(clock.instant());
        if (currentEntry.isEmpty()) {
            return Optional.empty();
        }
        String fixtureKey = fixtureKey(currentEntry.get());
        return mutleRepository.findGame(userId, currentEntry.get().series() - 1L)
                .filter(game -> game.fixtureKey().equals(fixtureKey))
                .map(game -> state(userId, game.gameId()));
    }

    public MutleStateView state(int userId, Integer gameId) {
        MutleGame game = gameId == null
                ? mutleRepository.findGame(userId, currentRotationPeriod()).orElseThrow(() -> new NotFoundException())
                : ownedGame(userId, gameId);
        PuzzleGame puzzleGame = puzzleRepository.getPuzzleGameForId(game.gameId());
        if (puzzleGame == null) {
            throw new NotFoundException();
        }
        MutleEntry entry = entry(game.fixtureKey());
        MutleClass fixtureClass = catalog.fixtureClass(entry.classKey());
        MutleMutant metadata = mutant(entry);
        Mutant target = target(puzzleGame);
        boolean practice = game.rotationPeriod() == -game.gameId();
        boolean current = userId == MUTLE_GUEST_USER_ID
                ? catalog.currentEntry(clock.instant()).map(currentEntry -> currentEntry.series() == entry.series()
                        && fixtureKey(currentEntry).equals(game.fixtureKey())).orElse(false)
                : practice || game.rotationPeriod() == currentRotationPeriod();
        List<MutleGuessView> guesses = guesses(puzzleGame, metadata, target);
        boolean solved = puzzleGame.getState() == GameState.SOLVED || !target.isAlive();
        boolean failed = !solved && (puzzleGame.getState() == GameState.FAILED || guesses.size() >= MAX_GUESSES);
        boolean expired = !current && !solved && !failed;
        MutleRevealView reveal = solved || failed || expired
                ? new MutleRevealView(FileUtils.readJavaFileWithDefault(java.nio.file.Paths.get(target.getJavaFile())),
                        target.getPatchString(), metadata.revealExplanation(), proofSource(entry.classKey(), fixtureClass, metadata))
                : null;
        return new MutleStateView(game.gameId(), entry.series(), fixtureClass.title(),
                fixtureClass.specification(), catalog.readClassResource(entry.classKey(), fixtureClass.sourcePath()),
                guesses.size(),
                MAX_GUESSES - guesses.size(), expired, solved, failed, puzzleGame.getCUT().getTestTemplate(),
                guesses, reveal);
    }

    public MutleStateView submit(int userId, int gameId, String source) throws IOException {
        return submit(userId, gameId, source, i18nService.getI18n(userId));
    }

    public MutleStateView submit(int userId, int gameId, String source, I18n i18n) throws IOException {
        // Serialize live submissions for the same game; the database lease recovers abandoned ones.
        Lock submissionLock = SUBMISSION_LOCKS.get(gameId);
        submissionLock.lock();
        try {
            return submitLocked(userId, gameId, source, i18n);
        } finally {
            submissionLock.unlock();
        }
    }

    private MutleStateView submitLocked(int userId, int gameId, String source, I18n i18n) throws IOException {
        MutleGame game = ownedGame(userId, gameId);
        MutleStateView before = state(userId, gameId);
        if (before.expired() || before.solved() || before.failed()) {
            throw new IllegalStateException("Mutle game is read-only");
        }
        String token = UUID.randomUUID().toString();
        Instant now = clock.instant();
        if (!mutleRepository.reserveSubmission(gameId, userId, token, now, now.minus(SUBMISSION_TIMEOUT))) {
            throw new SubmissionInProgressException();
        }
        try {
            PuzzleGame puzzleGame = puzzleRepository.getPuzzleGameForId(gameId);
            var validation = testValidator.validateTestCode(source, puzzleGame.getMaxAssertionsPerTest(),
                    puzzleGame.getCUT().getAssertionLibrary());
            if (!validation.isValid()) {
                throw new RejectedSubmissionException(validation.getMessage(i18n));
            }
            Test test = gameManagingUtils.createTest(gameId, puzzleGame.getClassId(), source, userId, "mutle");
            TargetExecution compilation = TargetExecutionDAO.getTargetExecutionForTest(test, COMPILE_TEST);
            TargetExecution original = TargetExecutionDAO.getTargetExecutionForTest(test, TEST_ORIGINAL);
            if (compilation == null || original == null || compilation.status != TargetExecution.Status.SUCCESS
                    || original.status != TargetExecution.Status.SUCCESS) {
                throw new RejectedSubmissionException(rejectionMessage(compilation, original));
            }
            boolean killed = mutationTester.testVsMutant(test, target(puzzleGame));
            if (killed) {
                puzzleGame.setState(GameState.SOLVED);
            } else if (before.acceptedGuesses() + 1 >= MAX_GUESSES) {
                puzzleGame.setState(GameState.FAILED);
            } else {
                puzzleGame.incrementCurrentRound();
            }
            puzzleGame.update();
            if (killed && game.rotationPeriod() >= 0) {
                try {
                    achievementService.addMutleSolved(userId, before.acceptedGuesses() + 1);
                } catch (RuntimeException exception) {
                    logger.warn("Could not award Mutle achievements for user {} and game {}", userId, gameId, exception);
                }
            }
            return state(userId, gameId);
        } finally {
            mutleRepository.clearSubmission(gameId, userId, token);
        }
    }

    public List<MutleStateView> history(int userId) {
        return historyGames(mutleRepository.findGamesForUser(userId), catalog.allEntries(),
                currentRotationPeriod()).stream()
                .map(game -> state(userId, game.gameId()))
                .filter(state -> state.acceptedGuesses() > 0).toList();
    }

    static List<MutleGame> historyGames(List<MutleGame> games, List<MutleEntry> entries, long currentPeriod) {
        return games.stream()
                .filter(game -> game.rotationPeriod() >= 0)
                .filter(game -> game.rotationPeriod() != currentPeriod)
                .filter(game -> fixtureAvailable(game, entries))
                .toList();
    }

    static boolean fixtureAvailable(MutleGame game, List<MutleEntry> entries) {
        return entries.stream().anyMatch(entry -> fixtureKey(entry).equals(game.fixtureKey()));
    }

    private MutleGame createGame(int userId, String fixtureKey, long rotationPeriod, boolean practice) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return transactionManager.executeInTransaction((TransactionalSupplier<MutleGame>) transaction ->
                        createAndCommitGame(transaction, userId, fixtureKey, rotationPeriod, practice));
            } catch (RuntimeException exception) {
                if (!practice && isDuplicateKey(exception)) {
                    Optional<MutleGame> existing = mutleRepository.findGame(userId, rotationPeriod);
                    if (existing.isPresent()) {
                        return existing.get();
                    }
                    continue;
                }
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException("Could not create Mutle game", exception);
            }
        }
        throw new IllegalStateException("Could not create Mutle game after a concurrent creation");
    }

    private MutleGame createGameInTransaction(int userId, String fixtureKey, long rotationPeriod, boolean practice) {
        if (userId == MUTLE_GUEST_USER_ID) {
            mutleRepository.reserveGuestCapacity();
        }
        MutleFixtureInstallation installation = mutleRepository.findFixture(fixtureKey)
                .orElseThrow(() -> new IllegalStateException("Mutle fixture is not installed: " + fixtureKey));
        Puzzle puzzle = puzzleRepository.getPuzzleForId(installation.hiddenPuzzleId());
        if (puzzle == null) {
            throw new IllegalStateException("Mutle fixture has no hidden puzzle: " + fixtureKey);
        }
        PuzzleGame game = new PuzzleGame(puzzle, userId);
        if (!game.insert() || !game.addPlayer(DUMMY_ATTACKER_USER_ID, Role.ATTACKER)
                || !game.addPlayer(DUMMY_DEFENDER_USER_ID, Role.DEFENDER)) {
            throw new IllegalStateException("Could not prepare Mutle game");
        }
        gameManagingUtils.copyUploadedTemplateMutant(game, installation.templateMutantId());
        if (!game.addPlayer(userId, game.getActiveRole()) || !gameService.startGame(game)) {
            throw new IllegalStateException("Could not start Mutle game");
        }
        long storedRotationPeriod = practice ? -game.getId() : rotationPeriod;
        MutleGame mutleGame = new MutleGame(game.getId(), fixtureKey, storedRotationPeriod, userId, null, null,
                clock.instant());
        mutleRepository.insertGame(mutleGame);
        return mutleGame;
    }

    private MutleGame createAndCommitGame(Transaction transaction, int userId,
                                          String fixtureKey, long rotationPeriod, boolean practice) throws Exception {
        MutleGame game = createGameInTransaction(userId, fixtureKey, rotationPeriod, practice);
        transaction.commit();
        return game;
    }

    private static boolean isDuplicateKey(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof UncheckedSQLException sqlException && sqlException.getErrorCode() == 1062) {
                return true;
            }
        }
        return false;
    }

    private List<MutleGuessView> guesses(PuzzleGame game, MutleMutant metadata, Mutant target) {
        return game.getTests().stream().filter(test -> test.getRoundCreated() > 0 && accepted(test))
                .sorted(Comparator.comparingInt(Test::getRoundCreated).thenComparingInt(Test::getId))
                .map(test -> guess(game, metadata, target, test)).limit(MAX_GUESSES).toList();
    }

    static String rejectionMessage(TargetExecution compilation, TargetExecution original) {
        if (compilation == null) {
            return "The test could not be accepted.";
        }
        if (compilation.status != TargetExecution.Status.SUCCESS) {
            Matcher error = COMPILER_ERROR.matcher(compilation.message == null ? "" : compilation.message);
            if (error.find()) {
                return "Your test did not compile. Line " + error.group(1) + ": " + error.group(2).trim();
            }
            return TEST_DID_NOT_COMPILE_MESSAGE;
        }
        if (original == null) {
            return "The test could not be accepted.";
        }
        String details = testFailureDetails(original.message);
        return details.isEmpty()
                ? TEST_DID_NOT_PASS_ON_CUT_MESSAGE
                : TEST_DID_NOT_PASS_ON_CUT_MESSAGE + "\n\nDetails:\n" + details;
    }

    static String testFailureDetails(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String[] lines = message.replace("\r", "").split("\n");
        boolean hasJunitOutput = Arrays.stream(lines)
                .map(String::stripLeading)
                .anyMatch(line -> line.startsWith("[junit]"));
        StringBuilder details = new StringBuilder();
        int detailLines = 0;
        for (String rawLine : lines) {
            String line = rawLine.stripLeading();
            if (hasJunitOutput) {
                if (!line.startsWith("[junit]")) {
                    continue;
                }
                line = line.substring("[junit]".length()).stripLeading();
            }
            if (line.isBlank()
                    || line.startsWith("Testsuite:")
                    || line.startsWith("Tests run:")
                    || line.matches("Testcase: .* took .* sec")
                    || line.equals("Caused an ERROR")
                    || line.equals("FAILED")
                    || line.matches("Test .* FAILED")) {
                continue;
            }
            if (details.length() > 0) {
                details.append('\n');
            }
            details.append(line);
            detailLines++;
            if (detailLines == 12 || details.length() >= 1200) {
                break;
            }
        }
        if (details.length() > 1200) {
            return details.substring(0, 1200).stripTrailing() + "…";
        }
        return details.toString().stripTrailing();
    }

    static boolean accepted(Test test) {
        TargetExecution compilation = TargetExecutionDAO.getTargetExecutionForTest(test, COMPILE_TEST);
        TargetExecution original = TargetExecutionDAO.getTargetExecutionForTest(test, TEST_ORIGINAL);
        return compilation != null && original != null && compilation.status == TargetExecution.Status.SUCCESS
                && original.status == TargetExecution.Status.SUCCESS
                && TargetExecutionDAO.getTargetExecutionForTest(test, TEST_MUTANT) != null;
    }

    private MutleGuessView guess(PuzzleGame game, MutleMutant metadata, Mutant target, Test test) {
        Set<Integer> covered = Set.copyOf(test.getLineCoverage().getLinesCovered());
        boolean killed = !target.isAlive() && test.getMutantsKilled() > 0;
        return new MutleGuessView(test.getAsString(), covered.stream().sorted().toList(),
                MutleFeedback.classify(game.getCUT().getSourceCode(), metadata, covered, killed));
    }

    private Mutant target(PuzzleGame game) {
        return game.getMutants().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Mutle game has no target mutant"));
    }

    private MutleGame ownedGame(int userId, int gameId) {
        MutleGame game = mutleRepository.findGame(gameId).orElseThrow(NotFoundException::new);
        if (game.userId() != userId) {
            throw new NotFoundException();
        }
        return game;
    }

    private MutleEntry currentEntry() {
        return catalog.currentEntry(clock.instant()).orElseThrow(NotFoundException::new);
    }

    private long currentRotationPeriod() {
        return catalog.currentEntry(clock.instant()).map(entry -> entry.series() - 1L).orElse(-1L);
    }

    private MutleEntry entry(String fixtureKey) {
        return catalog.findEntry(fixtureKey).orElseThrow(NotFoundException::new);
    }

    private MutleFixtureView fixtureView(MutleEntry entry) {
        return new MutleFixtureView(fixtureKey(entry), entry.series(),
                catalog.fixtureClass(entry.classKey()).title());
    }

    private MutleMutant mutant(MutleEntry entry) {
        MutleClass fixtureClass = catalog.fixtureClass(entry.classKey());
        return fixtureClass.mutants().stream().filter(mutant -> mutant.key().equals(entry.mutantKey())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Mutle entry references no mutant"));
    }

    private String proofSource(String classKey, MutleClass fixtureClass, MutleMutant mutant) {
        return fixtureClass.tests().stream().filter(test -> test.key().equals(mutant.proofTestKey())).findFirst()
                .map(MutleTest::sourcePath).map(path -> catalog.readClassResource(classKey, path))
                .orElseThrow(() -> new IllegalStateException("Mutle mutant proof test is missing"));
    }

    public static String fixtureKey(MutleEntry entry) {
        return entry.classKey() + "/" + entry.mutantKey();
    }

    public static class NotFoundException extends RuntimeException {
    }

    public static class SubmissionInProgressException extends RuntimeException {
    }

    public static class RejectedSubmissionException extends RuntimeException {
        public RejectedSubmissionException(String message) {
            super(message);
        }
    }
}
