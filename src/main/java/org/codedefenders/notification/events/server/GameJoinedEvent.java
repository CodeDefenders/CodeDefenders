package org.codedefenders.notification.events.server;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.User;

/**
 * @author gambi
 */
// Maybe Ended ?!
public class GameJoinedEvent extends GameLifecycleEvent {
    private User user;

    public GameJoinedEvent(AbstractGame game, User user) {
        super(game);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
