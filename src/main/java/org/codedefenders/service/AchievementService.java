package org.codedefenders.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.database.GameDAO;
import org.codedefenders.game.Role;
import org.codedefenders.model.Achievement;
import org.codedefenders.model.Player;
import org.codedefenders.model.UserEntity;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.achievement.AchievementUnlockedEvent;
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
        notificationQueue.computeIfAbsent(userId, k -> new LinkedList<>()).add(achievement);
        logger.debug("Achievement unlocked & added to queue: {} (Level {})", achievement.getName(),
                achievement.getLevel());
    }

    public void sendAchievementNotifications() {
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

        @Subscribe
        @SuppressWarnings("unused")
        public void handleGameStopped(GameStoppedEvent event) {
            Arrays.asList(Role.DEFENDER, Role.ATTACKER, Role.PLAYER).forEach(role -> {
                List<Player> players = GameDAO.getPlayersForGame(event.getGameId(), role);
                addGamePlayed(players, role);
            });
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
    }
}
