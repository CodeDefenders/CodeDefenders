package org.codedefenders.notification.events.server.game;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.notification.events.server.ServerEvent;

/**
 * Higher level LifeCycleEvent, by subscribing to this type of event one can
 * receive ALL sort of life cycle events (i.e., subclasses)
 *
 * @author gambi
 */
public abstract class GameLifecycleEvent extends ServerEvent {
    private AbstractGame game;
    @Expose private int gameId;

    public GameLifecycleEvent(AbstractGame game) {
        this.game = game;
        this.gameId = game.getId();
    }

    public AbstractGame getGame() {
        return game;
    }

    public int getGameId() {
        return gameId;
    }
}
