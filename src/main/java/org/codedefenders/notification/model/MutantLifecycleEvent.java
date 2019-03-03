package org.codedefenders.notification.model;

import org.codedefenders.game.Mutant;

/**
 * 
 * @author gambi
 *
 */
public class MutantLifecycleEvent {

    private String eventType;
    private Mutant mutant;

    public MutantLifecycleEvent(
            Mutant mutant,
            String eventType // This is a superset of TargetExecution.Target.Type unless we extend that class
            ) {
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
