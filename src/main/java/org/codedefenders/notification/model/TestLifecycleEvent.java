package org.codedefenders.notification.model;

import org.codedefenders.game.Test;

/**
 * 
 * @author gambi
 *
 */
public abstract class TestLifecycleEvent {
    /*
     * This is a superset of TargetExecution.Target.Type unless we extend that
     * class
     */
    private String eventType;
    private Test test;

    public TestLifecycleEvent(Test test, String eventType) {
        this.eventType = eventType;
        this.test = test;
    }

    public String getEventType() {
        return eventType;
    }

    public Test getTest() {
        return test;
    }
}
