package org.codedefenders.notification.events.client;

import org.codedefenders.notification.handling.client.ClientEventHandler;

/**
 * A message used to register for notifications events.
 * <ul>
 *     <li>Events specifies a list of events the websocket wants to register to / unregister from.</li>
 *     <li>Action is either {@code "register"} or {@code "unregister}" (maybe more options later?).</li>
 *     <li>Other parameters depend on the type of events.</li>
 * </ul>
 */
public class RegistrationEvent extends ClientEvent {
    /* Required. */
    private EventType type;
    private Action action;

    /* Optional, depending on the events. */
    private Integer gameId;
    private Integer playerId;
    private Integer userId;

    public RegistrationEvent(EventType type, Action action) {
        this.type = type;
        this.action = action;
    }

    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Action getAction() {
        return action;
    }

    public EventType getType() {
        return type;
    }

    public Integer getGameId() {
        return gameId;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public Integer getUserId() {
        return userId;
    }

    @Override
    public void accept(ClientEventHandler visitor) {
        throw new UnsupportedOperationException("Registration event is treated as a special event.");
    }

    public enum Action {
        REGISTER,
        UNREGISTER
    }

    public enum EventType {
        GAME,
        CHAT,
        PROGRESSBAR
    }
}
