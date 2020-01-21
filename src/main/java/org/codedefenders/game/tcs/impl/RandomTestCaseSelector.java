package org.codedefenders.game.tcs.impl;

import org.codedefenders.game.Test;
import org.codedefenders.game.tcs.ITestCaseSelector;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.inject.Alternative;

/**
 * Return a number of tests selected randomly.
 *
 * @author gambi
 */
@Alternative // Avoid Weld to look it up and complain
public class RandomTestCaseSelector implements ITestCaseSelector {

    @Override
    public List<Test> select(List<Test> allTests, int maxTests) {
        Collections.shuffle(allTests, new SecureRandom());
        return allTests.stream().limit(maxTests).collect(Collectors.toList());
    }
}
