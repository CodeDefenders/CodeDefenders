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
package org.codedefenders.beans.notification;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.codedefenders.notification.events.EventNames;
import org.codedefenders.notification.events.client.chat.ClientGameChatEvent;
import org.codedefenders.notification.events.client.registration.GameChatRegistrationEvent;
import org.codedefenders.notification.events.server.chat.ServerGameChatEvent;
import org.codedefenders.notification.events.server.chat.ServerSystemChatEvent;

@ApplicationScoped
@Named("eventNames")
public class EventNamesBean {
    public String getGameChatRegistrationEventName() {
        return EventNames.toClientEventName(GameChatRegistrationEvent.class);
    }

    public String getServerChatEventName() {
        return EventNames.toServerEventName(ServerGameChatEvent.class);
    }

    public String getServerSystemChatEventName() {
        return EventNames.toServerEventName(ServerSystemChatEvent.class);
    }

    public String getClientChatEventName() {
        return EventNames.toClientEventName(ClientGameChatEvent.class);
    }
}
