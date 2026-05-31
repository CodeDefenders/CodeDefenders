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
package org.codedefenders.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.codedefenders.beans.game.ScoreboardBean;
import org.codedefenders.database.EventDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.model.Achievement;
import org.codedefenders.model.AchievementType;
import org.codedefenders.model.EventType;
import org.codedefenders.model.Player;
import org.codedefenders.model.UserEntity;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.achievement.AchievementUnlockedEvent;
import org.codedefenders.notification.events.server.achievement.ServerAchievementNotificationShownEvent;
import org.codedefenders.notification.events.server.equivalence.EquivalenceDuelAttackerWonEvent;
import org.codedefenders.notification.events.server.equivalence.EquivalenceDuelDefenderWonEvent;
import org.codedefenders.notification.events.server.equivalence.EquivalenceDuelWonEvent;
import org.codedefenders.notification.events.server.game.GameSolvedEvent;
import org.codedefenders.notification.events.server.game.GameStoppedEvent;
import org.codedefenders.notification.events.server.mutant.MutantTestedEvent;
import org.codedefenders.notification.events.server.test.TestTestedMutantsEvent;
import org.codedefenders.persistence.database.AchievementRepository;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.persistence.database.PuzzleRepository;
import org.codedefenders.persistence.database.TestRepository;
import org.codedefenders.persistence.database.TestSmellRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;

import com.google.common.eventbus.Subscribe;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Locale.ENGLISH;
import static org.codedefenders.model.EventType.ATTACKER_MUTANT_CREATED;
import static org.codedefenders.model.EventType.DEFENDER_TEST_CREATED;
import static org.codedefenders.model.EventType.PLAYER_MUTANT_CREATED;
import static org.codedefenders.model.EventType.PLAYER_TEST_CREATED;

/**
 * Service for achievements. Handles updating the achievements for users.
 */
@Named
@ApplicationScoped
public class AchievementService {

    private static final Logger logger = LoggerFactory.getLogger(AchievementService.class);
    private final AchievementRepository repo;
    private final TestSmellRepository testSmellRepo;
    private final EventDAO eventDAO;
    private final INotificationService notificationService;
    private final GameRepository gameRepo;
    private final PuzzleRepository puzzleRepo;
    private final TestRepository testRepo;
    private final PlayerRepository playerRepo;
    private final AchievementEventHandler handler;
    private final I18nService i18nService;
    private boolean isEventHandlerRegistered = false;
    private final Map<Integer, List<Achievement>> notificationQueue;

    /**
     * Constructor for the AchievementService.
     *
     * @param achievementRepository the repository for achievements
     * @param testSmellRepo         the DAO for test smells
     * @param eventDAO              the DAO for events, only used for getting the number of tests written in the last
     *                              2 minutes, not for event handling.
     * @param notificationService   the notification service
     */
    @Inject
    public AchievementService(AchievementRepository achievementRepository, INotificationService notificationService,
                              TestSmellRepository testSmellRepo, EventDAO eventDAO, GameRepository gameRepo,
                              PuzzleRepository puzzleRepo, TestRepository testRepo, PlayerRepository playerRepo,
                              I18nService i18nService) {
        repo = achievementRepository;
        this.testSmellRepo = testSmellRepo;
        this.eventDAO = eventDAO;
        this.notificationService = notificationService;
        this.gameRepo = gameRepo;
        this.puzzleRepo = puzzleRepo;
        this.testRepo = testRepo;
        this.playerRepo = playerRepo;
        this.i18nService = i18nService;
        handler = new AchievementEventHandler();
        notificationQueue = new HashMap<>();
    }

    /**
     * Returns all achievements for a user.
     *
     * @param userId the id of the user
     * @return a collection of achievements
     */
    public Collection<Achievement> getAchievementsForUser(int userId) {
        return repo.getAchievementsForUser(userId);
    }

    /**
     * Translates and sends all queued achievement notifications as events via the notification service.
     */
    public void sendAchievementNotifications() {
        synchronized (notificationQueue) {
            notificationQueue.forEach((userId, achievements) -> {
                I18n i18n = i18nService.getI18n(userId);
                achievements.forEach(achievement -> {
                    logger.debug("Sending achievement notification for user with id {} and achievement {}",
                            userId, achievement.getType());

                    AchievementUnlockedEvent newEvent = new AchievementUnlockedEvent();
                    newEvent.setAchievement(achievement.translate(i18n));
                    newEvent.setUserId(userId);
                    notificationService.post(newEvent);
                });
            });
        }
    }

