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
package org.codedefenders.execution;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.regex.Matcher;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class AntProcessResultTest {

    @Test
    public void reduceJUnitOutputToEssential(){
        String fullJUnitOutput = ""
                + "[junit] Running TestLift" + "\n"
                + "[junit] Testsuite: TestLift" + "\n"
                + "[junit] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.025 sec" + "\n"
                + "[junit] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.025 sec" + "\n"
                + "[junit] Testcase: test took 0.004 sec" + "\n"
                + "[junit]         FAILED" + "\n"
                + "[junit] null" + "\n"
                + "[junit] junit.framework.AssertionFailedError" + "\n"
                + "[junit]         at TestLift.test(TestLift.java:9)" + "\n"
                + "[junit]         at java.util.concurrent.FutureTask.run(FutureTask.java:266)" + "\n"
                + "[junit]         at java.lang.Thread.run(Thread.java:748)" + "\n"
                + "[junit] Test TestLift FAILED";
        
        AntProcessResult result = new AntProcessResult();
        result.setInputStream( new BufferedReader( new StringReader( fullJUnitOutput ) ));
        //
        String reducedJunitOutput = result.getJUnitMessage();
        assertThat( reducedJunitOutput.split("\n").length, is( lessThan( fullJUnitOutput.split("\n").length)));
    }
}
