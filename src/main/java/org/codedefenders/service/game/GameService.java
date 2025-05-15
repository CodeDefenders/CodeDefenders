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
package org.codedefenders.service.game;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.TestDTO;
import org.codedefenders.execution.KillMap;
import org.codedefenders.execution.KillMapService;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.game.tcs.ITestCaseSelector;
import org.codedefenders.notification.events.server.game.GameResolvedAllDuelsEvent;
import org.codedefenders.notification.impl.NotificationService;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.persistence.database.TestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.client.Histogram;

import static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.FAILED_DUEL_VALIDATION_THRESHOLD;

@ApplicationScoped
public class GameService implements IGameService {

    protected TestRepository testRepo;
    protected MutantRepository mutantRepo;
    protected GameRepository gameRepo;

    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    private static final Histogram.Child isEquivalentMutantKillableValidation = Histogram.build()
            .name("codedefenders_isEquivalentMutantKillableValidation_duration")
            .help("How long the validation whether an as equivalent accepted mutant is killable took")
            .unit("seconds")
            // This can take rather long so add a 25.0-second bucket
            .buckets(new double[] {0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0,
                    25.0})
            .labelNames("gameType")
            .register()
            .labels("multiplayer");

    private final MultiplayerGameService multiplayerGameService;
    private final MeleeGameService meleeGameService;
    private final PuzzleGameService puzzleGameService;
    private final PlayerRepository playerRepo;
    private final ITestCaseSelector regressionTestCaseSelector;
    private final KillMapService killMapService;
    private final NotificationService notificationService;

    private final Set<Integer> gamesCurrentlyClosing = Collections.synchronizedSet(new HashSet<>());


    @Inject
    public GameService(MultiplayerGameService multiplayerGameService, MeleeGameService meleeGameService,
                       PuzzleGameService puzzleGameService, TestRepository testRepo, MutantRepository mutantRepo,
                       GameRepository gameRepo, PlayerRepository playerRepo, NotificationService notificationService,
                       ITestCaseSelector regressionTestCaseSelector, KillMapService killMapService) {
        this.multiplayerGameService = multiplayerGameService;
        this.meleeGameService = meleeGameService;
        this.puzzleGameService = puzzleGameService;
        this.testRepo = testRepo;
        this.mutantRepo = mutantRepo;
        this.gameRepo = gameRepo;
        this.playerRepo = playerRepo;
        this.regressionTestCaseSelector = regressionTestCaseSelector;
        this.killMapService = killMapService;
        this.notificationService = notificationService;
    }

    @Override
    public MutantDTO getMutant(int userId, int mutantId) {
        // I can't delegate this to the other services, as the game type is still unknown.
        Mutant mutant = mutantRepo.getMutantById(mutantId);
        if (mutant != null) {
            return getMutant(userId, mutant);
        } else {
            return null;
        }
    }

    @Override
    public MutantDTO getMutant(int userId, Mutant mutant) {
        IGameService gameService = getGameServiceForGameId(mutant.getGameId());
        if (gameService != null) {
            return gameService.getMutant(userId, mutant);
        } else {
            return null;
        }
    }

    @Override
    public List<MutantDTO> getMutants(int userId, int gameId) {
        IGameService gameService = getGameServiceForGameId(gameId);
        if (gameService != null) {
            return gameService.getMutants(userId, gameId);
        } else {
            return null;
        }
    }

    @Override
    public List<MutantDTO> getMutants(SimpleUser user, AbstractGame game) {
        IGameService gameService = getGameServiceForGame(game);
        if (gameService != null) {
            return gameService.getMutants(user, game);
        } else {
            return null;
        }
    }

    @Override
    public TestDTO getTest(int userId, int testId) {
        // I can't delegate this to the other services, as the game type is still unknown.
        Test test = testRepo.getTestById(testId);
        if (test != null) {
            return getTest(userId, test);
        } else {
            return null;
        }
    }

    @Override
    public TestDTO getTest(int userId, Test test) {
        IGameService gameService = getGameServiceForGameId(test.getGameId());
        if (gameService != null) {
            return gameService.getTest(userId, test);
        } else {
            return null;
        }
    }

