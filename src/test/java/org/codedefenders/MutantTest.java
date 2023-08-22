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
package org.codedefenders;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MutantTest {

    @TempDir
    static Path tempDir;

    // Required for mocking Configuration, which is loaded into a static field of FileUtils, required by GameClass.
    @Rule
    public WeldInitiator weld = WeldInitiator.of(MutantTest.class);

    @Produces
    public Configuration produceConfiguration() {
        return new Configuration() {};
    }

    @Test
    public void testApplyPatch() throws IOException, PatchFailedException {
        List<String> originalCode = Arrays.asList("public class Lift {", "private int topFloor;",
                "private int currentFloor = 0; // default", "private int capacity = 10;    // default",
                "private int numRiders = 0;    // default", "public Lift(int highestFloor) { ",
                "topFloor = highestFloor;", "}", "}");

        List<String> mutantCode = Arrays.asList("public class Lift {", "private int topFloor;",
                "private int currentFloor = 0; // default", "private int capacity = 10;    // default",
                "private int numRiders = 0;    // default", "public Lift(int highestFloor) { topFloor = highestFloor;", // Here's the change
                "topFloor = highestFloor;", "}", "}");

        // generating diff information.
        Patch<String> thePatch = DiffUtils.diff(originalCode, mutantCode);
        List<String> unifiedPatches = UnifiedDiffUtils.generateUnifiedDiff(null, null, originalCode, thePatch, 3);
        System.out.println("MutantTest.testApplyPatch() " + unifiedPatches);
        List<String> diff = Arrays.asList("--- null", "+++ null", "@@ -3,7 +3,7 @@",
                " private int currentFloor = 0; // default", " private int capacity = 10;    // default",
                " private int numRiders = 0;    // default", "-public Lift(int highestFloor) { ",
                "+public Lift(int highestFloor) { topFloor = highestFloor;", " topFloor = highestFloor;", " }", " }");

        Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(diff);

        // Reapply the patch
        List<String> patchedCode = DiffUtils.patch(originalCode, patch);
        assertEquals(mutantCode, patchedCode);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ArgumentsSource(MutantTestArguments.class)
    public void test(String name, String originalCode, String mutantCode, Consumer<Mutant> assertions)
            throws IOException {
        File cutJavaFile = tempDir.resolve("original.java").toFile();
        FileUtils.writeStringToFile(cutJavaFile, originalCode, StandardCharsets.UTF_8);

        File mutantJavaFile = tempDir.resolve("mutant.java").toFile();
        FileUtils.writeStringToFile(mutantJavaFile, mutantCode, StandardCharsets.UTF_8);

        GameClass mockedGameClass = mock(GameClass.class);
        MultiplayerGame mockedGame = mock(MultiplayerGame.class);

        int mockedClassId = 1;
        int mockedGameId = 1;
        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());

        try (var mockedMultiDAO = mockStatic(MultiplayerGameDAO.class);
             var mockedClassDAO = mockStatic(GameClassDAO.class)) {
            mockedMultiDAO.when(() -> MultiplayerGameDAO.getMultiplayerGame(mockedGameId))
                    .thenReturn(mockedGame);
            mockedClassDAO.when(() -> GameClassDAO.getClassForId(mockedClassId))
                    .thenReturn(mockedGameClass);

            Mutant mutant = new Mutant(mockedGameId, mockedClassId, mutantJavaFile.getAbsolutePath(), null, true, 1, 2);
            assertions.accept(mutant);
        }
    }

    public static class MutantTestArguments implements ArgumentsProvider {
        private Arguments testGetLinesForChangeSingleLine() {
            String originalCode = "public class Lift {" + "\n"
                    + "private int topFloor;" + "\n"
                    + "private int currentFloor = 0; // default" + "\n"
                    + "private int capacity = 10;    // default" + "\n"
                    + "private int numRiders = 0;    // default" + "\n"
                    + "\n"
                    + "public Lift(int highestFloor) {" + "\n"
                    + "topFloor = highestFloor;" + "\n"
                    + "}" + "\n"
                    + "}";

            String mutantCode = "public class Lift {" + "\n"
                    + "private int topFloor;" + "\n"
                    + "private int currentFloor = 0; // default" + "\n"
                    + "private int capacity = 10;    // default" + "\n"
                    + "private int numRiders = 0;    // default" + "\n"
                    + "\n"
                    + "public Lift(int highestFloor) {  topFloor = highestFloor;" + "\n" // 7 -Change this
                    + "topFloor = highestFloor;" + "\n"
                    + "}" + "\n"
                    + "}";

            Consumer<Mutant> assertions = mutant -> {
                Patch<String> patch = mutant.getDifferences();
                assertEquals(1, patch.getDeltas().size());
                assertEquals(Arrays.asList(7), mutant.getLines());
            };

            return Arguments.of("testGetLinesForChangeSingleLine", originalCode, mutantCode, assertions);
        }

        private Arguments testGetLinesForChangeMultipleLines() {
            String originalCode = "public class Lift {" + "\n"
                    + "private int topFloor;" + "\n"
                    + "private int currentFloor = 0; // default" + "\n"
                    + "private int capacity = 10;    // default" + "\n"
                    + "private int numRiders = 0;    // default" + "\n"
                    + "\n"
                    + "public Lift(int highestFloor) {" + "\n"
                    + "topFloor = highestFloor;" + "\n"
                    + "}" + "\n"
                    + "}";

            String mutantCode = "public class Lift {" + "\n"
                    + "private int topFloor;" + "\n"
                    + "private int currentFloor = 0; // default" + "\n"
                    + "private int capacity = 10;    // default" + "\n"
                    + "private int numRiders = 0;    // default" + "\n"
                    + "\n"
                    + "public Lift(int highestFloor) {  topFloor = " + "\n" // Change lines 7 - 10
                    + "" + "\n"
                    + "" + "\n"
                    + "highestFloor;" + "\n"
                    + "topFloor = highestFloor;" + "\n"
                    + "}" + "\n"
                    + "}";

            Consumer<Mutant> assertions = mutant -> {
                Patch<String> patch = mutant.getDifferences();
                assertEquals(1, patch.getDeltas().size());
                assertEquals(Arrays.asList(7), mutant.getLines());
            };

            return Arguments.of("testGetLinesForChangeMultipleLines", originalCode, mutantCode, assertions);
        }

        private Arguments testGetLinesForInsertSingeLine() {
            String originalCode = "public class Lift {" + "\n"
                    + "private int topFloor;" + "\n"
                    + "private int currentFloor = 0; // default" + "\n"
                    + "private int capacity = 10;    // default" + "\n"
                    + "private int numRiders = 0;    // default" + "\n"
                    + "\n"
                    + "public Lift(int highestFloor) {" + "\n"
                    + "topFloor = highestFloor;" + "\n"
                    + "}" + "\n"
                    + "}";

            String mutantCode = "public class Lift {" + "\n"
                    + "private int topFloor;" + "\n"
                    + "private int currentFloor = 0; // default" + "\n"
                    + "private int capacity = 10;    // default" + "\n"
                    + "private int numRiders = 0;    // default" + "\n"
                    + "\n"
                    + "public Lift(int highestFloor) {" + "\n"
                    + "topFloor = highestFloor;" + "\n" // 8 - Add this
                    + "topFloor = highestFloor;" + "\n"
                    + "}" + "\n"
                    + "}";

            Consumer<Mutant> assertions = mutant -> {
                Patch<String> patch = mutant.getDifferences();
                assertEquals(1, patch.getDeltas().size());
                assertEquals(Arrays.asList(9), mutant.getLines());
            };

            return Arguments.of("testGetLinesForInsertSingeLine", originalCode, mutantCode, assertions);
        }

        private Arguments testGetLinesForInsertMultipleLines() {
            String originalCode = "public class Lift {" + "\n"
                    + "private int topFloor;" + "\n"
                    + "private int currentFloor = 0; // default" + "\n"
                    + "private int capacity = 10;    // default" + "\n"
                    + "private int numRiders = 0;    // default" + "\n"
                    + "\n"
                    + "public Lift(int highestFloor) {" + "\n"
                    + "topFloor = highestFloor;" + "\n"
                    + "}" + "\n"
                    + "}";

            String mutantCode = "public class Lift {" + "\n"
                    + "private int topFloor;" + "\n"
                    + "private int currentFloor = 0; // default" + "\n"
                    + "private int capacity = 10;    // default" + "\n"
                    + "private int numRiders = 0;    // default" + "\n"
                    + "\n"
                    + "public Lift(int highestFloor) {" + "\n"
                    + "topFloor = highestFloor + 1;" + "\n" // 8 - Add this
                    + "topFloor = highestFloor + 1;" + "\n" // 9 - Add this
                    + "topFloor = highestFloor;" + "\n"
                    + "}" + "\n"
                    + "}";

            Consumer<Mutant> assertions = mutant -> {
                Patch<String> patch = mutant.getDifferences();
                assertEquals(1, patch.getDeltas().size());
                assertEquals(Arrays.asList(8), mutant.getLines());
            };

            return Arguments.of("testGetLinesForInsertMultipleLines", originalCode, mutantCode, assertions);
        }

        private Arguments testGetLinesForChangeLineAndInsertMultipleLines() {
            String originalCode = "public class Lift {" + "\n"
                    + "private int topFloor;" + "\n"
                    + "private int currentFloor = 0; // default" + "\n"
                    + "private int capacity = 10;    // default" + "\n"
                    + "private int numRiders = 0;    // default" + "\n"
                    + "\n"
                    + "public Lift(int highestFloor) {" + "\n"
                    + "topFloor = highestFloor;" + "\n"
                    + "}" + "\n"
                    + "}";

            String mutantCode = "public class Lift {" + "\n"
                    + "private int topFloor;" + "\n"
                    + "private int currentFloor = 0; // default" + "\n"
                    + "private int capacity = 10;    // default" + "\n"
                    + "private int numRiders = 0;    // default" + "\n"
                    + "\n"
                    + "public Lift(int highestFloor) { topFloor = 0; " + "\n" // 7 - Change
                    + "topFloor = highestFloor;" + "\n"
                    + "topFloor = highestFloor + 1;" + "\n" // 9 - Insert
                    + "topFloor = highestFloor + 1;" + "\n" // 10 - Insert
                    + "}" + "\n"
                    + "}";

            Consumer<Mutant> assertions = mutant -> {
                Patch<String> patch = mutant.getDifferences();
                assertEquals(2, patch.getDeltas().size()); // Change and insertion
                assertEquals(2, mutant.getLines().size());  // Change line, and line of insertion
                assertEquals(Arrays.asList(7, 9), mutant.getLines());
            };

            return Arguments.of("testGetLinesForChangeLineAndInsertMultipleLines", originalCode, mutantCode, assertions);
        }

        private Arguments testGetLinesForInsertionMutantOnDisjointLines() {
            String originalCode = "public class Lift {" + "\n"
                    + "private int topFloor;" + "\n"
                    + "private int currentFloor = 0; // default" + "\n"
                    + "private int capacity = 10;    // default" + "\n"
                    + "private int numRiders = 0;    // default" + "\n"
                    + "\n"
                    + "public Lift(int highestFloor) {" + "\n"
                    + "topFloor = highestFloor;" + "\n"
                    + "}" + "\n"
                    + "\n"
                    + "public Lift(int highestFloor, int maxRiders) {" + "\n"
                    + "this(highestFloor);" + "\n"
                    + "capacity = maxRiders;" + "\n"
                    + "}" + "\n"
                    + "}";

            String mutantCode = "public class Lift {" + "\n"
                    + "private int topFloor;" + "\n"
                    + "private int currentFloor = 0; // default" + "\n"
                    + "private int capacity = 10;    // default" + "\n"
                    + "private int numRiders = 0;    // default" + "\n"
                    + "\n"
                    + "public Lift(int highestFloor) {" + "\n"
                    + "topFloor = highestFloor;" + "\n"
                    + "topFloor = highestFloor;" + "\n" // Add line
                    + "}" + "\n"
                    + "\n"
                    + "public Lift(int highestFloor, int maxRiders) {" + "\n"
                    + "this(highestFloor);" + "\n"
                    + "topFloor = highestFloor;" + "\n" // Add line
                    + "capacity = maxRiders;" + "\n"
                    + "}" + "\n"
                    + "}";

            Consumer<Mutant> assertions = mutant -> {
                Patch<String> patch = mutant.getDifferences();
                assertEquals(2, patch.getDeltas().size()); // Change and insertion
                assertEquals(Arrays.asList(9, 13), mutant.getLines());
            };

            return Arguments.of("testGetLinesForInsertionMutantOnDisjointLines", originalCode, mutantCode, assertions);
        }

        private Arguments testGetLinesForEmptySpaces() {
            String originalCode = "public String toString(int doubleLength) {" + "\n"
                    + "StringBuffer temp = new StringBuffer();" + "\n"
                    + "temp.append(trim(real, doubleLength));" + "\n"
                    + "if(imag < 0.0) {" + "temp.append(\" - \");" + "\n"
                    + "temp.append(trim(-imag, doubleLength));" + "\n"
                    + "temp.append(\" i\");" + "} else {" + "\n"
                    + "temp.append(\" + \");" + "\n"
                    + "temp.append(trim(imag, doubleLength));" + "\n"
                    + "temp.append(\" i\");" + "\n"
                    + "}" + "\n"
                    + "return temp.toString();}";

            String mutantCode = "public String toString(int doubleLength) {" + "\n"
                    + "StringBuffer temp = new StringBuffer();" + "\n"
                    + "temp.append(trim(real, doubleLength));" + "\n"
                    + "if(imag < 0.0) {" + "temp.append(\" - \");" + "\n"
                    + "temp.append(trim(-imag, doubleLength));" + "\n"
                    + "temp.append(\" i   \");" + "} else {" + "\n"
                    + "temp.append(\" + \");" + "\n"
                    + "temp.append(trim(imag, doubleLength));" + "\n"
                    + "temp.append(\" i\");" + "\n"
                    + "}" + "\n"
                    + "return temp.toString();}";

            Consumer<Mutant> assertions = mutant -> {
                Patch<String> p = mutant.getDifferences();
                assertEquals(1, p.getDeltas().size());
            };

            return Arguments.of("testGetLinesForEmptySpaces", originalCode, mutantCode, assertions);
        }

        private Arguments testGetLinesForEmptySpacesOutsideStrings() {
            String originalCode = "public String toString(int doubleLength) {" + "\n"
                    + "StringBuffer temp = new StringBuffer();" + "\n"
                    + "temp.append(trim(real, doubleLength));" + "\n"
                    + "if(imag < 0.0) {" + "temp.append(\" - \");" + "\n"
                    + "temp.append(trim(-imag, doubleLength));" + "\n"
                    + "temp.append(\" i\");" + "} else {" + "\n"
                    + "temp.append(\" + \");" + "\n"
                    + "temp.append(trim(imag, doubleLength));" + "\n"
                    + "temp.append(\" i\");" + "\n"
                    + "}" + "\n"
                    + "return temp.toString();}";

            String mutantCode = "public String toString(int doubleLength) {" + "\n"
                    + "StringBuffer temp = new StringBuffer();" + "\n"
                    + "temp.append(trim(real, doubleLength));" + "\n"
                    + "if(imag < 0.0) {" + "temp.append(\" - \");" + "\n"
                    + "temp.append(trim(-imag, doubleLength));" + "\n"
                    + "temp.append(\" i\");" + "} else {" + "\n"
                    + "temp.append(\" + \");    " + "\n" // Add random spaces here. Those should be trimmed
                    + "temp.append(trim(imag, doubleLength));" + "\n"
                    + "temp.append(\" i\");" + "\n"
                    + "}" + "\n"
                    + "return temp.toString();}";

            Consumer<Mutant> assertions = mutant -> {
                Patch<String> patch = mutant.getDifferences();
                assertEquals(0, patch.getDeltas().size());
            };

            return Arguments.of("testGetLinesForEmptySpacesOutsideStrings", originalCode, mutantCode, assertions);
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
            return Stream.of(
                    testGetLinesForChangeSingleLine(),
                    testGetLinesForChangeMultipleLines(),
                    testGetLinesForInsertSingeLine(),
                    testGetLinesForInsertMultipleLines(),
                    testGetLinesForChangeLineAndInsertMultipleLines(),
                    testGetLinesForInsertionMutantOnDisjointLines(),
                    testGetLinesForEmptySpaces(),
                    testGetLinesForEmptySpacesOutsideStrings()
            );
        }
    }
}
