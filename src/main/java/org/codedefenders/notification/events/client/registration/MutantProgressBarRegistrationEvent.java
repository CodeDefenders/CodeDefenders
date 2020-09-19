package org.codedefenders.notification.events.client.registration;

import org.codedefenders.notification.events.server.mutant.MutantCompiledEvent;
import org.codedefenders.notification.events.server.mutant.MutantSubmittedEvent;
import org.codedefenders.notification.events.server.mutant.MutantTestedEvent;
import org.codedefenders.notification.events.server.mutant.MutantValidatedEvent;
import org.codedefenders.notification.handling.ClientEventHandler;

import com.google.gson.annotations.Expose;

/**
 * A message used to register for mutant progressbar events.
 * <br>
 * This includes:
 * <ul>
 * <li>{@link MutantSubmittedEvent}</li>
 * <li>{@link MutantValidatedEvent}</li>
 * <li>{@link MutantCompiledEvent}</li>
 * <li>{@link MutantTestedEvent}</li>
 * </ul>
 */
public class MutantProgressBarRegistrationEvent extends RegistrationEvent {
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
