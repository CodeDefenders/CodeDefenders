/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
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
