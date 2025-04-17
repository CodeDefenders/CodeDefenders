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

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Alternative;

import org.codedefenders.game.Test;
import org.codedefenders.game.tcs.ITestCaseSelector;

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
