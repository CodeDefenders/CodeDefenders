/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.api.analytics;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;

import org.codedefenders.testsmells.ConfigurableEagerTest;

import testsmell.AbstractSmell;
import testsmell.TestSmellDetector;
import testsmell.smell.AssertionRoulette;
import testsmell.smell.DuplicateAssert;
import testsmell.smell.RedundantAssertion;
import testsmell.smell.SensitiveEquality;
import testsmell.smell.UnknownTest;

public class TestSmellDetectorProducer {

    // Enable
    public static final int EAGER_TEST_THRESHOLD = 4;
    
    // See https://stackoverflow.com/questions/2264758/resolution-of-external-3rd-party-beans-in-weld
    public @Produces @RequestScoped TestSmellDetector createTestSmellDetector() {
        List<AbstractSmell> testSmells = new ArrayList<AbstractSmell>();
        testSmells.add(new AssertionRoulette());
        testSmells.add(new DuplicateAssert());
        testSmells.add(new RedundantAssertion());
        testSmells.add(new SensitiveEquality());
        testSmells.add(new UnknownTest());
        // Those two might require some love according to #426.
        // testSmells.add(new ExceptionCatchingThrowing());
        // Disabled as per #426, waiting for #500
        // testSmells.add(new MagicNumberTest());
        testSmells.add(new ConfigurableEagerTest(EAGER_TEST_THRESHOLD));
        /*
         * Those two are not mentioned on:
         * https://testsmells.github.io/pages/testsmells.html but might become
         * relevant later
         */
        // testSmells.add(new VerboseTest());
        // testSmells.add(new DependentTest());
        return new TestSmellDetector(testSmells);
      }
}
