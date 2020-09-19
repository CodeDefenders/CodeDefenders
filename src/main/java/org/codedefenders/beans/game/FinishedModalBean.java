package org.codedefenders.beans.game;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;

import org.codedefenders.util.Constants;

/**
 * <p>Provides data for the finished modal game component.</p>
 * <p>Bean Name: {@code finishedModal}</p>
 */
@ManagedBean
@RequestScoped
public class FinishedModalBean {
    /**
     * Indicates if the player has won or lost the game, or if the game ended in a draw.
     */
    private GameOutcome outcome;

    public FinishedModalBean() {
        outcome = null;
    }

    // TODO: finished_modal is currently not used. Add setters as needed.

    // --------------------------------------------------------------------------------

    public String getMessage() {
        switch (outcome) {
            case WIN:
                return Constants.WINNER_MESSAGE;
            case LOSS:
                return Constants.LOSER_MESSAGE;
            case DRAW:
                return Constants.DRAW_MESSAGE;
            default:
                return "";
        }
    }

    private enum GameOutcome {
        WIN, LOSS, DRAW
    }
}
