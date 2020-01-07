package org.codedefenders.beans.game;

/**
 * <p>Provides data for the test progress bar game component.</p>
 * <p>Bean Name: {@code testProgressBar}</p>
 */
public class TestProgressBarBean {
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
