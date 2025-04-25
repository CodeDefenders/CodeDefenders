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
package org.codedefenders.notification.events.server.achievement;

import org.codedefenders.model.Achievement;
import org.codedefenders.notification.events.server.ServerEvent;

import com.google.gson.annotations.Expose;

/**
 * This event is sent by the server to notify the client that an achievement has been unlocked.
 * It is sent multiple times for the same achievement until the client acknowledges that it received the event.
 */
public class AchievementUnlockedEvent extends ServerEvent {

    @Expose
    private Achievement achievement;
    @Expose
    private int userId;

    public Achievement getAchievement() {
        return achievement;
    }

    public void setAchievement(Achievement achievement) {
        this.achievement = achievement;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
