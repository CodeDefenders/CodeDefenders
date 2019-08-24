package org.codedefenders.notification.events.server.game;

import org.codedefenders.game.AbstractGame;

public class GameCreatedEvent extends GameLifecycleEvent {
    public GameCreatedEvent(AbstractGame game) {
        super(game);
    }
}
