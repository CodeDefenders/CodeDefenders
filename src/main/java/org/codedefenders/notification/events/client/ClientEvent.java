package org.codedefenders.notification.events.client;

import org.codedefenders.notification.handling.ClientEventHandler;
import org.codedefenders.notification.web.EventDecoder;

/**
 * <p>
 * A message sent by a client WebSocket.
 * JSON messages from clients are converted into this.
 * </p>
 *
 * <p>
 * The client JSON messages must contain all attributes of the Event class
 * annotated by {@code @Expose(deserialize = true)}.
 * </p>
 *
 * <p>
 * Client events must have a no-arg constructor and getter/setter methods for all attributes.
 * </p>
 * @see EventDecoder
 */
public abstract class ClientEvent {
    public abstract void accept(ClientEventHandler visitor);
}
