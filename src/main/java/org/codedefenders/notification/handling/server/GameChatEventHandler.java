package org.codedefenders.notification.handling.server;

import com.google.common.eventbus.Subscribe;
import org.codedefenders.game.Role;
import org.codedefenders.notification.events.server.chat.ServerGameChatEvent;
import org.codedefenders.notification.web.PushSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.EncodeException;
import java.io.IOException;
import java.util.Objects;

public class GameChatEventHandler implements ServerEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(GameChatEventHandler.class);

    private PushSocket socket;
    private int gameId;
    private Role role;

    public GameChatEventHandler(PushSocket socket, int gameId, Role role) {
        this.socket = socket;
        this.gameId = gameId;
        this.role = role;
    }

    public int getGameId() {
        return gameId;
    }

    public Role getRole() {
        return role;
    }

    @Subscribe
    public void sendChatMessage(ServerGameChatEvent event) throws IOException, EncodeException {
        if (event.getGameId() == this.gameId && (event.isAllChat() || event.getRole() == role)) {
            socket.sendEvent(event);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GameChatEventHandler that = (GameChatEventHandler) o;
        return gameId == that.gameId
                && role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, role);
    }
}
