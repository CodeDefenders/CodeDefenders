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

import javax.annotation.ManagedBean;
import javax.enterprise.context.ApplicationScoped;

import com.google.common.eventbus.SubscriberExceptionHandler;
import com.google.gson.Gson;
import org.codedefenders.notification.INotificationService;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notification Service implementation.
 * This service behaves like a singleton in the app.
 * See https://docs.oracle.com/javaee/6/api/javax/enterprise/context/ApplicationScoped.html
 * @author gambi
 */
@ManagedBean
@ApplicationScoped
public class NotificationService implements INotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private SubscriberExceptionHandler exceptionHandler = (exception, context) -> {
        Gson gson = new Gson();
        logger.warn("Got " + exception.getClass().getSimpleName() + " while calling notification handler.", exception);
        logger.warn("Event was: " + gson.toJson(context.getEvent()));
    };

    // TODO: Use AsyncEventBus?
    @SuppressWarnings("UnstableApiUsage")
    private EventBus eventBus = new EventBus(exceptionHandler);

    // TODO Ensures that event bus is defined in a Tomcat System Listener !
    // public NotificationService(EventBus eventBus) {
    //     this.eventBus = eventBus;
    // }

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
