package org.codedefenders.notification.events.server.mutant;

import com.google.gson.annotations.Expose;
import org.codedefenders.game.Mutant;

public class MutantValidatedEvent extends MutantLifecycleEvent {
    @Expose private boolean success;
    @Expose private String validationMessage;

    public MutantValidatedEvent(Mutant mutant, boolean success, String validationMessage) {
        super(mutant);
        this.success = success;
        this.validationMessage = validationMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getValidationMessage() {
        return validationMessage;
    }
}
