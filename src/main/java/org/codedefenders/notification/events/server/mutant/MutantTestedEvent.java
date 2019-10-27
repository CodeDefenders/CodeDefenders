package org.codedefenders.notification.events.server.mutant;

import com.google.gson.annotations.Expose;

public class MutantTestedEvent extends MutantLifecycleEvent {
    /* TODO: enable once the MutationTester (and similar) methods return a result instead of void.
    @Expose private boolean survived;
    @Expose private int numTests;
    @Expose private int numKilled;

    public boolean isSurvived() {
        return survived;
    }

    public int getNumTests() {
        return numTests;
    }

    public int getNumKilled() {
        return numKilled;
    }

    public void setSurvived(boolean survived) {
        this.survived = survived;
    }

    public void setNumTests(int numTests) {
        this.numTests = numTests;
    }

    public void setNumKilled(int numKilled) {
        this.numKilled = numKilled;
    }
    */
}
