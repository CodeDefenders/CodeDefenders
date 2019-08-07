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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import difflib.PatchFailedException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GameDAO.class, GameClassDAO.class, MultiplayerGame.class})
public class MutantTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @org.junit.Test
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
        Patch thePatch = DiffUtils.diff(originalCode, mutantCode);
        List<String> unifiedPatches = DiffUtils.generateUnifiedDiff(null, null, originalCode, thePatch, 3);
        System.out.println("MutantTest.testApplyPatch() " + unifiedPatches);
        List<String> diff = Arrays.asList("--- null", "+++ null", "@@ -3,7 +3,7 @@",
                " private int currentFloor = 0; // default", " private int capacity = 10;    // default",
                " private int numRiders = 0;    // default", "-public Lift(int highestFloor) { ",
                "+public Lift(int highestFloor) { topFloor = highestFloor;", " topFloor = highestFloor;", " }", " }");

        Patch patch = DiffUtils.parseUnifiedDiff(diff);

        // Reapply the patch
        List<String> patchedCode = (List<String>) DiffUtils.patch(originalCode, patch);
        assertEquals(mutantCode, patchedCode);
    }

    @Test
    public void exploratoryTest() {
        List<Integer> mutatedLines = new ArrayList<>();
        String s = StringUtils.join(mutatedLines, ",");
        System.out.println("MutantTest.exploratoryTest() " + s);

    }

    @Test
    public void testGetLinesForChangeSingleLine() throws IOException {
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

        File cutJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(cutJavaFile, originalCode);
        //
        File mutantJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(mutantJavaFile, mutantCode);

        GameClass mockedGameClass = mock(GameClass.class);
        MultiplayerGame mockedGame = mock(MultiplayerGame.class);

        int mockedClassID = 1;
        int mockedGameID = 1;

        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());
        when(mockedGameClass.getId()).thenReturn(mockedClassID);
        when(mockedGame.getClassId()).thenReturn(mockedClassID);
        when(mockedGame.getId()).thenReturn(mockedGameID);

        PowerMockito.mockStatic(MultiplayerGameDAO.class);
        when(MultiplayerGameDAO.getMultiplayerGame(mockedGameID)).thenReturn(mockedGame);
        when(mockedGame.getClassId()).thenReturn(1);
        PowerMockito.mockStatic(GameClassDAO.class);
        when(GameClassDAO.getClassForId(mockedClassID)).thenReturn(mockedGameClass);
        PowerMockito.mockStatic(GameDAO.class);
        when(GameDAO.getCurrentRound(mockedGameID)).thenReturn(2);
        //
        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getAbsolutePath());
        //
        Mutant m = new Mutant(mockedGameID, mockedClassID, mutantJavaFile.getAbsolutePath(), null, true, 1);

        Patch p = m.getDifferences();

        assertEquals(1, p.getDeltas().size());
        assertEquals(Arrays.asList(7), m.getLines());

        for (Delta d : p.getDeltas()) {
            System.out.println("MutantTest.testGetLinesForInsertionMutant() " + d);
        }
    }

    @Test
    public void testGetLinesForChangeMultipleLines() throws IOException {
        String originalCode =
                "public class Lift {" + "\n"
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
                + "" + "\n" //
                + "" + "\n" //
                + "highestFloor;" + "\n" //
                + "topFloor = highestFloor;" + "\n"
                + "}" + "\n"
                + "}";

        File cutJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(cutJavaFile, originalCode);
        //
        File mutantJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(mutantJavaFile, mutantCode);

        GameClass mockedGameClass = mock(GameClass.class);
        MultiplayerGame mockedGame = mock(MultiplayerGame.class);

        int mockedClassID = 1;
        int mockedGameID = 1;

        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());
        when(mockedGameClass.getId()).thenReturn(mockedClassID);
        when(mockedGame.getClassId()).thenReturn(mockedClassID);
        when(mockedGame.getId()).thenReturn(mockedGameID);

        PowerMockito.mockStatic(MultiplayerGameDAO.class);
        PowerMockito.mockStatic(GameClassDAO.class);
        when(MultiplayerGameDAO.getMultiplayerGame(1)).thenReturn(mockedGame);
        when(mockedGame.getClassId()).thenReturn(1);
        when(GameClassDAO.getClassForId(mockedClassID)).thenReturn(mockedGameClass);
        PowerMockito.mockStatic(GameDAO.class);
        when(GameDAO.getCurrentRound(mockedGameID)).thenReturn(2);
        //
        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getAbsolutePath());
        //
        Mutant m = new Mutant(mockedGameID, mockedGameClass.getId(), mutantJavaFile.getAbsolutePath(), null, true, 1);

        Patch p = m.getDifferences();

        assertEquals(1, p.getDeltas().size());
        assertEquals(Arrays.asList(7), m.getLines());
    }

    @Test
    public void testGetLinesForInsertSingeLine() throws IOException {
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

        File cutJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(cutJavaFile, originalCode);
        //
        File mutantJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(mutantJavaFile, mutantCode);

        GameClass mockedGameClass = mock(GameClass.class);
        MultiplayerGame mockedGame = mock(MultiplayerGame.class);

        int mockedClassID = 1;
        int mockedGameID = 1;

        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());
        when(mockedGameClass.getId()).thenReturn(mockedClassID);
        when(mockedGame.getClassId()).thenReturn(mockedClassID);
        when(mockedGame.getId()).thenReturn(mockedGameID);

        PowerMockito.mockStatic(MultiplayerGameDAO.class);
        PowerMockito.mockStatic(GameClassDAO.class);
        when(MultiplayerGameDAO.getMultiplayerGame(1)).thenReturn(mockedGame);
        when(mockedGame.getClassId()).thenReturn(1);
        when(GameClassDAO.getClassForId(mockedClassID)).thenReturn(mockedGameClass);
        PowerMockito.mockStatic(GameDAO.class);
        when(GameDAO.getCurrentRound(mockedGameID)).thenReturn(2);
        //
        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getAbsolutePath());
        //
        Mutant m = new Mutant(mockedGameID, mockedGameClass.getId(), mutantJavaFile.getAbsolutePath(), null, true, 1);

        Patch p = m.getDifferences();

        assertEquals(1, p.getDeltas().size());
        assertEquals(Arrays.asList(9), m.getLines());
    }

    @Test
    public void testGetLinesForInsertMultipleLines() throws IOException {
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

        File cutJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(cutJavaFile, originalCode);
        //
        File mutantJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(mutantJavaFile, mutantCode);

        GameClass mockedGameClass = mock(GameClass.class);
        MultiplayerGame mockedGame = mock(MultiplayerGame.class);

        int mockedClassID = 1;
        int mockedGameID = 1;

        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());
        when(mockedGameClass.getId()).thenReturn(mockedClassID);
        when(mockedGame.getClassId()).thenReturn(mockedClassID);
        when(mockedGame.getId()).thenReturn(mockedGameID);

        PowerMockito.mockStatic(MultiplayerGameDAO.class);
        PowerMockito.mockStatic(GameClassDAO.class);
        when(MultiplayerGameDAO.getMultiplayerGame(1)).thenReturn(mockedGame);
        when(mockedGame.getClassId()).thenReturn(1);
        when(GameClassDAO.getClassForId(mockedClassID)).thenReturn(mockedGameClass);
        PowerMockito.mockStatic(GameDAO.class);
        when(GameDAO.getCurrentRound(mockedGameID)).thenReturn(2);
        //
        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getAbsolutePath());
        //
        Mutant m = new Mutant(mockedGameID, mockedGameClass.getId(), mutantJavaFile.getAbsolutePath(), null, true, 1);

        Patch p = m.getDifferences();

        assertEquals(1, p.getDeltas().size());
        assertEquals(Arrays.asList(8), m.getLines());

        System.out.println("MutantTest.testGetLinesForInsertMultipleLines()" + m.getHTMLReadout());
    }

    @Test
    public void testGetLinesForChangeLineAndInsertMultipleLines() throws IOException {
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

        File cutJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(cutJavaFile, originalCode);
        //
        File mutantJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(mutantJavaFile, mutantCode);

        GameClass mockedGameClass = mock(GameClass.class);
        MultiplayerGame mockedGame = mock(MultiplayerGame.class);

        int mockedClassID = 1;
        int mockedGameID = 1;

        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());
        when(mockedGameClass.getId()).thenReturn(mockedClassID);
        when(mockedGame.getClassId()).thenReturn(mockedClassID);
        when(mockedGame.getId()).thenReturn(mockedGameID);

        PowerMockito.mockStatic(MultiplayerGameDAO.class);
        PowerMockito.mockStatic(GameClassDAO.class);
        when(MultiplayerGameDAO.getMultiplayerGame(1)).thenReturn(mockedGame);
        when(GameClassDAO.getClassForId(mockedClassID)).thenReturn(mockedGameClass);
        PowerMockito.mockStatic(GameDAO.class);
        when(GameDAO.getCurrentRound(mockedGameID)).thenReturn(2);
        //
        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getAbsolutePath());
        //
        Mutant m = new Mutant(mockedGameID, mockedGameClass.getId(), mutantJavaFile.getAbsolutePath(), null, true, 1);

        Patch p = m.getDifferences();

        assertEquals(2, p.getDeltas().size()); // Change and insertion
        assertEquals(2, m.getLines().size());  // Change line, and line of insertion
        assertEquals(Arrays.asList(7, 9), m.getLines());

        System.out.println("MutantTest.testGetLinesForInsertMultipleLines()" + m.getHTMLReadout());
    }

    @Test
    public void testGetLinesForInsertionMutantOnDisjointLines() throws IOException {
//		int classId = MultiplayerGameDAO.getMultiplayerGame(gameId).getClassId();
//		GameClass sut = MultiplayerGameDAO.getClassForId(classId);

        // Mock the class provide a temmp sourceFile with original content in it
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

        File cutJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(cutJavaFile, originalCode);
        //
        File mutantJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(mutantJavaFile, mutantCode);

        GameClass mockedGameClass = mock(GameClass.class);
        MultiplayerGame mockedGame = mock(MultiplayerGame.class);

        int mockedClassID = 1;
        int mockedGameID = 1;

        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());
        when(mockedGameClass.getId()).thenReturn(mockedClassID);
        when(mockedGame.getClassId()).thenReturn(mockedClassID);
        when(mockedGame.getId()).thenReturn(mockedGameID);

        PowerMockito.mockStatic(MultiplayerGameDAO.class);
        PowerMockito.mockStatic(GameClassDAO.class);
        when(MultiplayerGameDAO.getMultiplayerGame(1)).thenReturn(mockedGame);
        when(mockedGame.getClassId()).thenReturn(1);
        when(GameClassDAO.getClassForId(mockedClassID)).thenReturn(mockedGameClass);
        PowerMockito.mockStatic(GameDAO.class);
        when(GameDAO.getCurrentRound(mockedGameID)).thenReturn(2);
        //
        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getAbsolutePath());
        //
        Mutant m = new Mutant(mockedGameID, mockedGameClass.getId(), mutantJavaFile.getAbsolutePath(), null, true, 1);

        System.out.println("MutantTest.testGetLinesForInsertionMutant() Lines " + m.getLines());
        System.out.println("MutantTest.testGetLinesForInsertionMutant() Lines " + m.getHTMLReadout());

        Patch p = m.getDifferences();

        assertEquals(2, p.getDeltas().size()); // Change and insertion
        assertEquals(Arrays.asList(9, 13), m.getLines());

        for (Delta d : p.getDeltas()) {
            System.out.println("MutantTest.testGetLinesForInsertionMutant() " + d);
        }
    }

    @Test
    public void testGetLinesForEmptySpaces() throws IOException {
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

        File cutJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(cutJavaFile, originalCode);
        //
        File mutantJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(mutantJavaFile, mutantCode);

        GameClass mockedGameClass = mock(GameClass.class);
        MultiplayerGame mockedGame = mock(MultiplayerGame.class);

        int mockedClassID = 1;
        int mockedGameID = 1;

        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());
        when(mockedGameClass.getId()).thenReturn(mockedClassID);
        when(mockedGame.getClassId()).thenReturn(mockedClassID);
        when(mockedGame.getId()).thenReturn(mockedGameID);

        PowerMockito.mockStatic(MultiplayerGameDAO.class);
        PowerMockito.mockStatic(GameClassDAO.class);
        when(MultiplayerGameDAO.getMultiplayerGame(1)).thenReturn(mockedGame);
        when(mockedGame.getClassId()).thenReturn(1);
        when(GameClassDAO.getClassForId(mockedClassID)).thenReturn(mockedGameClass);
        PowerMockito.mockStatic(GameDAO.class);
        when(GameDAO.getCurrentRound(mockedGameID)).thenReturn(2);
        //
        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getAbsolutePath());
        //
        Mutant m = new Mutant(mockedGameID, mockedGameClass.getId(), mutantJavaFile.getAbsolutePath(), null, true, 1);

        Patch p = m.getDifferences();

        assertEquals(1, p.getDeltas().size());
