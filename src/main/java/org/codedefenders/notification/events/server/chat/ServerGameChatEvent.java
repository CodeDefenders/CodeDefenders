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

    public ServerGameChatEvent(User user, String message, int gameId, Role role, boolean isAllChat) {
        super(user, message);
        this.gameId = gameId;
        this.role = role;
        this.isAllChat = isAllChat;
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
}
