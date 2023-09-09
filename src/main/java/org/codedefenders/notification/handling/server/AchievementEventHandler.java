package org.codedefenders.notification.handling.server;

import java.util.Objects;

import org.codedefenders.notification.events.server.achievement.AchievementUnlockedEvent;
import org.codedefenders.notification.web.PushSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

public class AchievementEventHandler implements ServerEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(AchievementEventHandler.class);

    private final PushSocket socket;
    private final int userId;

    public AchievementEventHandler(PushSocket socket, int userId) {
        this.socket = socket;
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    @Subscribe
    public void sendAchievementUnlockedEvent(AchievementUnlockedEvent event) {
        if (event.getUserId() == this.userId) {
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
        AchievementEventHandler that = (AchievementEventHandler) o;
        return userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
