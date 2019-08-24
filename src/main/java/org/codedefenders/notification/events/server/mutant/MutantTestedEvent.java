package org.codedefenders.notification.events.server.mutant;

import com.google.gson.annotations.Expose;
import org.codedefenders.game.Mutant;

public class MutantTestedEvent extends MutantLifecycleEvent {
    @Expose private boolean survived;
    @Expose private int numTests;
    @Expose private int numKilled;

    public MutantTestedEvent(Mutant mutant, boolean survived, int numTests, int numKilled) {
        super(mutant);
        this.survived = survived;
        this.numTests = numTests;
        this.numKilled = numKilled;
    }

    public boolean isSurvived() {
        return survived;
    }

    public int getNumTests() {
        return numTests;
    }

    public int getNumKilled() {
        return numKilled;
    }
}
