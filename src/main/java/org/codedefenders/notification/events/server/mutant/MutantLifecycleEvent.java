package org.codedefenders.notification.events.server.mutant;

import org.codedefenders.notification.events.server.ServerEvent;

import com.google.gson.annotations.Expose;

public abstract class MutantLifecycleEvent extends ServerEvent {
    /* TODO: Mutants don't have an Id until compiled, why not give them and Id before? */
    @Expose private Integer mutantId;
    @Expose private int userId;
    @Expose private int gameId;

    public Integer getMutantId() {
        return mutantId;
    }

    public int getUserId() {
        return userId;
    }

    public int getGameId() {
        return gameId;
    }

    public void setMutantId(Integer mutantId) {
        this.mutantId = mutantId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }
}
