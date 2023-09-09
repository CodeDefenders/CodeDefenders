package org.codedefenders.notification.events.server.achievement;

import org.codedefenders.model.Achievement;
import org.codedefenders.notification.events.server.ServerEvent;

import com.google.gson.annotations.Expose;

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
