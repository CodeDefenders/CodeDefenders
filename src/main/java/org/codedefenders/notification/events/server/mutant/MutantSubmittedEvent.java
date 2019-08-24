package org.codedefenders.notification.events.server.mutant;

import org.codedefenders.game.Mutant;

public class MutantSubmittedEvent extends MutantLifecycleEvent {
    public MutantSubmittedEvent(Mutant mutant) {
        super(mutant);
    }
}
