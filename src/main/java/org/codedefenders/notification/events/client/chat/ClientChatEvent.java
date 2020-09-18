package org.codedefenders.notification.events.client.chat;

import org.codedefenders.notification.events.client.ClientEvent;

import com.google.gson.annotations.Expose;

/**
 * A chat message (from the in-game chat).
 */
// TODO: What other attributes do we need? Team message or all-chat message? Other scopes?
public abstract class ClientChatEvent extends ClientEvent {
    @Expose private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
