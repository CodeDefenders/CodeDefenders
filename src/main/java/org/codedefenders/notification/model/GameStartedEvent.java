package org.codedefenders.notification.model;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.EventStatus;

/**
 * @author gambi
 */
public class GameStartedEvent extends GameLifecycleEvent {
    public GameStartedEvent(AbstractGame game) {
        super(game);
    }
}
