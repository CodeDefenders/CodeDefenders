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
