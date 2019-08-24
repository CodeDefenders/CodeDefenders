package org.codedefenders.notification.events.server.test;

import com.google.gson.annotations.Expose;
import org.codedefenders.game.Test;
import org.codedefenders.notification.events.server.ServerEvent;

public abstract class TestLifecycleEvent extends ServerEvent {
    private Test test;
    @Expose private int testId;

    public TestLifecycleEvent(Test test) {
        this.test = test;
        this.testId = test.getId();
    }

    public Test getTest() {
        return test;
    }

    public int getTestId() {
        return testId;
    }
}