    private AchievementType getGamePlayedAchievementTypeForRole(Role role) {
        return switch (role) {
            case DEFENDER -> AchievementType.PLAY_AS_DEFENDER;
            case ATTACKER -> AchievementType.PLAY_AS_ATTACKER;
            case PLAYER -> AchievementType.PLAY_MELEE_GAMES;
            default -> throw new IllegalArgumentException();
        };
    }

    private void addGamePlayed(List<Player> players, Role role) {
        AchievementType achievementType = getGamePlayedAchievementTypeForRole(role);
        players.stream()
                .map(Player::getUser)
                .mapToInt(UserEntity::getId)
                .forEach(userId -> {
                    updateAchievement(userId, AchievementType.PLAY_GAMES, 1);
                    updateAchievement(userId, achievementType, 1);
                });
    }

    private List<AchievementType> getMultiplayerGameResultAchievementTypesForStatus(ScoreboardBean.PlayerStatus status) {
        List<AchievementType> types = new ArrayList<>();
        switch (status) {
            case WINNING_ATTACKER -> {
                types.add(AchievementType.WIN_GAMES_AS_ATTACKER);
                types.add(AchievementType.WIN_GAMES);
            }
            case WINNING_DEFENDER -> {
                types.add(AchievementType.WIN_GAMES_AS_DEFENDER);
                types.add(AchievementType.WIN_GAMES);
            }
        }
        return types;
    }

    private void addMultiplayerGameResult(int userId, ScoreboardBean.PlayerStatus status) {
        getMultiplayerGameResultAchievementTypesForStatus(status)
                .forEach(achievementId -> updateAchievement(userId, achievementId, 1));
    }

    private void addPuzzleSolved(int userId) {
        updateAchievement(userId, AchievementType.SOLVE_PUZZLES, 1);
    }

    private void addPuzzleSolvedInFewTries(int userId, int currentRound) {
        if (currentRound == 1) {
            updateAchievement(userId, AchievementType.PUZZLES_SOLVED_ON_FIRST_TRY, 1);
        }
    }

    private void addTestWritten(int userId) {
        updateAchievement(userId, AchievementType.WRITE_TESTS, 1);
    }

    private int getAmountOfRecentEvents(int userId, int gameId, List<EventType> events) {
        final long fiveMinAgo = Instant.now().minus(5, MINUTES).getEpochSecond();
        final long amount = eventDAO.getNewEventsForGameAndUser(gameId, fiveMinAgo, userId).stream()
                .filter(event -> events.contains(event.getEventType()))
                .count();
        return (int) amount;
    }

    private void checkAmountOfRecentTests(int userId, int gameId) {
        final int testCount =
                getAmountOfRecentEvents(userId, gameId, List.of(DEFENDER_TEST_CREATED, PLAYER_TEST_CREATED));
        setAchievementMax(userId, AchievementType.MAX_TESTS_IN_SHORT_TIME, testCount);
    }

    private void checkAmountOfRecentMutants(int userId, int gameId) {
        final int mutantCount =
                getAmountOfRecentEvents(userId, gameId, List.of(ATTACKER_MUTANT_CREATED, PLAYER_MUTANT_CREATED));
        setAchievementMax(userId, AchievementType.MAX_MUTANTS_IN_SHORT_TIME, mutantCount);
    }

    private void addMutantCreated(int userId) {
        updateAchievement(userId, AchievementType.CREATE_MUTANTS, 1);
    }

    private void checkTestSmells(int userId, int testId) {
        List<String> smells = testSmellRepo.getDetectedTestSmellsForTest(testId);
        if (smells.isEmpty()) {
            updateAchievement(userId, AchievementType.WRITE_CLEAN_TESTS, 1);
        }
    }

    private void checkTestKills(int userId, int testId) {
        Set<Mutant> killedMutants = testRepo.getKilledMutantsForTestId(testId);
        int killCount = killedMutants.size();
        if (killCount > 0) {
            updateAchievement(userId, AchievementType.KILL_MUTANTS, killCount);
        }
    }

    private void checkMutantKilled(int mutantId) {
        Test test = testRepo.getKillingTestForMutantId(mutantId);
        if (test != null) {
            int playerId = test.getPlayerId();
            Player player = playerRepo.getPlayer(playerId);
            if (player != null) { // Player can be null if a system player killed the mutant.
                int userId = player.getUser().getId();
                updateAchievement(userId, AchievementType.KILL_MUTANTS, 1);
            }
        }
    }

