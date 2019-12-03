package org.codedefenders.notification.events.client.chat;

import com.google.gson.annotations.Expose;
import org.codedefenders.notification.handling.ClientEventHandler;

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