    @Override
    public List<TestDTO> getTests(int userId, int gameId) {
        IGameService gameService = getGameServiceForGameId(gameId);
        if (gameService != null) {
            return gameService.getTests(userId, gameId);
        } else {
            return null;
        }
    }

    @Override
    public List<TestDTO> getTests(SimpleUser user, AbstractGame game) {
        IGameService gameService = getGameServiceForGame(game);
        if (gameService != null) {
            return gameService.getTests(user, game);
        } else {
            return null;
        }
    }

    @Override
    public boolean closeGame(AbstractGame game) {
        IGameService gameService = getGameServiceForGame(game);
        if (gameService != null) {
            boolean closed = gameService.closeGame(game);
            if (closed) {
                logger.info("Closed game with id {}", game.getId());
            } else {
                logger.warn("Failed to close game with id {}", game.getId());
            }
            return closed;
        }
        return false;
    }

    @Override
    public boolean startGame(AbstractGame game) {
        IGameService gameService = getGameServiceForGame(game);
        if (gameService != null) {
            return gameService.startGame(game);
        } else {
            return false;
        }
    }

    /**
     * Fetches and closes all expired multiplayer and melee games.
     *
     * @return the amount of games closed.
     */
    public int closeExpiredGames() {
        final List<AbstractGame> expiredGames = gameRepo.getExpiredGames();
        int closedGames = 0;

        for (AbstractGame game : expiredGames) {
            final boolean closed = closeGame(game);
            closedGames += closed ? 1 : 0;
        }
        return closedGames;
    }

    private IGameService getGameServiceForGameId(int gameId) {
        GameMode mode = gameRepo.getGameMode(gameId).orElseThrow();
        switch (mode) {
            case PARTY:
                return multiplayerGameService;
            case MELEE:
                return meleeGameService;
            case PUZZLE:
                return puzzleGameService;
            default:
                // TODO This might require some love, like an exception...
                return null;
        }
    }

    // TODO Honestly, this smells like some anti-pattern... the right service should
    // be instantiated by the CI directly
    private IGameService getGameServiceForGame(AbstractGame game) {
        if (game instanceof MultiplayerGame) {
            return multiplayerGameService;
        } else if (game instanceof MeleeGame) {
            return meleeGameService;
        } else if (game instanceof PuzzleGame) {
            return puzzleGameService;
        }
        // TODO This might require some love, like an exception...
        return null;
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
            logger.debug("Validation of mutant {} skipped due to threshold being 0", mutantToValidate.getId());
            return false;
        }

