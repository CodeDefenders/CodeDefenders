package org.codedefenders.notification.events.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.codedefenders.game.AbstractGame;

/**
 * Higher level LifeCycleEvent, by subscribing to this type of event one can
 * receive ALL sort of life cycle events (i.e., subclasses)
 *
 * @author gambi
 */
public abstract class GameLifecycleEvent extends ServerEvent {
    private AbstractGame game;

    public GameLifecycleEvent(AbstractGame game) {
        this.game = game;
    }

    public AbstractGame getGame() {
        return game;
    }

    // TODO: what information is needed?
    @Override
    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("gameId", game.getId());
        return obj;
    }
}
