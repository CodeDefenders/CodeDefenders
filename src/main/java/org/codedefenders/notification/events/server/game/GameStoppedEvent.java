package org.codedefenders.notification.events.server.game;

import org.codedefenders.game.AbstractGame;

public class GameStoppedEvent extends GameLifecycleEvent {
    public GameStoppedEvent(AbstractGame game) {
        super(game);
    }
}
