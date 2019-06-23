package org.codedefenders.notification.model;

import org.codedefenders.game.AbstractGame;

/**
 * Higher level LifeCycleEvent, by subscribing to this type of event one can
 * receive ALL sort of life cycle events (i.e., subclasses)
 *
 * @author gambi
 */
public abstract class GameLifecycleEvent {
    private AbstractGame game;

    public GameLifecycleEvent(AbstractGame game) {
        this.game = game;
    }

    public AbstractGame getGame() {
        return game;
    }
}
