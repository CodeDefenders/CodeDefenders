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
package org.codedefenders.notification.impl;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.WhitelistType;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.invite.InviteEvent;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.codedefenders.util.concurrent.ExecutorServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;

/**
 * Notification Service implementation.
 * This service behaves like a singleton in the app.
 *
 * @author gambi
 */
@Singleton
public class NotificationService implements INotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final int NUM_THREADS = 8;

    @Inject
    URLUtils urlUtils;

    /**
     * @implNote executor is shutdown by {@link ExecutorServiceProvider#shutdown()}
     */
    private final EventBus eventBus;

    @Inject
    public NotificationService(ExecutorServiceProvider executorServiceProvider) {
        eventBus = new AsyncEventBus(executorServiceProvider.createExecutorService("notificationServiceEventBus", NUM_THREADS), ((exception, context) -> {
            logger.warn("Got {} while calling notification handler.", exception.getClass().getSimpleName(), exception);
            logger.warn("Event was: {} {}", context.getEvent().getClass().getSimpleName(), new Gson().toJson(context.getEvent()));
        }));
    }

    @Override
    public void post(Object message) {
        eventBus.post(message);
    }

    @Override
    public void register(Object eventHandler) {
        eventBus.register(eventHandler);
    }

    @Override
    public void unregister(Object eventHandler) {
        eventBus.unregister(eventHandler);
    }

    /**
     * Send an invitation event for a player to a specific game, so that they can see a pop-up to accept.
     * @param game The game the player is invited to. May be melee or battlegrounds.
     * @param userId The id of the invited user.
     * @param type If {@code game} is a battlegrounds game and {@link MultiplayerGame#isMayChooseRoles()} is
     *             {@code false}, this is the role selected for the player, otherwise the value is ignored and may be
     *             {@code null}.
     */
    public void sendInviteNotification(AbstractGame game, int userId, WhitelistType type) {
        InviteEvent event = new InviteEvent();
        event.setInviteLink(urlUtils.forPath(Paths.INVITE) + "?inviteId=" + game.getInviteId());
        event.setUserId(userId);
        event.setClassName(game.getClass().getSimpleName());
        if (game instanceof MultiplayerGame) {

            event.setMayChooseRole(((MultiplayerGame)game).isMayChooseRoles());
            if (!((MultiplayerGame)game).isMayChooseRoles()) {
                event.setRole(type);
            }
        }
        logger.info("About to send invite for game {} to user {}",
                game.getId(), userId);
        post(event);
    }
}
