package org.codedefenders.notification.events.server.game;

import com.google.gson.annotations.Expose;
import org.codedefenders.notification.events.server.ServerEvent;

/**
 * Higher level LifeCycleEvent, by subscribing to this type of event one can
 * receive ALL sort of life cycle events (i.e., subclasses)
 *
 * @author gambi
 */
public abstract class GameLifecycleEvent extends ServerEvent {
    @Expose private int gameId;

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }
}
