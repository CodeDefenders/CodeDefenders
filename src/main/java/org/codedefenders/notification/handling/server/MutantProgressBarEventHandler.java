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

    private final PushSocket socket;
    private int playerId;

    public MutantProgressBarEventHandler(PushSocket socket, int playerId) {
        this.socket = socket;
        this.playerId = playerId;
    }

    public int getPlayerId() {
        return playerId;
    }

    @Subscribe
    public void updateProgressBar(MutantLifecycleEvent event) throws IOException, EncodeException {
        if (this.playerId == event.getMutant().getPlayerId()) {
            socket.sendEvent(event);
        }
    }
}
