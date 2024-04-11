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

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.beans.game.ScoreboardBean;
import org.codedefenders.database.EventDAO;
import org.codedefenders.persistence.database.TestRepository;
import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.Achievement;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import static java.time.temporal.ChronoUnit.MINUTES;
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
    private final TestSmellsDAO testSmellsDAO;
    private final EventDAO eventDAO;
    private final INotificationService notificationService;
    private final GameRepository gameRepo;
    private final PuzzleRepository puzzleRepo;
    private final TestRepository testRepo;
    private final PlayerRepository playerRepo;
    private final AchievementEventHandler handler;
    private boolean isEventHandlerRegistered = false;
    private final Map<Integer, List<Achievement>> notificationQueue;

    /**
     * Constructor for the AchievementService.
     *
     * @param achievementRepository the repository for achievements
     * @param testSmellsDAO         the DAO for test smells
     * @param eventDAO              the DAO for events, only used for getting the number of tests written in the last
     *                              2 minutes, not for event handling.
     * @param notificationService   the notification service
     */
    @Inject
       public AchievementService(AchievementRepository achievementRepository, INotificationService notificationService,
                                 TestSmellsDAO testSmellsDAO, EventDAO eventDAO,
                                 GameRepository gameRepo, PuzzleRepository puzzleRepo,
                                 TestRepository testRepo, PlayerRepository playerRepo) {
        repo = achievementRepository;
        this.testSmellsDAO = testSmellsDAO;
        this.eventDAO = eventDAO;
        this.notificationService = notificationService;
        this.gameRepo = gameRepo;
        this.puzzleRepo = puzzleRepo;
        this.testRepo = testRepo;
        this.playerRepo = playerRepo;
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
     * Sends all queued achievement notifications as events via the notification service.
     */
    public void sendAchievementNotifications() {
        synchronized (notificationQueue) {
            notificationQueue.forEach((userId, achievements) -> {
                achievements.forEach(achievement -> {
                    logger.debug("Sending achievement notification for user with id {} and achievement {}",
                            userId, achievement.getId());
                    AchievementUnlockedEvent newEvent = new AchievementUnlockedEvent();
                    newEvent.setAchievement(achievement);
                    newEvent.setUserId(userId);
                    notificationService.post(newEvent);
                });
            });
        }
    }

    private Achievement.Id getGamePlayedAchievementIdForRole(Role role) {
        return switch (role) {
            case DEFENDER -> Achievement.Id.PLAY_AS_DEFENDER;
            case ATTACKER -> Achievement.Id.PLAY_AS_ATTACKER;
            case PLAYER -> Achievement.Id.PLAY_MELEE_GAMES;
            default -> throw new IllegalArgumentException();
        };
    }

    private void addGamePlayed(List<Player> players, Role role) {
        Achievement.Id achievementId = getGamePlayedAchievementIdForRole(role);
        players.stream()
                .map(Player::getUser)
                .mapToInt(UserEntity::getId)
                .forEach(userId -> {
                    updateAchievement(userId, Achievement.Id.PLAY_GAMES, 1);
                    updateAchievement(userId, achievementId, 1);
                });
    }

    private List<Achievement.Id> getMultiplayerGameResultAchievementIdForStatus(ScoreboardBean.PlayerStatus status) {
        List<Achievement.Id> ids = new ArrayList<>();
        switch (status) {
            case WINNING_ATTACKER -> {
                ids.add(Achievement.Id.WIN_GAMES_AS_ATTACKER);
                ids.add(Achievement.Id.WIN_GAMES);
            }
            case WINNING_DEFENDER -> {
                ids.add(Achievement.Id.WIN_GAMES_AS_DEFENDER);
                ids.add(Achievement.Id.WIN_GAMES);
            }
        }
        return ids;
    }

    private void addMultiplayerGameResult(int userId, ScoreboardBean.PlayerStatus status) {
        getMultiplayerGameResultAchievementIdForStatus(status)
                .forEach(achievementId -> updateAchievement(userId, achievementId, 1));
    }

    private void addTestWritten(int userId) {
        updateAchievement(userId, Achievement.Id.WRITE_TESTS, 1);
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
        setAchievementMax(userId, Achievement.Id.MAX_TESTS_IN_SHORT_TIME, testCount);
    }

    private void checkAmountOfRecentMutants(int userId, int gameId) {
        final int mutantCount =
                getAmountOfRecentEvents(userId, gameId, List.of(ATTACKER_MUTANT_CREATED, PLAYER_MUTANT_CREATED));
        setAchievementMax(userId, Achievement.Id.MAX_MUTANTS_IN_SHORT_TIME, mutantCount);
    }

    private void addMutantCreated(int userId) {
        updateAchievement(userId, Achievement.Id.CREATE_MUTANTS, 1);
    }

    private void checkTestSmells(int userId, int testId) {
        List<String> smells = testSmellsDAO.getDetectedTestSmellsForTest(testId);
        if (smells.isEmpty()) {
            updateAchievement(userId, Achievement.Id.WRITE_CLEAN_TESTS, 1);
        }
    }

    private void checkTestKills(int userId, int testId) {
        Set<Mutant> killedMutants = testRepo.getKilledMutantsForTestId(testId);
        int killCount = killedMutants.size();
        if (killCount > 0) {
            updateAchievement(userId, Achievement.Id.KILL_MUTANTS, killCount);
        }
    }

    private void checkMutantKilled(int mutantId) {
        Test test = testRepo.getKillingTestForMutantId(mutantId);
        if (test != null) {
            int playerId = test.getPlayerId();
            int userId = playerRepo.getPlayer(playerId).getUser().getId();
            updateAchievement(userId, Achievement.Id.KILL_MUTANTS, 1);
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

            updateAchievement(userId, Achievement.Id.TOTAL_COVERAGE, newLinesCovered);
            setAchievementMax(userId, Achievement.Id.MAX_COVERAGE, totalLinesCovered);
        }
    }

    private void equivalenceDuelWon(int userId) {
        updateAchievement(userId, Achievement.Id.WIN_EQUIVALENCE_DUELS, 1);
    }

    private void equivalenceDuelAttackerWon(int userId) {
        updateAchievement(userId, Achievement.Id.WIN_EQUIVALENCE_DUELS_AS_ATTACKER, 1);
    }

    private void equivalenceDuelDefenderWon(int userId) {
        updateAchievement(userId, Achievement.Id.WIN_EQUIVALENCE_DUELS_AS_DEFENDER, 1);
    }

    private void updateAchievement(int userId, Achievement.Id achievementId, int metricChange) {
        int affected = repo.updateAchievementForUser(userId, achievementId, metricChange);
        if (affected > 0) {
            logger.info("Updated achievement {} for user with id {}", achievementId.name(), userId);
            enqueueAchievementNotification(userId, achievementId);
        }
    }

    private void setAchievementMax(int userId, Achievement.Id achievementId, int newMetricAbsolute) {
        int affected = repo.setAchievementForUser(userId, achievementId, newMetricAbsolute);
        if (affected > 0) {
            logger.info("Updated achievement {} for user with id {}", achievementId.name(), userId);
            enqueueAchievementNotification(userId, achievementId);
        }
    }

    private void enqueueAchievementNotification(int userId, Achievement.Id achievementId) {
        Achievement achievement = repo.getAchievementForUser(userId, achievementId).orElseThrow();
        synchronized (notificationQueue) {
            notificationQueue.computeIfAbsent(userId, k -> new LinkedList<>()).add(achievement);
        }
        logger.debug("Achievement unlocked & added to queue: {} (Level {})", achievement.getName(),
                achievement.getLevel());
    }

    private void achievementNotificationSent(int userId, int achievementId) {
        synchronized (notificationQueue) {
            List<Achievement> achievements = notificationQueue.get(userId);
            if (achievements == null) {
                return;
            }

            achievements.removeIf(achievement -> achievement.getId().getAsInt() == achievementId);
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
            int userId = puzzleRepo.getPuzzleGameForId(event.getGameId()).getCreatorId();
            Achievement.Id achievementId = Achievement.Id.SOLVE_PUZZLES;
            if (repo.updateAchievementForUser(userId, achievementId, 1) > 0) {
                logger.info("Updated achievement {} for user with id {}", achievementId, userId);
                enqueueAchievementNotification(userId, achievementId);
            }
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
