package org.codedefenders.notification.events.server.mutant;

import com.google.gson.annotations.Expose;

public class MutantValidatedEvent extends MutantLifecycleEvent {
    @Expose private boolean success;
    @Expose private String validationMessage;

    public boolean isSuccess() {
        return success;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }
}