    private void checkCoverage(int userId, Integer testId) {
        Test test = testRepo.getTestById(testId);
        if (test != null) {
            Set<Integer> prevCoveredLines = testRepo.getTestsForGameAndUser(test.getGameId(), userId)
                    .stream()
                    .filter(t -> t.getId() != testId) // exclude the new test
                    .map(Test::getLineCoverage)
                    .map(LineCoverage::getLinesCovered)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());

            List<Integer> coveredLines = test.getLineCoverage().getLinesCovered();
            int totalLinesCovered = coveredLines.size();
            int newLinesCovered = coveredLines.stream()
                    .filter(line -> !prevCoveredLines.contains(line))
                    .mapToInt(l -> 1).sum();

            updateAchievement(userId, AchievementType.TOTAL_COVERAGE, newLinesCovered);
            setAchievementMax(userId, AchievementType.MAX_COVERAGE, totalLinesCovered);
        }
    }

    private void equivalenceDuelWon(int userId) {
        updateAchievement(userId, AchievementType.WIN_EQUIVALENCE_DUELS, 1);
    }

    private void equivalenceDuelAttackerWon(int userId) {
        updateAchievement(userId, AchievementType.WIN_EQUIVALENCE_DUELS_AS_ATTACKER, 1);
    }

    private void equivalenceDuelDefenderWon(int userId) {
        updateAchievement(userId, AchievementType.WIN_EQUIVALENCE_DUELS_AS_DEFENDER, 1);
    }

    private void updateAchievement(int userId, AchievementType achievementType, int metricChange) {
        int affected = repo.updateAchievementForUser(userId, achievementType, metricChange);
        if (affected > 0) {
            logger.info("Updated achievement {} for user with id {}", achievementType.name(), userId);
            enqueueAchievementNotification(userId, achievementType);
        }
    }

    private void setAchievementMax(int userId, AchievementType achievementType, int newMetricAbsolute) {
        int affected = repo.setAchievementForUser(userId, achievementType, newMetricAbsolute);
        if (affected > 0) {
            logger.info("Updated achievement {} for user with id {}", achievementType.name(), userId);
            enqueueAchievementNotification(userId, achievementType);
        }
    }

    private void enqueueAchievementNotification(int userId, AchievementType achievementType) {
        Achievement achievement = repo.getAchievementForUser(userId, achievementType).orElseThrow();
        synchronized (notificationQueue) {
            notificationQueue.computeIfAbsent(userId, k -> new LinkedList<>()).add(achievement);
        }
        logger.debug("Achievement unlocked & added to queue: {} (Level {})",
                achievement.getName(I18nService.getI18n(ENGLISH)), achievement.getLevel());
    }

    private void achievementNotificationSent(int userId, int achievementId) {
        synchronized (notificationQueue) {
            List<Achievement> achievements = notificationQueue.get(userId);
            if (achievements == null) {
                return;
            }

            achievements.removeIf(achievement -> achievement.getType().getId() == achievementId);
            if (achievements.isEmpty()) {
                notificationQueue.remove(userId);
            }
        }
    }

    /**
     * Registers the {@link AchievementEventHandler} to listen for events on the notification service.
     */
    public void registerEventHandler() {
        if (isEventHandlerRegistered) {
            logger.warn("AchievementEventHandler is already registered");
            return;
        }

        notificationService.register(handler);
        isEventHandlerRegistered = true;
    }

    /**
     * Unregisters the {@link AchievementEventHandler} from the notification service.
     */
    @PreDestroy
    public void unregisterEventHandler() {
        if (!isEventHandlerRegistered) {
            logger.warn("AchievementEventHandler is not registered");
            return;
        }

        notificationService.unregister(handler);
        isEventHandlerRegistered = false;
    }

    /**
     * The event handler is used to listen for events and then check & (if necessary) update
     * the achievements for the users.
     */
    public class AchievementEventHandler {

        /**
         * The {@link GameStoppedEvent} is fired when a game is finished. It is used to count the amount of games
         * someone has played in total, as attacker and defender. It is also used to determine how often someone has won
         * a game in total, as attacker and defender.
         */
        @Subscribe
        @SuppressWarnings("unused")
        public void handleGameStopped(GameStoppedEvent event) {
            Function<Role, List<Player>> getPlayersWithRole =
                    role -> gameRepo.getPlayersForGame(event.getGameId(), role);

            List.of(Role.DEFENDER, Role.ATTACKER, Role.PLAYER).forEach(role -> {
                addGamePlayed(getPlayersWithRole.apply(role), role);
            });

            AbstractGame abstractGame = gameRepo.getGame(event.getGameId());
            if (abstractGame instanceof MultiplayerGame game) {
                ScoreboardBean scoreboard = new ScoreboardBean();
                scoreboard.setGameId(event.getGameId());
                scoreboard.setScores(game.getMutantScores(), game.getTestScores());
                scoreboard.setPlayers(game.getAttackerPlayers(), game.getDefenderPlayers());

                Stream.of(Role.DEFENDER, Role.ATTACKER)
                        .map(getPlayersWithRole)
                        .flatMap(List::stream)
                        .collect(Collectors.toMap(
                                player -> player.getUser().getId(),
                                player -> scoreboard.getStatusForPlayer(player.getId())
                        )).forEach(AchievementService.this::addMultiplayerGameResult);
            } else if (abstractGame instanceof MeleeGame game) {
                // Achievement if a player has the most points in the game?
            }
        }

        /**
         * The {@link GameSolvedEvent} is fired when a puzzle is solved.
         * It is used to count the number of solved puzzles.
         */
        @Subscribe
        @SuppressWarnings("unused")
        public void handlePuzzleGameSolvedEvent(GameSolvedEvent event) {
            PuzzleGame game = puzzleRepo.getPuzzleGameForId(event.getGameId());
            int userId = puzzleRepo.getPuzzleGameForId(event.getGameId()).getCreatorId();

            addPuzzleSolved(userId);
            addPuzzleSolvedInFewTries(userId, game.getCurrentRound());
        }

        /**
         * The {@link TestTestedMutantsEvent} is the last event that is fired when a test is successfully submitted.
         * So we use this event to count the test for the achievements.
         */
        @Subscribe
        @SuppressWarnings("unused")
        public void handleTestTestedMutantsEvent(TestTestedMutantsEvent event) {
            addTestWritten(event.getUserId());
            checkTestSmells(event.getUserId(), event.getTestId());
            checkTestKills(event.getUserId(), event.getTestId());
            checkCoverage(event.getUserId(), event.getTestId());
            checkAmountOfRecentTests(event.getUserId(), event.getGameId());
        }

        /**
         * The {@link MutantTestedEvent} is the last event that is fired when a mutant is successfully submitted.
         * So we use this event to count the mutant for the achievements.
         */
        @Subscribe
        @SuppressWarnings("unused")
        public void handleMutantTestedEvent(MutantTestedEvent event) {
            addMutantCreated(event.getUserId());
            checkMutantKilled(event.getMutantId());
            checkAmountOfRecentMutants(event.getUserId(), event.getGameId());
        }

        /**
         * The {@link EquivalenceDuelWonEvent} is fired when a user (either attacker or defender)
         * has won an equivalence duel.
         * It is used to count the number of won equivalence duels.
         *
         * @param event the event
         */
        @Subscribe
        @SuppressWarnings("unused")
        public void handleEquivalenceDuelWonEvent(EquivalenceDuelWonEvent event) {
            equivalenceDuelWon(event.getUserId());
        }

        /**
         * The {@link EquivalenceDuelAttackerWonEvent} is fired when an attacker has won an equivalence duel.
         * It is used to count the number of won equivalence duels as attacker.
         *
         * @param event the event
         */
        @Subscribe
        @SuppressWarnings("unused")
        public void handleEquivalenceDuelAttackerWonEvent(EquivalenceDuelAttackerWonEvent event) {
            equivalenceDuelAttackerWon(event.getUserId());
        }

        /**
         * The {@link EquivalenceDuelDefenderWonEvent} is fired when a defender has won an equivalence duel.
         * It is used to count the number of won equivalence duels as defender.
         *
         * @param event the event
         */
        @Subscribe
        @SuppressWarnings("unused")
        public void handleEquivalenceDuelDefenderWonEvent(EquivalenceDuelDefenderWonEvent event) {
            equivalenceDuelDefenderWon(event.getUserId());
        }

        /**
         * This event signals that the client has received the achievement notification and shown it to the user.
         * We can therefore remove the achievement from the queue and prohibit it from being sent again.
         */
        @Subscribe
        @SuppressWarnings("unused")
        public void handleAchievementNotificationShownEvent(ServerAchievementNotificationShownEvent event) {
            achievementNotificationSent(event.getUserId(), event.getAchievementId());
        }
    }
}
