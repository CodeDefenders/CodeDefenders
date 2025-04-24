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
package org.codedefenders.notification.events.client.chat;

import org.codedefenders.notification.handling.ClientEventHandler;

import com.google.gson.annotations.Expose;

public class ClientGameChatEvent extends ClientChatEvent {
    @Expose private int gameId;

    /**
     * {@code true} if message is intended for all players,
     * {@code false} if for team members only.
     */
    @Expose private boolean allChat;

    public int getGameId() {
        return gameId;
    }

    public boolean isAllChat() {
        return allChat;
    }

    public void setAllChat(boolean allChat) {
        this.allChat = allChat;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public void accept(ClientEventHandler visitor) {
        visitor.visit(this);
    }
}
