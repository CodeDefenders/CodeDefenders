package org.codedefenders.notification.events.server.game;

import com.google.gson.annotations.Expose;

public class GameLeftEvent extends GameLifecycleEvent {
    @Expose private int userId;
    @Expose private String userName;

    public int getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
