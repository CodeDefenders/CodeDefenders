package org.codedefenders.notification.model;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.EventStatus;

/**
 * @author gambi
 */
// Maybe Ended ?!
public class GameStoppedEvent extends GameLifecycleEvent {
    public GameStoppedEvent(AbstractGame game) {
        super(game);
    }

}
