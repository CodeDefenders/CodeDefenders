package org.codedefenders.notification.handling.server;

import com.google.common.eventbus.Subscribe;
import org.codedefenders.notification.events.server.mutant.MutantLifecycleEvent;
import org.codedefenders.notification.web.PushSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.EncodeException;
import java.io.IOException;

public class MutantProgressBarEventHandler {
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
}
