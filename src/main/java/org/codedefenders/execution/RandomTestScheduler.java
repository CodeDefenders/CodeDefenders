package org.codedefenders.execution;

import org.codedefenders.game.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RandomTestScheduler implements TestScheduler {

    @Override
    public List<Test> scheduleTests(Collection<Test> tests) {
        // Shuffle tests
        List<Test> randomSchedule = new ArrayList<>(tests);
        Collections.shuffle(randomSchedule);
        return randomSchedule;
    }
}
