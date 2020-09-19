package org.codedefenders.notification.events.server.chat;

import org.codedefenders.notification.events.server.ServerEvent;

import com.google.gson.annotations.Expose;

public abstract class ServerChatEvent extends ServerEvent {
    @Expose private int senderId;
    @Expose private String senderName;
    @Expose private String message; // TODO: Escaping, size limit, etc..

    public int getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getMessage() {
        return message;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
