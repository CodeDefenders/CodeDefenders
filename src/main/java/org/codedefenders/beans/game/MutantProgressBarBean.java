package org.codedefenders.beans.game;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;

/**
 * <p>Provides data for the mutant progress bar game component.</p>
 * <p>Bean Name: {@code mutantProgressBar}</p>
 */
@ManagedBean
@RequestScoped
public class MutantProgressBarBean {
    /**
     * The game id of the currently played game.
     * Used for event parameters.
     */
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
