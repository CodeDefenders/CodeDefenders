package org.codedefenders.notification.events.server.mutant;

import com.google.gson.annotations.Expose;

public class MutantCompiledEvent extends MutantLifecycleEvent {
    @Expose private boolean success;
    @Expose private String errorMessage;

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
