package org.codedefenders.notification.model;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.User;

/**
 * @author gambi
 */
public class GameJoinedEvent extends GameLifecycleEvent {

    // Maybe Ended ?!
    final static String eventType = EventStatus.JOINED.toString();
    private User user;

    public GameJoinedEvent(AbstractGame game, User user) {
        super(game, eventType);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
