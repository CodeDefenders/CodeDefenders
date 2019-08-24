package org.codedefenders.notification.events.server;

import com.google.gson.annotations.Expose;

public class KillEvent {
    @Expose private int mutantId;
    @Expose private int testId;
    // TODO: What other attributes?

    public KillEvent(int mutantId, int testId) {
        this.mutantId = mutantId;
        this.testId = testId;
    }

    public int getMutantId() {
        return mutantId;
    }

    public int getTestId() {
        return testId;
    }
}
