package org.codedefenders.notification.events.server;

import org.codedefenders.game.AbstractGame;

/**
 * @author gambi
 */
// Maybe Ended ?!
public class GameStoppedEvent extends GameLifecycleEvent {
    public GameStoppedEvent(AbstractGame game) {
        super(game);
    }
}
