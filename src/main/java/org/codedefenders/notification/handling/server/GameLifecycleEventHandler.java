package org.codedefenders.notification.handling.server;

import java.util.Objects;

import org.codedefenders.notification.events.server.game.GameLifecycleEvent;
import org.codedefenders.notification.web.PushSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

public class GameLifecycleEventHandler implements ServerEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(GameLifecycleEventHandler.class);

    private final PushSocket socket;
    private final int gameId;
    private final int userId;

    public GameLifecycleEventHandler(PushSocket socket, int gameId, int userId) {
        this.socket = socket;
        this.gameId = gameId;
        this.userId = userId;
    }

    public int getGameId() {
        return gameId;
    }

    public int getUserId() {
        return userId;
    }

    @Subscribe
    public void sendGameLifecycleEvent(GameLifecycleEvent event) {
        if (event.getGameId() == this.gameId) {
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
        GameLifecycleEventHandler that = (GameLifecycleEventHandler) o;
        return gameId == that.gameId
                && userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, userId);
    }
}
