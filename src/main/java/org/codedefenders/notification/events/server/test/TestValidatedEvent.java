package org.codedefenders.notification.events.server.test;

import com.google.gson.annotations.Expose;

public class TestValidatedEvent extends TestLifecycleEvent {

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
