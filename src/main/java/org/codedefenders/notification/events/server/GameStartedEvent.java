package org.codedefenders.notification.events.server;

import org.codedefenders.game.AbstractGame;

/**
 * @author gambi
 */
public class GameStartedEvent extends GameLifecycleEvent {
    public GameStartedEvent(AbstractGame game) {
        super(game);
    }
}
