package org.codedefenders.notification.events.client.achievement;

import org.codedefenders.notification.events.client.ClientEvent;
import org.codedefenders.notification.handling.ClientEventHandler;

import com.google.gson.annotations.Expose;

public class ClientAchievementNotificationShownEvent extends ClientEvent {

    @Expose
    private int achievementId;

    public int getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(int achievementId) {
        this.achievementId = achievementId;
    }

    @Override
    public void accept(ClientEventHandler visitor) {
        visitor.visit(this);
    }
}
