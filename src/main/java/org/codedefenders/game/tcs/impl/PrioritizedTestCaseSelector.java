package org.codedefenders.game.tcs.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.codedefenders.game.Test;
import org.codedefenders.game.tcs.ITestCaseSelector;

public abstract class PrioritizedTestCaseSelector implements ITestCaseSelector {

    public abstract List<Test> prioritize(List<Test> allTests);

    @Override
    public final List<Test> select(List<Test> allTests, int maxTests) {
        if (allTests.size() < 2) {
            return allTests.stream().limit(maxTests).collect(Collectors.toList());
        }
        return prioritize(allTests).stream().limit(maxTests).collect(Collectors.toList());
    }
}
