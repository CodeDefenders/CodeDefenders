package org.codedefenders.service;

import java.util.Arrays;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.database.GameDAO;
import org.codedefenders.game.Role;
import org.codedefenders.model.Player;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.game.GameStartedEvent;
import org.codedefenders.notification.events.server.game.GameStoppedEvent;
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

    @Inject
    public AchievementService(AchievementRepository achievementRepository, INotificationService notificationService) {
        repo = achievementRepository;
        this.notificationService = notificationService;
        handler = new AchievementEventHandler();
    }

    private void addGamePlayed(List<Player> players, Role role) {

    }

    public void registerEventHandler() {
        if (isEventHandlerRegistered) {
            logger.info("AchievementEventHandler is already registered");
            return;
        }

        notificationService.register(handler);
        isEventHandlerRegistered = true;
    }

    @PreDestroy
    public void unregisterEventHandler() {
        if (!isEventHandlerRegistered) {
            logger.info("AchievementEventHandler is not registered");
            return;
        }

        notificationService.unregister(handler);
        isEventHandlerRegistered = false;
    }

    public class AchievementEventHandler {

        @Subscribe
        @SuppressWarnings("unused")
        public void handleGameStarted(GameStartedEvent event) {
            Arrays.asList(Role.DEFENDER, Role.ATTACKER, Role.PLAYER).forEach(role -> {
                List<Player> players = GameDAO.getPlayersForGame(event.getGameId(), role);
                addGamePlayed(players, role);
            });
        }

        @Subscribe
        @SuppressWarnings("unused")
        public void handleGameStopped(GameStoppedEvent event) {

        }
    }
}
