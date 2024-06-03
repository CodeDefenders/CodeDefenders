package org.codedefenders.notification.handling.server;

import java.io.IOException;
import java.util.Objects;

import org.codedefenders.notification.events.server.mutant.MutantLifecycleEvent;
import org.codedefenders.notification.web.PushSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import jakarta.websocket.EncodeException;

public class MutantProgressBarEventHandler implements ServerEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(MutantProgressBarEventHandler.class);

    private PushSocket socket;
    private int gameId;
    private int userId;

    public MutantProgressBarEventHandler(PushSocket socket, int gameId, int userId) {
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
    public void updateProgressBar(MutantLifecycleEvent event) throws IOException, EncodeException {
        if (this.gameId == event.getGameId() || this.userId == event.getUserId()) {
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
        MutantProgressBarEventHandler that = (MutantProgressBarEventHandler) o;
        return gameId == that.gameId
                && userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, userId);
    }
}
