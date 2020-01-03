package org.codedefenders.notification.events.server.chat;

import com.google.gson.annotations.Expose;
import org.codedefenders.game.Role;
import org.codedefenders.model.User;

public class ServerGameChatEvent extends ServerChatEvent {
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

    public int getGameId() {
        return gameId;
    }

    public Role getRole() {
        return role;
    }

    public boolean isAllChat() {
        return isAllChat;
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
