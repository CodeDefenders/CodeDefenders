package org.codedefenders.notification.events.client;

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
}
