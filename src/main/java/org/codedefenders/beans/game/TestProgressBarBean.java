package org.codedefenders.beans.game;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;

/**
 * <p>Provides data for the test progress bar game component.</p>
 * <p>Bean Name: {@code testProgressBar}</p>
 */
@ManagedBean
@RequestScoped
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