        try (Histogram.Timer ignored = isEquivalentMutantKillableValidation.startTimer()) {
            // Get all the covering tests of this mutant which do not belong to this game
            int classId = mutantToValidate.getClassId();
            List<Test> tests = testRepo.getValidTestsForClass(classId);

            // Remove tests which belong to the same game as the mutant
            tests.removeIf(test -> test.getGameId() == mutantToValidate.getGameId());
            // Remove tests which are not covering the mutant
            tests.removeIf(test -> !test.isMutantCovered(mutantToValidate));

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
                for (KillMap.KillMapEntry killMapEntry : killmap.getEntries()) {
                    if (killMapEntry.status.equals(KillMap.KillMapEntry.Status.KILL)
                            || killMapEntry.status.equals(KillMap.KillMapEntry.Status.ERROR)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }


    /**
     * Selects a max of AdminSystemSettings.SETTING_NAME.FAILED_DUEL_VALIDATION_THRESHOLD tests randomly sampled
     * which cover the mutant but belongs to other games and executes them against the mutant.
     *
     * @param mutantToValidate The mutant why try to find a killing test for
     * @return whether the mutant is killable or not/cannot be validated
     */
    public CompletableFuture<Boolean> isMutantKillableByOtherTestsAsync(Mutant mutantToValidate) {
        int validationThreshold = AdminDAO.getSystemSetting(FAILED_DUEL_VALIDATION_THRESHOLD).getIntValue();
        if (validationThreshold <= 0) {
            logger.debug("Validation of mutant {} skipped due to threshold being 0", mutantToValidate.getId());
            return CompletableFuture.completedFuture(false);
        }

        // Get all the covering tests of this mutant which do not belong to this game
        int classId = mutantToValidate.getClassId();
        List<Test> tests = testRepo.getValidTestsForClass(classId);

        // Remove tests which belong to the same game as the mutant
        tests.removeIf(test -> test.getGameId() == mutantToValidate.getGameId());
        // Remove tests which are not covering the mutant
        tests.removeIf(test -> !test.isMutantCovered(mutantToValidate));

        List<Test> selectedTests = regressionTestCaseSelector.select(tests, validationThreshold);
        logger.debug("Validating the mutant with {} selected tests:\n{}", selectedTests.size(), selectedTests);

        return killMapService.forMutantValidationAsync(selectedTests, mutantToValidate, classId)
                .thenCompose(killmap -> {
                    if (killmap == null) {
                        // There was an error we cannot empirically prove the mutant was killable.
                        logger.warn("An error prevents validation of mutant {}", mutantToValidate);
                        return CompletableFuture.completedFuture(false);
                    } else {
                        for (KillMap.KillMapEntry killMapEntry : killmap.getEntries()) {
                            if (killMapEntry.status.equals(KillMap.KillMapEntry.Status.KILL)
                                    || killMapEntry.status.equals(KillMap.KillMapEntry.Status.ERROR)) {
                                return CompletableFuture.completedFuture(true);
                            }
                        }
                    }
                    return CompletableFuture.completedFuture(false);
                });
    }


    /**
     * Resolves all open duels by checking if the mutants are killable by tests from other games using the same class.
     * If they are, they are marked as not equivalent (PROVEN_NO).
     * Otherwise, they are marked as equivalent (ASSUMED_YES).
     * <p>
     * This method is asynchronous and returns a CompletableFuture.
     * <p>
     * Resolution of the equivalence duels can only be triggered once per game,
     * all subsequent calls will have no effect.
     *
     * @param gameId The game ID for which to resolve open duels.
     */
    public CompletableFuture<Void> resolveAllOpenDuelsAsync(int gameId) {
        if (gamesCurrentlyClosing.add(gameId)) {
            logger.info("Game {} is being closed, resolving open duels.", gameId);
        } else {
            logger.debug("Game {} is already being closed, skipping resolution of open duels.", gameId);
            return CompletableFuture.completedFuture(null);
        }

        var game = gameRepo.getGame(gameId);
        var mutantsPendingTests = game.getMutantsMarkedEquivalentPending();
        var futures = mutantsPendingTests.stream().map(mutant -> {
            return isMutantKillableByOtherTestsAsync(mutant).thenAccept(isKillable -> {
                /*
                 * Points are handled by MultiplayerGame.java:456, a new equivalence status would be needed to change the scoring.
                 * As the scoring rules are already hard to grasp for new players, this would introduce additional complexity.
                 *
                 * Additionally, the attacker's possibility to gain points from the auto-resolved duel stops the defenders
                 * from claiming all mutants as equivalent before the game ends without any risk.
                 */
                if (isKillable) {
                    mutantRepo.killMutant(mutant, Mutant.Equivalence.PROVEN_NO);
                } else {
                    mutantRepo.killMutant(mutant, Mutant.Equivalence.ASSUMED_YES);
                    int playerIdDefender = mutantRepo.getEquivalentDefenderId(mutant);
                    playerRepo.increasePlayerPoints(1, playerIdDefender);
                }

                logger.info("Mutant {} was automatically resolved as {}.",
                        mutant.getId(), isKillable ? "not equivalent" : "equivalent");

                // TODO: create events like the EquivalenceDuelAttackerWonEvent?

            });
        }).toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures).thenRun(() -> {
            gamesCurrentlyClosing.remove(gameId);

            var gse = new GameResolvedAllDuelsEvent();
            gse.setGameId(gameId);
            notificationService.post(gse);
        });
    }
}
