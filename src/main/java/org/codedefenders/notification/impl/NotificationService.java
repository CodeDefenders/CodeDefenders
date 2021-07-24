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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import org.codedefenders.notification.INotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;

/**
 * Notification Service implementation.
 * This service behaves like a singleton in the app.
 * See https://docs.oracle.com/javaee/6/api/javax/enterprise/context/ApplicationScoped.html
 *
 * @author gambi
 */
@ManagedBean
@Singleton
public class NotificationService implements INotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final int NUM_THREADS = 8;

    private final ExecutorService executor;

    @SuppressWarnings("UnstableApiUsage")
    private final EventBus eventBus;

    public NotificationService() {
        executor = Executors.newFixedThreadPool(NUM_THREADS);
        //noinspection UnstableApiUsage
        eventBus = new AsyncEventBus(executor, ((exception, context) -> {
            logger.warn("Got {} while calling notification handler.", exception.getClass().getSimpleName(), exception);
            logger.warn("Event was: {}", new Gson().toJson(context.getEvent()));
        }));
    }

    @Override
    public void post(Object message) {
        //noinspection UnstableApiUsage
        eventBus.post(message);
    }

    @Override
    public void register(Object eventHandler) {
        //noinspection UnstableApiUsage
        eventBus.register(eventHandler);
    }

    @Override
    public void unregister(Object eventHandler) {
        //noinspection UnstableApiUsage
        eventBus.unregister(eventHandler);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
