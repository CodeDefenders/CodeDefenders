package org.codedefenders.notification.events.server;

import org.codedefenders.game.Mutant;

/**
 * @author gambi
 */
/* Types of mutant lifecycle events are a superset of TargetExecution.Target.Type
 * TODO: extend this class */
public abstract class MutantLifecycleEvent {
    private Mutant mutant;

    public MutantLifecycleEvent(Mutant mutant) {
        this.mutant = mutant;
    }

    public Mutant getMutant() {
        return mutant;
    }
}
