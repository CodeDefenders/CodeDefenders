package org.codedefenders.game;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.game.GameStoppedEvent;
import org.codedefenders.service.game.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

@ApplicationScoped
@Named
public class GameStoppedEventHandlerContainer {

    private final INotificationService notificationService;
    private GameStoppedEventHandler gameStoppedEventHandler;

    private static final Logger logger = LoggerFactory.getLogger(GameStoppedEventHandlerContainer.class);

    @Inject
    public GameStoppedEventHandlerContainer(INotificationService notificationService, GameService gameService) {
        this.notificationService = notificationService;
        this.gameStoppedEventHandler = new GameStoppedEventHandler(gameService);
    }

    public void registerEventHandler() {
        notificationService.register(gameStoppedEventHandler);
    }

    @PreDestroy
    public void unregisterEventHandler() {
        if (gameStoppedEventHandler != null) {
            notificationService.unregister(gameStoppedEventHandler);
            gameStoppedEventHandler = null;
        } else {
            logger.warn("No event handler to unregister");
        }
    }

    private record GameStoppedEventHandler(GameService gameService) {
        @Subscribe
        @SuppressWarnings("unused")
        public void handleGameStoppedEvent(GameStoppedEvent event) {
            var gameId = event.getGameId();
            logger.info("Game {} is closed. Resolving all open equivalence duels now.", gameId);
            gameService.resolveAllOpenDuels(gameId);
        }
    }
}
