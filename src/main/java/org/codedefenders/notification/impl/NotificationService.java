/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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

import org.codedefenders.notification.INotificationService;
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

    /**
     * @implNote executor is shutdown by {@link ExecutorServiceProvider#shutdown()}
     */
    private final EventBus eventBus;

    @Inject
    public NotificationService(ExecutorServiceProvider executorServiceProvider) {
        eventBus = new AsyncEventBus(executorServiceProvider.createExecutorService("notificationServiceEventBus", NUM_THREADS), ((exception, context) -> {
            logger.warn("Got {} while calling notification handler.", exception.getClass().getSimpleName(), exception);
            logger.warn("Event was: {}", new Gson().toJson(context.getEvent()));
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
}
