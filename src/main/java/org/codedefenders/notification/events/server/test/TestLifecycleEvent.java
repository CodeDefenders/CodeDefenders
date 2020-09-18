package org.codedefenders.notification.events.server.test;

import org.codedefenders.notification.events.server.ServerEvent;

import com.google.gson.annotations.Expose;

public abstract class TestLifecycleEvent extends ServerEvent {
    /* TODO: Tests don't have an Id until compiled, why not give them and Id before? */
    @Expose private Integer testId;
    @Expose private int userId;
    @Expose private int gameId;

    public Integer getTestId() {
        return testId;
    }

    public int getUserId() {
        return userId;
    }

    public int getGameId() {
        return gameId;
    }

    public void setTestId(Integer testId) {
        this.testId = testId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }
}
