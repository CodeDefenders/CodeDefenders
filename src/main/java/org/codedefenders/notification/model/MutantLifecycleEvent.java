package org.codedefenders.notification.model;

import org.codedefenders.game.Mutant;

/**
 * @author gambi
 */
public abstract class MutantLifecycleEvent {

    /*
     * This is a superset of TargetExecution.Target.Type unless we extend that class
     */
    private String eventType;
    private Mutant mutant;

    public MutantLifecycleEvent(Mutant mutant, String eventType) {
        this.mutant = mutant;
        this.eventType = eventType;
    }

    public String getEventType() {
        return eventType;
    }

    public Mutant getMutant() {
        return mutant;
    }
    
}
