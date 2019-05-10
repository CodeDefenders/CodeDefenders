package org.codedefenders.notification.model;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.EventStatus;

/**
 * @author gambi
 */
public class GameStoppedEvent extends GameLifecycleEvent {

    // Maybe Ended ?!
    final static String eventType = EventStatus.DELETED.toString();
    
    public GameStoppedEvent(AbstractGame game) {
        super(game, eventType);
    }

}
