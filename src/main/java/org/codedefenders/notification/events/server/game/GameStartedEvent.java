package org.codedefenders.notification.events.server.game;

import org.codedefenders.game.AbstractGame;

public class GameStartedEvent extends GameLifecycleEvent {
    public GameStartedEvent(AbstractGame game) {
        super(game);
    }
}
