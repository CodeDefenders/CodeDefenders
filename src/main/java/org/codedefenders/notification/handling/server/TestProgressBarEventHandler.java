package org.codedefenders.notification.handling.server;

import com.google.common.eventbus.Subscribe;
import org.codedefenders.notification.events.server.test.TestLifecycleEvent;
import org.codedefenders.notification.web.PushSocket;

import javax.websocket.EncodeException;
import java.io.IOException;

public class TestProgressBarEventHandler {
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
