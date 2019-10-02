package org.codedefenders.notification.handling.server;

import com.google.common.eventbus.Subscribe;
import org.codedefenders.notification.events.server.test.TestLifecycleEvent;
import org.codedefenders.notification.web.PushSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.EncodeException;
import java.io.IOException;

public class TestProgressBarEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(TestProgressBarEventHandler.class);

    private final PushSocket socket;
    private int playerId;

    public TestProgressBarEventHandler(PushSocket socket, int playerId) {
        this.socket = socket;
        this.playerId = playerId;
    }

    public int getPlayerId() {
        return playerId;
    }

    @Subscribe
    public void updateProgressBar(TestLifecycleEvent event) throws IOException, EncodeException {
        if (this.playerId == event.getTest().getPlayerId()) {
            socket.sendEvent(event);
        }
    }
}
