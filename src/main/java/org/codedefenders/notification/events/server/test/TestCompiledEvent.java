package org.codedefenders.notification.events.server.test;

import com.google.gson.annotations.Expose;
import org.codedefenders.game.Test;

public class TestCompiledEvent extends TestLifecycleEvent {
    @Expose private boolean success;
    @Expose private String errorMessage;

    public TestCompiledEvent(Test test, boolean success, String errorMessage) {
        super(test);
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