//		assertEquals(Arrays.asList(7), m.getLines());

        for (Delta d : p.getDeltas()) {
            System.out.println("MutantTest.testGetLinesForInsertionMutant() " + d);
        }
    }

    @Test
    public void testGetLinesForEmptySpacesOutsideStrings() throws IOException {
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

        File cutJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(cutJavaFile, originalCode);
        //
        File mutantJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(mutantJavaFile, mutantCode);

        GameClass mockedGameClass = mock(GameClass.class);
        MultiplayerGame mockedGame = mock(MultiplayerGame.class);

        int mockedClassID = 1;
        int mockedGameID = 1;

        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());
        when(mockedGameClass.getId()).thenReturn(mockedClassID);
        when(mockedGame.getClassId()).thenReturn(mockedClassID);
        when(mockedGame.getId()).thenReturn(mockedGameID);

        PowerMockito.mockStatic(MultiplayerGameDAO.class);
        PowerMockito.mockStatic(GameClassDAO.class);
        when(MultiplayerGameDAO.getMultiplayerGame(1)).thenReturn(mockedGame);
        when(mockedGame.getClassId()).thenReturn(1);
        when(GameClassDAO.getClassForId(mockedClassID)).thenReturn(mockedGameClass);
        PowerMockito.mockStatic(GameDAO.class);
        when(GameDAO.getCurrentRound(mockedGameID)).thenReturn(2);
        //
        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getAbsolutePath());
        //
        Mutant m = new Mutant(mockedGameID, mockedGameClass.getId(), mutantJavaFile.getAbsolutePath(), null, true, 1);

        Patch p = m.getDifferences();

        assertEquals(0, p.getDeltas().size());
    }
}
