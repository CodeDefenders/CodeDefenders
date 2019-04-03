package org.codedefenders.notification.model;

import org.codedefenders.game.AbstractGame;

/**
 * Higher level LifeCycleEvent, by subscribing to this type of event one can
 * receive ALL sort of life cycle events (i.e., subclasses)
 * 
 * @author gambi
 *
 */
public abstract class GameLifecycleEvent {

    private AbstractGame game;
    private String eventType;

    public GameLifecycleEvent(AbstractGame game, String eventType) {
        super();
        this.game = game;
        this.eventType = eventType;
    }

    public AbstractGame getGame() {
        return game;
    }

    public String getEventType() {
        return eventType;
    }

}
