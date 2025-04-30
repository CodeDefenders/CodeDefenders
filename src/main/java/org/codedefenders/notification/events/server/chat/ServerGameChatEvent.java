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
package org.codedefenders.notification.events.server.chat;

import org.codedefenders.game.Role;

import com.google.gson.annotations.Expose;

/**
 * Represents a message written by a user in the game chat.
 */
public class ServerGameChatEvent extends ServerChatEvent {
    /**
     * The user id of the sender.
     */
    @Expose private int senderId;

    /**
     * The username of the sender.
     */
    @Expose private String senderName;

    /**
     * Role of the sender.
     */
    @Expose private Role role;

    /**
     * {@code true} if message is intended for all players,
     * {@code false} if for team members only.
     */
    @Expose private boolean isAllChat;

    private int gameId;

    public int getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public int getGameId() {
        return gameId;
    }

    public Role getRole() {
        return role;
    }

    public boolean isAllChat() {
        return isAllChat;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setAllChat(boolean allChat) {
        isAllChat = allChat;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }
}
