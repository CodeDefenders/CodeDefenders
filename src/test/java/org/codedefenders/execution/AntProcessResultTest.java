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
