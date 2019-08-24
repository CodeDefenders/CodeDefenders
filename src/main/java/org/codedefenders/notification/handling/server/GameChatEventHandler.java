package org.codedefenders.notification.handling.server;

import com.google.common.eventbus.Subscribe;
import org.codedefenders.game.Role;
import org.codedefenders.notification.events.server.chat.ServerGameChatEvent;
import org.codedefenders.notification.web.PushSocket;

import javax.websocket.EncodeException;
import java.io.IOException;

public class GameChatEventHandler {
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
}
