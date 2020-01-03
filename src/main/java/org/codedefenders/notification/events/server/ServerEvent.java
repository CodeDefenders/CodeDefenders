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
