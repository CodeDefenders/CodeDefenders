package org.codedefenders.notification.events.server.mutant;

import com.google.gson.annotations.Expose;
import org.codedefenders.game.Mutant;
import org.codedefenders.notification.events.server.ServerEvent;

public abstract class MutantLifecycleEvent extends ServerEvent {
    private Mutant mutant;
    @Expose private int mutantId;

    public MutantLifecycleEvent(Mutant mutant) {
        this.mutant = mutant;
        this.mutantId = mutant.getId();
    }

    public Mutant getMutant() {
        return mutant;
    }

    public int getMutantId() {
        return mutantId;
    }
}
