package org.codedefenders.notification.events.client;

import org.codedefenders.notification.handling.client.ClientEventHandler;

/**
 * A message sent by a client WebSocket.
 * JSON messages from clients are converted into this.
 */
public abstract class ClientEvent {
    public abstract void accept(ClientEventHandler visitor);
}
