package org.codedefenders.notification.events.client;

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
    private String[] events;
    private Action action;

    /* Optional, depending on the events. */
    private Integer gameId;
    private Integer playerId;

    public RegistrationEvent(String[] events, Action action) {
        this.events = events;
        this.action = action;
    }

    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public Action getAction() {
        return action;
    }

    public String[] getEvents() {
        return events;
    }

    public Integer getGameId() {
        return gameId;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public enum Action {
        REGISTER,
        UNREGISTER
    }
}
