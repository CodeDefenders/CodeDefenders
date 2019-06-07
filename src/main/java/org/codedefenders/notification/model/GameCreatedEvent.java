package org.codedefenders.notification.model;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.EventStatus;

/**
 * @author gambi
 */
public class GameCreatedEvent extends GameLifecycleEvent {
    public GameCreatedEvent(AbstractGame game) {
        super(game);
    }
}
