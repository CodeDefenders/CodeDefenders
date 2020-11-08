package org.codedefenders.notification.events.server.chat;

import org.codedefenders.notification.events.server.ServerEvent;

import com.google.gson.annotations.Expose;

public abstract class ServerChatEvent extends ServerEvent {
    @Expose private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
