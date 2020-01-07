package org.codedefenders.beans.game;

/**
 * <p>Provides data for the mutant progress bar game component.</p>
 * <p>Bean Name: {@code mutantProgressBar}</p>
 */
public class MutantProgressBarBean {
    private Integer gameId;

    public MutantProgressBarBean() {
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
