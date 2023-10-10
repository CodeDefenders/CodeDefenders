package org.codedefenders.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.beans.game.ScoreboardBean;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.PuzzleDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.Achievement;
import org.codedefenders.model.Player;
import org.codedefenders.model.UserEntity;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.achievement.AchievementUnlockedEvent;
import org.codedefenders.notification.events.server.achievement.ServerAchievementNotificationShownEvent;
import org.codedefenders.notification.events.server.game.GameSolvedEvent;
import org.codedefenders.notification.events.server.game.GameStoppedEvent;
import org.codedefenders.notification.events.server.mutant.MutantTestedEvent;
import org.codedefenders.notification.events.server.test.TestTestedMutantsEvent;
import org.codedefenders.persistence.database.AchievementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

@Named
@ApplicationScoped
public class AchievementService {

    private static final Logger logger = LoggerFactory.getLogger(AchievementService.class);
    private final AchievementRepository repo;
    private final INotificationService notificationService;
    private final AchievementEventHandler handler;
    private boolean isEventHandlerRegistered = false;
    private final Map<Integer, List<Achievement>> notificationQueue;

    @Inject
    public AchievementService(AchievementRepository achievementRepository, INotificationService notificationService) {
        repo = achievementRepository;
        this.notificationService = notificationService;
        handler = new AchievementEventHandler();
        notificationQueue = new HashMap<>();
    }

    public Collection<Achievement> getAchievementsForUser(int userId) {
        return repo.getAchievementsForUser(userId);
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
                    if (repo.updateAchievementForUser(userId, Achievement.Id.PLAY_GAMES, 1) > 0) {
                        logger.info("Updated achievement PLAY_GAMES for user with id {}", userId);
                        enqueueAchievementNotification(userId, Achievement.Id.PLAY_GAMES);
                    }

                    if (repo.updateAchievementForUser(userId, achievementId, 1) > 0) {
                        logger.info("Updated achievement {} for user with id {}", achievementId, userId);
                        enqueueAchievementNotification(userId, achievementId);
                    }
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
        getMultiplayerGameResultAchievementIdForStatus(status).forEach(achievementId -> {
                    if (repo.updateAchievementForUser(userId, achievementId, 1) > 0) {
                        logger.info("Updated achievement {} for user with id {}", achievementId, userId);
                        enqueueAchievementNotification(userId, achievementId);
                    }
                }
        );
    }

    private void addTestWritten(int userId) {
        int affected = repo.updateAchievementForUser(userId, Achievement.Id.WRITE_TESTS, 1);
        if (affected > 0) {
            logger.info("Updated achievement WRITE_TESTS for user with id {}", userId);
            enqueueAchievementNotification(userId, Achievement.Id.WRITE_TESTS);
        }
    }

    private void addMutantCreated(int userId) {
        int affected = repo.updateAchievementForUser(userId, Achievement.Id.CREATE_MUTANTS, 1);
        if (affected > 0) {
            logger.info("Updated achievement CREATE_MUTANTS for user with id {}", userId);
            enqueueAchievementNotification(userId, Achievement.Id.CREATE_MUTANTS);
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

    public void registerEventHandler() {
        if (isEventHandlerRegistered) {
            logger.warn("AchievementEventHandler is already registered");
            return;
        }

        notificationService.register(handler);
        isEventHandlerRegistered = true;
    }

    @PreDestroy
    public void unregisterEventHandler() {
        if (!isEventHandlerRegistered) {
            logger.warn("AchievementEventHandler is not registered");
            return;
        }

        notificationService.unregister(handler);
        isEventHandlerRegistered = false;
    }

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
                    role -> GameDAO.getPlayersForGame(event.getGameId(), role);

            List.of(Role.DEFENDER, Role.ATTACKER, Role.PLAYER).forEach(role -> {
                addGamePlayed(getPlayersWithRole.apply(role), role);
            });

            AbstractGame abstractGame = GameDAO.getGame(event.getGameId());
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
         * It is used to count the amount of solved puzzles.
         */
        @Subscribe
        @SuppressWarnings("unused")
        public void handlePuzzleGameSolvedEvent(GameSolvedEvent event) {
            int userId = PuzzleDAO.getPuzzleGameForId(event.getGameId()).getCreatorId();
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
        }

        /**
         * The {@link MutantTestedEvent} is the last event that is fired when a mutant is successfully submitted.
         * So we use this event to count the mutant for the achievements.
         */
        @Subscribe
        @SuppressWarnings("unused")
        public void handleMutantTestedEvent(MutantTestedEvent event) {
            addMutantCreated(event.getUserId());
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
