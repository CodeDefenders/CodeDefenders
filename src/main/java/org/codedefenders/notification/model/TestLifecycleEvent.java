package org.codedefenders.notification.model;

import org.codedefenders.game.Test;

/**
 * @author gambi
 */
/* Types of test lifecycle events are a superset of TargetExecution.Target.Type
 * TODO: extend this class */
public abstract class TestLifecycleEvent extends PushEvent {
    private Test test;

    public TestLifecycleEvent(Test test) {
        this.test = test;
    }

    public Test getTest() {
        return test;
    }
}
