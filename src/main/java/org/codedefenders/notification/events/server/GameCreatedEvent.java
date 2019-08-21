package org.codedefenders.notification.events.server;

import org.codedefenders.game.AbstractGame;

/**
 * @author gambi
 */
public class GameCreatedEvent extends GameLifecycleEvent {
    public GameCreatedEvent(AbstractGame game) {
        super(game);
    }
}
