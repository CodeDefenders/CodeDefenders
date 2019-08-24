package org.codedefenders.notification.events.client.registration;

import com.google.gson.annotations.Expose;
import org.codedefenders.notification.events.server.test.*;
import org.codedefenders.notification.handling.ClientEventHandler;

/**
 * A message used to register for test progressbar events.
 * <br>
 * This includes:
 * <ul>
 * <li>{@link TestSubmittedEvent}</li>
 * <li>{@link TestCompiledEvent}</li>
 * <li>{@link TestValidatedEvent}</li>
 * <li>{@link TestTestedOriginalEvent}</li>
 * <li>{@link TestTestedMutantsEvent}</li>
 * </ul>
 */
public class TestProgressBarRegistrationEvent extends RegistrationEvent {
    @Expose private int gameId;

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    @Override
    public void accept(ClientEventHandler visitor) {
        visitor.visit(this);
    }
}
