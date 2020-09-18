package org.codedefenders.notification.events.client.registration;

import org.codedefenders.notification.events.server.test.TestCompiledEvent;
import org.codedefenders.notification.events.server.test.TestSubmittedEvent;
import org.codedefenders.notification.events.server.test.TestTestedMutantsEvent;
import org.codedefenders.notification.events.server.test.TestTestedOriginalEvent;
import org.codedefenders.notification.events.server.test.TestValidatedEvent;
import org.codedefenders.notification.handling.ClientEventHandler;

import com.google.gson.annotations.Expose;

/**
 * A message used to register for test progressbar events.
 * <br>
 * This includes:
 * <ul>
 * <li>{@link TestSubmittedEvent}</li>
 * <li>{@link TestValidatedEvent}</li>
 * <li>{@link TestCompiledEvent}</li>
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
