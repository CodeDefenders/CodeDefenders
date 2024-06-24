package org.codedefenders.beans.game;

import jakarta.enterprise.context.RequestScoped;

/**
 * <p>Provides data for the test progress bar game component.</p>
 * <p>Bean Name: {@code testProgressBar}</p>
 */
@RequestScoped
public class TestProgressBarBean {
    /**
     * The game id of the currently played game.
     * Used for event parameters.
     */
    private Integer gameId;

    public TestProgressBarBean() {
        gameId = null;
    }

    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }

    // --------------------------------------------------------------------------------

    public Integer getGameId() {
        return gameId;
    }
}
