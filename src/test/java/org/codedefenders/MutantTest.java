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
package org.codedefenders;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;

import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MutantTest {

    @TempDir
    static Path tempDir;

    @Test
    public void testApplyPatch() throws IOException, PatchFailedException {
        List<String> originalCode = """
                public class Lift {
                    private int topFloor;
                    private int currentFloor = 0; // default
                    private int capacity = 10;    // default
                    private int numRiders = 0;    // default

                    public Lift(int highestFloor) {
                        topFloor = highestFloor;
                    }
                }""".stripIndent().lines().toList();

        List<String> mutantCode = """
                public class Lift {
                    private int topFloor;
                    private int currentFloor = 0; // default
                    private int capacity = 10;    // default
                    private int numRiders = 0;    // default

                    public Lift(int highestFloor) { topFloor = highestFloor;
                        topFloor = highestFloor;
                    }
                }""".stripIndent().lines().toList();

        // generating diff information.
        Patch<String> thePatch = DiffUtils.diff(originalCode, mutantCode);
        List<String> unifiedPatches = UnifiedDiffUtils.generateUnifiedDiff(null, null, originalCode, thePatch, 3);
        System.out.println("MutantTest.testApplyPatch() " + unifiedPatches);
        List<String> diff = """
                --- null
                +++ null
                @@ -4,7 +4,7 @@
                     private int capacity = 10;    // default
                     private int numRiders = 0;    // default

                -    public Lift(int highestFloor) {
                +    public Lift(int highestFloor) { topFloor = highestFloor;
                         topFloor = highestFloor;
                     }
                 }""".stripIndent().lines().toList();

        Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(diff);

        // Reapply the patch
        List<String> patchedCode = DiffUtils.patch(originalCode, patch);
        assertEquals(mutantCode, patchedCode);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @ArgumentsSource(MutantTestArguments.class)
    public void test(String originalCode, String mutantCode, Consumer<Mutant> assertions, String name)
            throws IOException {
        File cutJavaFile = tempDir.resolve("original.java").toFile();
        FileUtils.writeStringToFile(cutJavaFile, originalCode, StandardCharsets.UTF_8);

        File mutantJavaFile = tempDir.resolve("mutant.java").toFile();
        FileUtils.writeStringToFile(mutantJavaFile, mutantCode, StandardCharsets.UTF_8);

        GameClass mockedGameClass = mock(GameClass.class);

        int mockedClassId = 1;
        int mockedGameId = 1;
        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());

        Mutant mutant = new Mutant(mockedGameId, mockedClassId, mutantJavaFile.getAbsolutePath(), null, true, 1, 2) {
            @Override
            protected GameClass getCUT() {
                return mockedGameClass;
            }
        };
        assertions.accept(mutant);
    }

    public static class MutantTestArguments implements ArgumentsProvider {
        private Arguments testGetLinesForChangeSingleLine() {
            String originalCode = """
                public class Lift {
                    private int topFloor;
                    private int currentFloor = 0; // default
                    private int capacity = 10;    // default
                    private int numRiders = 0;    // default

                    public Lift(int highestFloor) {
                        topFloor = highestFloor;
                    }
                }""".stripIndent();

            String mutantCode = """
                public class Lift {
                    private int topFloor;
                    private int currentFloor = 0; // default
                    private int capacity = 10;    // default
                    private int numRiders = 0;    // default

                    public Lift(int highestFloor) { topFloor = highestFloor;
                        topFloor = highestFloor;
                    }
                }""".stripIndent();

            Consumer<Mutant> assertions = mutant -> {
                Patch<String> patch = mutant.getDifferences();
                assertEquals(1, patch.getDeltas().size());
                assertEquals(Arrays.asList(7), mutant.getLines());
            };

            return Arguments.of(originalCode, mutantCode, assertions);
        }

        private Arguments testGetLinesForChangeMultipleLines() {
            String originalCode = """
                    public class Lift {
                        private int topFloor;
                        private int currentFloor = 0; // default
                        private int capacity = 10;    // default
                        private int numRiders = 0;    // default

                        public Lift(int highestFloor) {
                            topFloor = highestFloor;
                        }
                    }""".stripIndent();

            String mutantCode = """
                    public class Lift {
                        private int topFloor;
                        private int currentFloor = 0; // default
                        private int capacity = 10;    // default
                        private int numRiders = 0;    // default

                        public Lift(int highestFloor) { topFloor =


                            highestFloor;
                            topFloor = highestFloor;
                        }
                    }""".stripIndent();

            Consumer<Mutant> assertions = mutant -> {
                Patch<String> patch = mutant.getDifferences();
                assertEquals(1, patch.getDeltas().size());
                assertEquals(Arrays.asList(7), mutant.getLines());
            };

            return Arguments.of(originalCode, mutantCode, assertions);
        }

        private Arguments testGetLinesForInsertSingeLine() {
            String originalCode = """
                    public class Lift {
                        private int topFloor;
                        private int currentFloor = 0; // default
                        private int capacity = 10;    // default
                        private int numRiders = 0;    // default

                        public Lift(int highestFloor) {
                            topFloor = highestFloor;
                        }
                    }""".stripIndent();

            String mutantCode = """
                    public class Lift {
                        private int topFloor;
                        private int currentFloor = 0; // default
                        private int capacity = 10;    // default
                        private int numRiders = 0;    // default

                        public Lift(int highestFloor) {
                            topFloor = highestFloor;
                            topFloor = highestFloor;
                        }
                    }""".stripIndent();

            Consumer<Mutant> assertions = mutant -> {
                Patch<String> patch = mutant.getDifferences();
                assertEquals(1, patch.getDeltas().size());
                assertEquals(Arrays.asList(9), mutant.getLines());
            };

            return Arguments.of(originalCode, mutantCode, assertions);
        }

        private Arguments testGetLinesForInsertMultipleLines() {
            String originalCode = """
                    public class Lift {
                        private int topFloor;
                        private int currentFloor = 0; // default
                        private int capacity = 10;    // default
                        private int numRiders = 0;    // default

                        public Lift(int highestFloor) {
                            topFloor = highestFloor;
                        }
                    }""".stripIndent();

            String mutantCode = """
                    public class Lift {
                        private int topFloor;
                        private int currentFloor = 0; // default
                        private int capacity = 10;    // default
                        private int numRiders = 0;    // default

                        public Lift(int highestFloor) {
                            topFloor = highestFloor + 1;
                            topFloor = highestFloor + 1;
                            topFloor = highestFloor;
                        }
                    }""".stripIndent();

            Consumer<Mutant> assertions = mutant -> {
                Patch<String> patch = mutant.getDifferences();
                assertEquals(1, patch.getDeltas().size());
                assertEquals(Arrays.asList(8), mutant.getLines());
            };

            return Arguments.of(originalCode, mutantCode, assertions);
        }

        private Arguments testGetLinesForChangeLineAndInsertMultipleLines() {
            String originalCode = """
                     public class Lift {
                         private int topFloor;
                         private int currentFloor = 0; // default
                         private int capacity = 10;    // default
                         private int numRiders = 0;    // default

                         public Lift(int highestFloor) {
                             topFloor = highestFloor;
                         }
                     }""".stripIndent();

            String mutantCode = """
                    public class Lift {
                        private int topFloor;
                        private int currentFloor = 0; // default
                        private int capacity = 10;    // default
                        private int numRiders = 0;    // default

                        public Lift(int highestFloor) { topFloor = 0;
                            topFloor = highestFloor;
                            topFloor = highestFloor + 1;
                            topFloor = highestFloor + 1;
                        }
                    }""".stripIndent();

            Consumer<Mutant> assertions = mutant -> {
                Patch<String> patch = mutant.getDifferences();
                assertEquals(2, patch.getDeltas().size()); // Change and insertion
                assertEquals(2, mutant.getLines().size());  // Change line, and line of insertion
                assertEquals(Arrays.asList(7, 9), mutant.getLines());
            };

            return Arguments.of(originalCode, mutantCode, assertions);
        }

        private Arguments testGetLinesForInsertionMutantOnDisjointLines() {
            String originalCode = """
                    public class Lift {
                        private int topFloor;
                        private int currentFloor = 0; // default
                        private int capacity = 10;    // default
                        private int numRiders = 0;    // default

                        public Lift(int highestFloor) {
                            topFloor = highestFloor;
                        }

                        public Lift(int highestFloor, int maxRiders) {
                            this(highestFloor);
                            capacity = maxRiders;
                        }
                    }""".stripIndent();

            String mutantCode = """
                    public class Lift {
                        private int topFloor;
                        private int currentFloor = 0; // default
                        private int capacity = 10;    // default
                        private int numRiders = 0;    // default

                        public Lift(int highestFloor) {
                            topFloor = highestFloor;
                            topFloor = highestFloor;
                        }

                        public Lift(int highestFloor, int maxRiders) {
                            this(highestFloor);
                            topFloor = highestFloor;
                            capacity = maxRiders;
                        }
                    }""".stripIndent();

            Consumer<Mutant> assertions = mutant -> {
                Patch<String> patch = mutant.getDifferences();
                assertEquals(2, patch.getDeltas().size()); // Change and insertion
                assertEquals(Arrays.asList(9, 13), mutant.getLines());
            };

            return Arguments.of(originalCode, mutantCode, assertions);
        }

        private Arguments testGetLinesForEmptySpaces() {
            String originalCode = """
                    public String toString(int doubleLength) {
                        StringBuffer temp = new StringBuffer();
                        temp.append(trim(real, doubleLength));
                        if (imag < 0.0) {
                            temp.append(" - ");
                            temp.append(trim(-imag, doubleLength));
                            temp.append(" i");
                        } else {
                            temp.append(" + ");
                            temp.append(trim(imag, doubleLength));
                            temp.append(" i");
                        }
                        return temp.toString();
                    }""".stripIndent();

            String mutantCode = """
                    public String toString(int doubleLength) {
                        StringBuffer temp = new StringBuffer();
                        temp.append(trim(real, doubleLength));
                        if (imag < 0.0) {
                            temp.append(" - ");
                            temp.append(trim(-imag, doubleLength));
                            temp.append(" i   ");
                        } else {
                            temp.append(" + ");
                            temp.append(trim(imag, doubleLength));
                            temp.append(" i");
                        }
                        return temp.toString();
                    }""".stripIndent();

            Consumer<Mutant> assertions = mutant -> {
                Patch<String> p = mutant.getDifferences();
                assertEquals(1, p.getDeltas().size());
            };

            return Arguments.of(originalCode, mutantCode, assertions);
        }

        private Arguments testGetLinesForEmptySpacesOutsideStrings() {
            String originalCode = """
                    public String toString(int doubleLength) {
                        StringBuffer temp = new StringBuffer();
                        temp.append(trim(real, doubleLength));
                        if (imag < 0.0) {
                            temp.append(" - ");
                            temp.append(trim(-imag, doubleLength));
                            temp.append(" i");
                        } else {
                            temp.append(" + ");
                            temp.append(trim(imag, doubleLength));
                            temp.append(" i");
                        }
                        return temp.toString();
                    }""".stripIndent();

            String mutantCode = """
                    public String toString(int doubleLength) {
                        StringBuffer temp = new StringBuffer();
                        temp.append(trim(real, doubleLength));
                        if (imag < 0.0) {
                            temp.append(" - ");
                            temp.append(trim(-imag, doubleLength));
                            temp.append(" i");
                        } else {
                            temp.append(" + ");\s\s\s\s
                            temp.append(trim(imag, doubleLength));
                            temp.append(" i");
                        }
                        return temp.toString();
                    }""".stripIndent();

            Consumer<Mutant> assertions = mutant -> {
                Patch<String> patch = mutant.getDifferences();
                assertEquals(0, patch.getDeltas().size());
            };

            return Arguments.of(originalCode, mutantCode, assertions);
        }

        /**
         * Test if whitespace changes are discarded when they are not adjacent to a non-whitespace change.
         * In this case, the whitespace changes are in a different chunk of the diff than the non-whitespace change.
         */
        private Arguments testIfNonAdjacentSingleLineWhitespaceChangesAreFilteredOutForPatchString() {
            String originalCode = """
                        line 1
                        line 2
                        line 3
                        line 4
                        line 5
                    }""".stripIndent();

            String mutantCode = """
                          line   1
                        line 2
                        line changed
                        line 4
                          line   5
                    }""".stripIndent();

            Consumer<Mutant> assertions = mutant -> {
                var expected = """
                    --- /dev/null
                    +++ /dev/null
                    @@ -0,6 +1,6 @@
                         line 1
                         line 2
                    -    line 3
                    +    line changed
                         line 4
                         line 5
                     }
                    """.stripIndent();
                assertEquals(expected, mutant.getPatchString());
            };

            return Arguments.of(originalCode, mutantCode, assertions);
        }

        /**
         * Test if whitespace deletions are discarded when they are not adjacent to a non-whitespace change.
         * In this case, the whitespace changes are in a different chunk of the diff than the non-whitespace change.
         */
        private Arguments testIfNonAdjacentTrailingSingleLineWhitespaceDeletionsAreFilteredOutForPatchString() {
            String originalCode = """
                        line 1 x
                        line 2
                        line 3
                        line 4
                        line 5 x
                    }""".stripIndent().replace("x", "");

            String mutantCode = """
                        line 1
                        line 2
                        line changed
                        line 4
                        line 5
                    }""".stripIndent();

            Consumer<Mutant> assertions = mutant -> {
                var expected = """
                    --- /dev/null
                    +++ /dev/null
                    @@ -0,6 +1,6 @@
                         line 1 x
                         line 2
                    -    line 3
                    +    line changed
                         line 4
                         line 5 x
                     }
                    """.stripIndent().replaceAll("x", "");
                assertEquals(expected, mutant.getPatchString());
            };

            return Arguments.of(originalCode, mutantCode, assertions);
        }

        /**
         * Test if whitespace additions are discarded when they are adjacent to a non-whitespace change.
         * In this case, the whitespace changes are in the same chunk of the diff as the non-whitespace change.
         */
        private Arguments testIfAdjacentSingleLineWhitespaceChangesAreFilteredOutForPatchString() {
            String originalCode = """
                        line 1
                        line 2
                        line 3
                        line 4
                        line 5
                    }""".stripIndent();

            String mutantCode = """
                        line 1
                          line   2
                        line changed
                          line   4
                        line 5
                    }""".stripIndent();

            Consumer<Mutant> assertions = mutant -> {
                var expected = """
                    --- /dev/null
                    +++ /dev/null
                    @@ -0,6 +1,6 @@
                         line 1
                         line 2
                    -    line 3
                    +    line changed
                         line 4
                         line 5
                     }
                    """.stripIndent();
                assertEquals(expected, mutant.getPatchString());
            };

            return Arguments.of(originalCode, mutantCode, assertions);
        }

        /**
         * Test if whitespace deletions are discarded when they are adjacent to a non-whitespace change.
         * In this case, the whitespace changes are in the same chunk of the diff as the non-whitespace change.
         */
        private Arguments testIfAdjacentTrailingSingleLineWhitespaceDeletionsAreFilteredOutForPatchString() {
            String originalCode = """
                        line 1
                        line 2 x
                        line 3
                        line 4 x
                        line 5
                    }""".stripIndent().replaceAll("x", "");

            String mutantCode = """
                        line 1
                        line 2
                        line changed
                        line 4
                        line 5
                    }""".stripIndent();

            Consumer<Mutant> assertions = mutant -> {
                var expected = """
                    --- /dev/null
                    +++ /dev/null
                    @@ -0,6 +1,6 @@
                         line 1
                         line 2 x
                    -    line 3
                    +    line changed
                         line 4 x
                         line 5
                     }
                    """.stripIndent().replaceAll("x", "");
                assertEquals(expected, mutant.getPatchString());
            };

            return Arguments.of(originalCode, mutantCode, assertions);
        }

        /**
         * Test if whitespace changes inside of strings are preserved.
         */
        private Arguments testIfSingleLineWhitespaceChangesInStringsAreFilteredNotOutForPatchString() {
            String originalCode = """
                        "line 1"
                        "line 2"
                        "line 3"
                        "line 4"
                        "line 5"
                    }""".stripIndent();

            String mutantCode = """
                          "line   1"
                        "line 2"
                        "line changed"
                          "line   4"
                        "line 5"
                    }""".stripIndent();

            Consumer<Mutant> assertions = mutant -> {
                var expected = """
                    --- /dev/null
                    +++ /dev/null
                    @@ -1,6 +1,6 @@
                    -    "line 1"
                    +      "line   1"
                         "line 2"
                    -    "line 3"
                    -    "line 4"
                    +    "line changed"
                    +      "line   4"
                         "line 5"
                     }
                    """.stripIndent();
                assertEquals(expected, mutant.getPatchString());
            };

            return Arguments.of(originalCode, mutantCode, assertions);
        }

        private Arguments testIfWhitespaceDelimitersAreNotFilteredOutForPatchString() {
            String originalCode = """
                        private final Example e = new Example(5);
                    }""".stripIndent();

            // final now part of class name, newExample method call instead of constructor
            String mutantCode = """
                        private finalExample e = newExample(5);
                    }""".stripIndent();

            Consumer<Mutant> assertions = mutant -> {
                var expected = """
                        --- /dev/null
                        +++ /dev/null
                        @@ -0,2 +1,2 @@
                        -    private final Example e = new Example(5);
                        +    private finalExample e = newExample(5);
                         }
                        """.stripIndent();
                // assertEquals(expected, mutant.getPatchString());
                assertNotEquals(expected, mutant.getPatchString());
            };

            return Arguments.of(originalCode, mutantCode, assertions);
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
            return Arrays.stream(getClass().getDeclaredMethods())
                .filter(method -> method.getReturnType().equals(Arguments.class))
                .filter(method -> !method.isSynthetic())
                .map(method -> {
                    try {
                        method.setAccessible(true);
                        var args = ((Arguments) method.invoke(this)).get();
                        var newArgs = Arrays.copyOf(args, args.length + 1);
                        newArgs[args.length] = method.getName();
                        return Arguments.of(newArgs);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        }
    }
}
