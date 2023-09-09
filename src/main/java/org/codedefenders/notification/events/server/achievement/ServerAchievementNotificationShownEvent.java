package org.codedefenders.notification.events.server.achievement;

import org.codedefenders.notification.events.server.ServerEvent;

import com.google.gson.annotations.Expose;

public class ServerAchievementNotificationShownEvent extends ServerEvent {

    @Expose
    private int achievementId;

    @Expose
    private int userId;

    public int getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(int achievementId) {
        this.achievementId = achievementId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
