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
package org.codedefenders.notification.events.server;

import org.codedefenders.notification.impl.NotificationService;
import org.codedefenders.notification.web.EventEncoder;

/**
 * <p>
 * A message sent through the {@link NotificationService}.
 * Server events are converted to JSON when sent to clients.
 * </p>
 *
 * <p>
 * Server events must have a no-arg constructor and getter/setter methods for all attributes.
 * </p>
 * @see EventEncoder
 */
public abstract class ServerEvent {
    private String ticket;

    /**
     * Get an optional WebSocket ticket for events that a single WebSocket session is interested in.
     * E.g. a WebSocket session might be interested in mutant lifecycle events for a progressbar,
     * but only for the mutant submitted on this page instance (the user may have multiple tabs/windows open).
     *
     * @return A WebSocket ticket, or {@code null}.
     */
    // TODO: use this?
    public String getTicket() {
        return ticket;
    }

    /**
     * Set an optional WebSocket ticket for events that a single WebSocket session is interested in.
     * E.g. a WebSocket session might be interested in mutant lifecycle events for a progressbar,
     * but only for the mutant submitted on this page instance (the user may have multiple tabs/windows open).
     *
     * @param ticket The WebSocket ticket.
     */
    // TODO: use this?
    public void setTicket(String ticket) {
        this.ticket = ticket;
    }
}
