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

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
                        int sure = 1;
                        assertEquals(42, Constants.answer);
                    }

                    @Test
                    public void foo_returns_21() {
                        assertEquals(21, new Constants().foo());
                    }
                }
                """;
        String expectedTestContent = """
                int sure = 1;
                assertEquals(42, Constants.answer);
                assertEquals(21, new Constants().foo());""".stripIndent();
        assertEquals(expectedTestContent, LlmUtils.extractTestContentFromReply(reply).stripIndent());
    }

    @ParameterizedTest
    @MethodSource("mutantSource")
    public void extractMutantContentFromReplyTest(String reply) {
        String expected = """
                public class MultipleClasses {
                    int foo() {
                        return 1;
                    }
                }
                """.stripIndent().strip();
        assertEquals(expected, LlmUtils.extractMutantFromReply(reply.stripIndent().strip(), "Bar"));
    }


    private static Stream<String> mutantSource() {
        return Stream.of("""
                        ```java
                        public class MultipleClasses {
                            int foo() {
                                return 1;
                            }
                        }
                        class Bar {
                            int bar() {
                                return 2;
                            }
                        }
                        ```

                        """,
                """
                         ```java
                        public class MultipleClasses {
                            int foo() {
                                return 1;
                            }
                        }
                        public class Bar {
                            int bar() {
                                return 2;
                            }
                        }
                        ```
                        """,
                """
                         ```java
                        public class MultipleClasses {
                            int foo() {
                                return 1;
                            }
                        }
                        public interface Bar {
                            int bar() {
                                return 2;
                            }
                        }
                        ```
                        """,
                """

                         ```java
                        public class MultipleClasses {
                            int foo() {
                                return 1;
                            }
                        }
                        public class
                        Bar {
                            int bar() {
                                return 2;
                            }
                        }
                        ```
                        """,
                """
                         ```java
                        public class MultipleClasses {
                            int foo() {
                                return 1;
                            }
                        }
                        ```
                        """);
    }
}
