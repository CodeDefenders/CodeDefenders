package org.codedefenders.notification.model;

import org.codedefenders.game.AbstractGame;

// TODO Not sure this is a good strategy...
public class GameLifecycleEvent {

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
