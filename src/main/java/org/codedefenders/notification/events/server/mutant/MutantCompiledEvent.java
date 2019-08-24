package org.codedefenders.notification.events.server.mutant;

import com.google.gson.annotations.Expose;
import org.codedefenders.game.Mutant;

public class MutantCompiledEvent extends MutantLifecycleEvent {
    @Expose private boolean success;
    @Expose private String errorMessage;

    public MutantCompiledEvent(Mutant mutant, boolean success, String errorMessage) {
        super(mutant);
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
