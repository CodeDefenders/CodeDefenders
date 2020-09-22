package org.codedefenders.notification.events.client.registration;

import org.codedefenders.notification.events.server.chat.ServerGameChatEvent;
import org.codedefenders.notification.handling.ClientEventHandler;

import com.google.gson.annotations.Expose;

/**
 * A message used to register for chat events.
 * <br>
 * This includes:
 * <ul>
 * <li>{@link ServerGameChatEvent}</li>
 * </ul>
 */
public class GameChatRegistrationEvent extends RegistrationEvent {
    @Expose private int gameId;

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    @Override
    public void accept(ClientEventHandler visitor) {
        visitor.visit(this);
    }
}
