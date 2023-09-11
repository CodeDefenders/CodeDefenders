package org.codedefenders.notification.events.server.achievement;

import org.codedefenders.notification.events.client.achievement.ClientAchievementNotificationShownEvent;
import org.codedefenders.notification.events.server.ServerEvent;

import com.google.gson.annotations.Expose;

/**
 * This event is fired once the client sends the {@link ClientAchievementNotificationShownEvent} to the server.
 * It is used to notify the server that the achievement notification has been shown to the user and does not need to be
 * sent again.
 */
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
