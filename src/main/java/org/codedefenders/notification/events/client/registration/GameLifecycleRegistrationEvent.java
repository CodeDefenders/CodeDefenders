package org.codedefenders.notification.events.client.registration;

import org.codedefenders.notification.events.server.game.*;
import org.codedefenders.notification.handling.ClientEventHandler;

/**
 * A message used to register for game lifecycle events.
 * <br>
 * This includes:
 * <ul>
 * <li>{@link GameCreatedEvent}</li>
 * <li>{@link GameJoinedEvent}</li>
 * <li>{@link GameLeftEvent}</li>
 * <li>{@link GameStartedEvent}</li>
 * <li>{@link GameStoppedEvent}</li>
 * </ul>
 */
public class GameLifecycleRegistrationEvent extends RegistrationEvent {
    @Override
    public void accept(ClientEventHandler visitor) {
        visitor.visit(this);
    }
}
