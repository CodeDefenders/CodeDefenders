package org.codedefenders.notification.model;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.EventStatus;

/**
 * @author gambi
 *
 */
public class GameStartedEvent extends GameLifecycleEvent{

    final static String eventType = EventStatus.STARTED.toString();
    
    public GameStartedEvent(AbstractGame game) {
        super(game, eventType);
    }

}
