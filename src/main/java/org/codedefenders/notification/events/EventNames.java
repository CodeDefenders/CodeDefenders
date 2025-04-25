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
package org.codedefenders.notification.events;

import org.codedefenders.notification.events.client.ClientEvent;
import org.codedefenders.notification.events.server.ServerEvent;

/**
 * Provides mapping between sever and client event names / types (used to send them via JSON) and their classes.
 * To avoid security risks, only part of the package name is usd for the event names.
 */
public class EventNames {
    /**
     * Returns the name of a server event.
     *
     * @param eventClass The class of the server event.
     * @return The name of the server event.
     */
    public static String toServerEventName(Class<? extends ServerEvent> eventClass) {
        String fullName = eventClass.getName();
        return fullName.substring("org.codedefenders.notification.events.server.".length());
    }

    /**
     * Returns the name of a client event.
     *
     * @param eventClass The class of the client event.
     * @return The name of the client event.
     */
    public static String toClientEventName(Class<? extends ClientEvent> eventClass) {
        String fullName = eventClass.getName();
        return fullName.substring("org.codedefenders.notification.events.client.".length());
    }

    /**
     * Returns the client event class for the given event name.
     *
     * @param eventName The event name.
     * @return The client event class for the given event name.
     * @throws ClassNotFoundException If no client event for the name exists.
     * @throws ClassCastException If the class with that name is no client event class.
     */
    @SuppressWarnings("unchecked")
    public static Class<ClientEvent> toClientEvent(String eventName) throws ClassNotFoundException, ClassCastException {
        return (Class<ClientEvent>) Class.forName("org.codedefenders.notification.events.client." + eventName);
    }
}
