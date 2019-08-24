package org.codedefenders.notification.events.server.test;

import com.google.gson.annotations.Expose;
import org.codedefenders.game.Test;

public class TestTestedMutantsEvent extends TestLifecycleEvent {
    @Expose private int numMutants;
    @Expose private int numKilled;

    public TestTestedMutantsEvent(Test test, int numMutants, int numKilled) {
        super(test);
        this.numMutants = numMutants;
        this.numKilled = numKilled;
    }

    public int getNumMutants() {
        return numMutants;
    }

    public int getNumKilled() {
        return numKilled;
    }
}
