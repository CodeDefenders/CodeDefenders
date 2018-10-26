/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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
 * along with Code Defenders.  If not, see <http://www.gnu.org/licenses/>.
 */
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
    @Ignore @Test
    public void testAntPath(){
        ProcessBuilder pb = new ProcessBuilder();

        Map env = pb.environment();

        String antHome = (String) env.get("ANT_HOME");

        assertNotNull("antHome is null", antHome);

        assertTrue("antHome has length of 0", antHome.length() > 0);
    }

    @Ignore @Test
    public void testAntSystemPath(){
        assertNotNull("ANT_HOME is null", System.getenv("ant.home"));

        assertTrue("ANT_HOME has a length of 0", System.getenv("AMT_HOME").length() > 0);
    }


}
