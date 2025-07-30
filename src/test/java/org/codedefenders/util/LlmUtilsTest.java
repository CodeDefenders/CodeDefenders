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
package org.codedefenders.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmUtilsTest {

    @Test
    public void extractTestContentFromReplyTest() {
        String reply = """
                  import static org.junit.whatever;
                import static org.junit.Assert.assertEquals;
                public class ConstantsTest {
                    @Test
                    public void answer_is_42() {
                        int sure = ok this doesn't have to parse
                        assertEquals(42, Constants.answer);
                    }

                    @Test
                    public void foo_returns_21() {
                        assertEquals(21, new Constants().foo());
                    }
                }
                """;
        String expectedTestContent = """
                int sure = ok this doesn't have to parse
                assertEquals(42, Constants.answer);
                assertEquals(21, new Constants().foo());
                """.stripIndent();
        assertEquals(expectedTestContent, LlmUtils.extractTestContentFromReply(reply));
    }
}
