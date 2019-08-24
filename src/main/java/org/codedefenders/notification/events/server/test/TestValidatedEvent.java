package org.codedefenders.notification.events.server.test;

import com.google.gson.annotations.Expose;
import org.codedefenders.game.Test;

public class TestValidatedEvent extends TestLifecycleEvent {
    @Expose private boolean success;
    @Expose private String validationMessage;

    public TestValidatedEvent(Test test, boolean success, String validationMessage) {
        super(test);
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
