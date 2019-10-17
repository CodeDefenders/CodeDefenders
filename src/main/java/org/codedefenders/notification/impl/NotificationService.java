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

import org.codedefenders.notification.INotificationService;

import com.google.common.eventbus.EventBus;

/**
 * Notification Service implementation.
 * This service behaves like a singleton in the app.
 * See https://docs.oracle.com/javaee/6/api/javax/enterprise/context/ApplicationScoped.html
 * @author gambi
 */
@ManagedBean
@ApplicationScoped
public class NotificationService implements INotificationService {

    // TODO Maybe there's some better instantiation options
    private EventBus eventBus = new EventBus();

    // TODO Ensures that event bus is defined in a Tomcat System Listener !
    // public NotificationService(EventBus eventBus) {
    //     this.eventBus = eventBus;
    // }

    public NotificationService() {}

    @Override
    public void post(Object message) {
        // TODO: disable notifications for now
        // eventBus.post(message);
    }

    @Override
    public void register(Object eventHandler) {
        // TODO: disable notifications for now
        // eventBus.register(eventHandler);
    }

    @Override
    public void unregister(Object eventHandler) {
        // TODO: disable notifications for now
        // eventBus.unregister(eventHandler);
    }
}
