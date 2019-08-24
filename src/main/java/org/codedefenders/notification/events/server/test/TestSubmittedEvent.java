package org.codedefenders.notification.events.server.test;

import org.codedefenders.game.Test;

public class TestSubmittedEvent extends TestLifecycleEvent {
    public TestSubmittedEvent(Test test) {
        super(test);
    }
}
