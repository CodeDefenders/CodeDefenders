package org.codedefenders.notification.model;

import org.codedefenders.game.Test;

/**
 * 
 * @author gambi
 *
 */
public class TestLifecycleEvent {

    private String eventType;
    
    private Test test;

    public TestLifecycleEvent(
            Test test,
            String eventType // This is a superset of TargetExecution.Target.Type unless we extend that class
            ) {
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
