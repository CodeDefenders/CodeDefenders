package org.codedefenders.notification.events.server;

import org.codedefenders.notification.web.EventEncoder;

/**
 * A message sent by a client WebSocket.
 * JSON messages from clients are converted into this.
 * <p></p>
 * The client JSON messages must contain all attributes of the Event class
 * annotated by {@code @Expose(deserialize = true)}.
 * <p></p>
 * Client events must have a no-arg constructor and getter/setter methods for all attributes.
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
    public void setTicket(String ticket) {
        this.ticket = ticket;
    }
}
