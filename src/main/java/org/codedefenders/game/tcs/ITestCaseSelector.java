package org.codedefenders.game.tcs;

import java.util.List;

import org.codedefenders.game.Test;

public interface ITestCaseSelector {

    List<Test> select(List<Test> allTests, int maxTests);

}
