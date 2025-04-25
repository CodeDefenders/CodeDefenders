/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
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
