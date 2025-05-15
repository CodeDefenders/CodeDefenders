/*
 * Copyright (C) 2025 Code Defenders contributors
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
        this.gameStoppedEventHandler = new GameStoppedEventHandler(gameService, notificationService);
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

    private record GameStoppedEventHandler(GameService gameService, INotificationService notificationService) {
        @Subscribe
        @SuppressWarnings("unused")
        public void handleGameStoppedEvent(GameStoppedEvent event) {
            var gameId = event.getGameId();

            logger.info("Game {} is closed. Resolving all open equivalence duels now.", gameId);

            gameService.resolveAllOpenDuelsAsync(gameId);
        }
    }
}
