package org.codedefenders;

import org.junit.*;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by thoma on 07/04/2016.
 */
public class AntTest {
    @Ignore
    @Test
    public void testAntPath() {
        ProcessBuilder pb = new ProcessBuilder();

        Map env = pb.environment();

        String antHome = (String) env.get("ANT_HOME");

        assertNotNull("antHome is null", antHome);

        assertTrue("antHome has length of 0", antHome.length() > 0);
    }

    @Ignore
    @Test
    public void testAntSystemPath() {
        assertNotNull("ANT_HOME is null", System.getenv("ant.home"));

        assertTrue("ANT_HOME has a length of 0", System.getenv("AMT_HOME").length() > 0);
    }


}
