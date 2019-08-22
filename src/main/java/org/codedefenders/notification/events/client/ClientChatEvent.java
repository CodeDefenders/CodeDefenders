package org.codedefenders.notification.events.client;

import org.codedefenders.notification.handling.client.ClientEventHandler;

/**
 * A chat message (from the in-game chat).
 */
// TODO: What other attributes do we need? Team message or all-chat message? Other scopes?
public class ClientChatEvent extends ClientEvent {
    private String message;

    public ClientChatEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void accept(ClientEventHandler visitor) {
        visitor.visit(this);
    }
}
