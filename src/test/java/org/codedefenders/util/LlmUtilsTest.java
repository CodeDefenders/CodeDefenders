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

import java.util.List;
import java.util.stream.Stream;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmUtilsTest {

    private static final String TEMPLATE = "class ExampleTest {\n"
            + "    void test() {\n        " + Constants.TEST_TEMPLATE_PLACEHOLDER + "\n    }\n}";

    private AbstractGame gameWithTemplate() {
        AbstractGame game = mock(AbstractGame.class);
        GameClass cut = mock(GameClass.class);
        when(game.getCUT()).thenReturn(cut);
        when(cut.getTestTemplate()).thenReturn(TEMPLATE);
        return game;
    }

    @ParameterizedTest
    @MethodSource("indentedTestBodies")
    void insertsTestBodyAtTemplateIndentation(String reply) {
        String expectedBody = "        int value = 1;\n"
                + "        if (value > 0) {\n            assertEquals(1, value);\n        }";
        assertEquals(TEMPLATE.replace("        " + Constants.TEST_TEMPLATE_PLACEHOLDER, expectedBody),
                LlmUtils.testTemplateFromReply(reply, gameWithTemplate()));
    }

    static Stream<String> indentedTestBodies() {
        String body = "int value = 1;\nif (value > 0) {\n    assertEquals(1, value);\n}";
        return Stream.of(body, body.indent(8),
                "class Reply {\n    @Test\n    void test() {\n" + body.indent(8) + "    }\n}");
    }

    @Test
    void indentsEveryTestInSuite() {
        String reply = "class Reply { void first() { int a = 1; int b = 2; } "
                + "void second() { if (true) { assertTrue(true); } } }";
        List<String> templates = LlmUtils.suiteOfTestTemplatesFromReply(reply, gameWithTemplate());
        assertEquals(List.of(
                TEMPLATE.replace("        " + Constants.TEST_TEMPLATE_PLACEHOLDER,
                        "        int a = 1;\n        int b = 2;"),
                TEMPLATE.replace("        " + Constants.TEST_TEMPLATE_PLACEHOLDER,
                        "        if (true) {\n            assertTrue(true);\n        }")), templates);
    }

    @Test
    void preservesTextBlockContentAndTrailingWhitespace() {
        String body = "        String value = \"\"\"\n"
                + "            first  \n          second\n            \"\"\";\n"
                + "        assertNotNull(value);";
        assertEquals(TEMPLATE.replace("        " + Constants.TEST_TEMPLATE_PLACEHOLDER, body),
                LlmUtils.testTemplateFromReply(body, gameWithTemplate()));
    }

    @ParameterizedTest
    @MethodSource("testSource")
    public void extractTestContentFromReplyTest(String reply) {
        String expectedTestContent = """
                int sure = 1;
                assertEquals(42, Constants.answer);
                assertEquals(21, new Constants().foo());""".stripIndent();
        assertEquals(expectedTestContent, LlmUtils.extractTestContentFromReply(reply).stripIndent());
    }

    @Test
    public void extractTestSuiteFromReply() {
        @Language("java")
        String reply = """
                public class Class {
                    String s = "toIgnore";
                    //comment to ignore
                    @Test
                    public void testA() {
                        int a = 0;
                        int b = 1;
                    }

                    public void testB()
                    {
                        if (true) {
                            int c = 2;
                        }
                        int d = 3;
                    }

                    public void testC()
                    {int e = 4; int f = 5;}
                }
                """;
        List<String> expectedResults = List.of(
                """
                        int a = 0;
                        int b = 1;
                        """,
                """
                        if (true) {
                            int c = 2;
                        }
                        int d = 3;
                        """,
                """
                        int e = 4;
                        int f = 5;
                        """
        );
        List<String> results = LlmUtils.multipleTestsFromReply(reply);
        assertEquals(expectedResults.size(), results.size());
        for (int i = 0; i < results.size(); i++) {
            assertEquals(expectedResults.get(i).stripIndent().strip(), results.get(i).stripIndent().strip());
        }
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

    private static Stream<String> testSource() {
        return Stream.of("""
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
                """,
                """
                int sure = 1;
                assertEquals(42, Constants.answer);
                assertEquals(21, new Constants().foo());""",
                """
                @Test
                    public void answer_is_42() {
                        int sure = 1;
                        assertEquals(42, Constants.answer);
                        assertEquals(21, new Constants().foo());
                    }
                """
                );
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
