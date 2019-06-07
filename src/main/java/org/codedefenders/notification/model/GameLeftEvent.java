package org.codedefenders.notification.model;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.User;

/**
 * @author gambi
 */
// Maybe Ended ?!
public class GameLeftEvent extends GameLifecycleEvent {
    private User user;

    public GameLeftEvent(AbstractGame game, User user) {
        super(game);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
