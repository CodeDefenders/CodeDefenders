package org.codedefenders.notification.handling.server;

import com.google.common.eventbus.Subscribe;
import org.codedefenders.notification.events.server.test.TestLifecycleEvent;
import org.codedefenders.notification.web.EventEncoder;
import org.codedefenders.notification.web.PushSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.EncodeException;
import java.io.IOException;

public class TestProgressBarEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(TestProgressBarEventHandler.class);

    private final PushSocket socket;
    private int gameId;
    private int userId;

    public TestProgressBarEventHandler(PushSocket socket, int gameId, int userId) {
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
    public void updateProgressBar(TestLifecycleEvent event) throws IOException, EncodeException {
        if (this.gameId == event.getGameId() || this.userId == event.getUserId()) {
            socket.sendEvent(event);
        }
    }
}
