package org.codedefenders.notification.model;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.EventStatus;

/**
 * @author gambi
 *
 */
public class GameCreatedEvent extends GameLifecycleEvent{

    final static String eventType = EventStatus.NEW.toString();
    
    public GameCreatedEvent(AbstractGame game) {
        super(game, eventType);
    }

}
