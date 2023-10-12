package org.codedefenders.notification.events.client.achievement;

import org.codedefenders.notification.events.client.ClientEvent;
import org.codedefenders.notification.handling.ClientEventHandler;

import com.google.gson.annotations.Expose;

/**
 * This event is sent by the client once it receives the achievement unlocked event from the server.
 * It is used to notify the server that the achievement notification has been shown to the user and does not need to be
 * sent again.
 */
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
