package org.codedefenders.notification.events.server.equivalence;

import org.codedefenders.notification.events.server.ServerEvent;

import com.google.gson.annotations.Expose;

/**
 * Equivalence duel won event.
 */
public class EquivalenceDuelWonEvent extends ServerEvent {
    @Expose
    private int userId;

    @Expose
    private int gameId;

    @Expose
    private int mutantId;

    /**
     * Returns the user id of the winner.
     *
     * @return the user id of the winner
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Sets the user id of the winner.
     *
     * @param userId the user id of the winner
     */
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getMutantId() {
        return mutantId;
    }

    public void setMutantId(int mutantId) {
        this.mutantId = mutantId;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }
}
