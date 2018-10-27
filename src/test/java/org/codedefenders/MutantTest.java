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
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.duel.DuelGame;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import difflib.Delta;
import difflib.Patch;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatabaseAccess.class})
public class MutantTest {
	
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();
	
	@org.junit.Test
	public void testGetLinesForChangeSingleLine() throws IOException{
		String originalCode = "public class Lift {"+ "\n"
				+ "private int topFloor;"+ "\n"
				+ "private int currentFloor = 0; // default"+ "\n"
				+ "private int capacity = 10;    // default"+ "\n"
				+ "private int numRiders = 0;    // default"+ "\n"
				+ "\n"
				+ "public Lift(int highestFloor) {"+ "\n"
				+ "topFloor = highestFloor;"+ "\n"
				+ "}"+ "\n"
				+ "}";
		
		String mutantCode = "public class Lift {"+ "\n"
				+ "private int topFloor;"+ "\n"
				+ "private int currentFloor = 0; // default"+ "\n"
				+ "private int capacity = 10;    // default"+ "\n"
				+ "private int numRiders = 0;    // default"+ "\n"
				+ "\n"
				+ "public Lift(int highestFloor) {  topFloor = highestFloor;"+ "\n" // 7 -Change this
				+ "topFloor = highestFloor;"+ "\n"
				+ "}"+ "\n"
				+ "}";
		
		File cutJavaFile = temporaryFolder.newFile();
		FileUtils.writeStringToFile(cutJavaFile, originalCode);
		//
		File mutantJavaFile = temporaryFolder.newFile();
		FileUtils.writeStringToFile(mutantJavaFile , mutantCode);
		
		GameClass mockedGameClass = mock(GameClass.class);
		DuelGame mockedDualGame = mock(DuelGame.class);
		//
		when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());

		PowerMockito.mockStatic(DatabaseAccess.class);
		when(DatabaseAccess.getGameForKey("ID", 1)).thenReturn( mockedDualGame);
		when(mockedDualGame.getClassId()).thenReturn(1);
		when(DatabaseAccess.getClassForKey("Class_ID", 1)).thenReturn( mockedGameClass );
		//
		when( mockedGameClass.getJavaFile()).thenReturn( cutJavaFile.getAbsolutePath() );
		//
		Mutant m = new Mutant(1, mutantJavaFile.getAbsolutePath(), null, true, 1);
		
		Patch p = m.getDifferences();

		assertEquals(1, p.getDeltas().size());
		assertEquals(Arrays.asList(7), m.getLines());

		for (Delta d : p.getDeltas()) {
			System.out.println("MutantTest.testGetLinesForInsertionMutant() " + d);
		}
	}
	
	@org.junit.Test
	public void testGetLinesForChangeMutlipleLines() throws IOException{
		String originalCode = 
				"public class Lift {"+ "\n"
				+ "private int topFloor;"+ "\n"
				+ "private int currentFloor = 0; // default"+ "\n"
				+ "private int capacity = 10;    // default"+ "\n"
				+ "private int numRiders = 0;    // default"+ "\n"
				+ "\n"
				+ "public Lift(int highestFloor) {"+ "\n"
				+ "topFloor = highestFloor;"+ "\n"
				+ "}"+ "\n"
				+ "}";
		
		String mutantCode = "public class Lift {"+ "\n"
				+ "private int topFloor;"+ "\n"
				+ "private int currentFloor = 0; // default"+ "\n"
				+ "private int capacity = 10;    // default"+ "\n"
				+ "private int numRiders = 0;    // default"+ "\n"
				+ "\n"
				+ "public Lift(int highestFloor) {  topFloor = " + "\n" // Change lines 7 - 10 
				+ ""+ "\n" // 
				+ ""+ "\n" // 
				+ "highestFloor;"+ "\n" //
				+ "topFloor = highestFloor;"+ "\n"
				+ "}"+ "\n"
				+ "}";
		
		File cutJavaFile = temporaryFolder.newFile();
		FileUtils.writeStringToFile(cutJavaFile, originalCode);
		//
		File mutantJavaFile = temporaryFolder.newFile();
		FileUtils.writeStringToFile(mutantJavaFile , mutantCode);
		
		GameClass mockedGameClass = mock(GameClass.class);
		DuelGame mockedDualGame = mock(DuelGame.class);
		//
		when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());

		PowerMockito.mockStatic(DatabaseAccess.class);
		when(DatabaseAccess.getGameForKey("ID", 1)).thenReturn( mockedDualGame);
		when(mockedDualGame.getClassId()).thenReturn(1);
		when(DatabaseAccess.getClassForKey("Class_ID", 1)).thenReturn( mockedGameClass );
		//
		when( mockedGameClass.getJavaFile()).thenReturn( cutJavaFile.getAbsolutePath() );
		//
		Mutant m = new Mutant(1, mutantJavaFile.getAbsolutePath(), null, true, 1);
	
		Patch p = m.getDifferences();
		
		assertEquals(1, p.getDeltas().size());
		assertEquals(Arrays.asList(7), m.getLines());
	}
	
	
	@org.junit.Test
	public void testGetLinesForInsertSingeLine() throws IOException{
		String originalCode = "public class Lift {"+ "\n"
				+ "private int topFloor;"+ "\n"
				+ "private int currentFloor = 0; // default"+ "\n"
				+ "private int capacity = 10;    // default"+ "\n"
				+ "private int numRiders = 0;    // default"+ "\n"
				+ "\n"
				+ "public Lift(int highestFloor) {"+ "\n"
				+ "topFloor = highestFloor;"+ "\n"
				+ "}"+ "\n"
				+ "}";
		
		String mutantCode = "public class Lift {"+ "\n"
				+ "private int topFloor;"+ "\n"
				+ "private int currentFloor = 0; // default"+ "\n"
				+ "private int capacity = 10;    // default"+ "\n"
				+ "private int numRiders = 0;    // default"+ "\n"
				+ "\n"
				+ "public Lift(int highestFloor) {" + "\n"
				+ "topFloor = highestFloor;"+ "\n" // 8 - Add this
				+ "topFloor = highestFloor;"+ "\n"
				+ "}"+ "\n"
				+ "}";
		
		File cutJavaFile = temporaryFolder.newFile();
		FileUtils.writeStringToFile(cutJavaFile, originalCode);
		//
		File mutantJavaFile = temporaryFolder.newFile();
		FileUtils.writeStringToFile(mutantJavaFile , mutantCode);
		
		GameClass mockedGameClass = mock(GameClass.class);
		DuelGame mockedDualGame = mock(DuelGame.class);
		//
		when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());

		PowerMockito.mockStatic(DatabaseAccess.class);
		when(DatabaseAccess.getGameForKey("ID", 1)).thenReturn( mockedDualGame);
		when(mockedDualGame.getClassId()).thenReturn(1);
		when(DatabaseAccess.getClassForKey("Class_ID", 1)).thenReturn( mockedGameClass );
		//
		when( mockedGameClass.getJavaFile()).thenReturn( cutJavaFile.getAbsolutePath() );
//		

		//
		Mutant m = new Mutant(1, mutantJavaFile.getAbsolutePath(), null, true, 1);
		
		Patch p = m.getDifferences();
		
		assertEquals(1, p.getDeltas().size());
		assertEquals(Arrays.asList(9), m.getLines());
	}
	
	@org.junit.Test
	public void testGetLinesForInsertMultipleLines() throws IOException{
		String originalCode = "public class Lift {"+ "\n"
				+ "private int topFloor;"+ "\n"
				+ "private int currentFloor = 0; // default"+ "\n"
				+ "private int capacity = 10;    // default"+ "\n"
				+ "private int numRiders = 0;    // default"+ "\n"
				+ "\n"
				+ "public Lift(int highestFloor) {"+ "\n"
				+ "topFloor = highestFloor;"+ "\n"
				+ "}"+ "\n"
				+ "}";
		
		String mutantCode = "public class Lift {"+ "\n"
				+ "private int topFloor;"+ "\n"
				+ "private int currentFloor = 0; // default"+ "\n"
				+ "private int capacity = 10;    // default"+ "\n"
				+ "private int numRiders = 0;    // default"+ "\n"
				+ "\n"
				+ "public Lift(int highestFloor) {" + "\n"
				+ "topFloor = highestFloor + 1;"+ "\n" // 8 - Add this
				+ "topFloor = highestFloor + 1;"+ "\n" // 9 - Add this
				+ "topFloor = highestFloor;"+ "\n"
				+ "}"+ "\n"
				+ "}";
		
		File cutJavaFile = temporaryFolder.newFile();
		FileUtils.writeStringToFile(cutJavaFile, originalCode);
		//
		File mutantJavaFile = temporaryFolder.newFile();
		FileUtils.writeStringToFile(mutantJavaFile , mutantCode);
		
		GameClass mockedGameClass = mock(GameClass.class);
		DuelGame mockedDualGame = mock(DuelGame.class);
		//
		when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());

		PowerMockito.mockStatic(DatabaseAccess.class);
		when(DatabaseAccess.getGameForKey("ID", 1)).thenReturn( mockedDualGame);
		when(mockedDualGame.getClassId()).thenReturn(1);
		when(DatabaseAccess.getClassForKey("Class_ID", 1)).thenReturn( mockedGameClass );
		//
		when( mockedGameClass.getJavaFile()).thenReturn( cutJavaFile.getAbsolutePath() );
//		

		//
		Mutant m = new Mutant(1, mutantJavaFile.getAbsolutePath(), null, true, 1);
		
		Patch p = m.getDifferences();
		
		assertEquals(1, p.getDeltas().size());
		assertEquals(Arrays.asList(8), m.getLines());

		System.out.println("MutantTest.testGetLinesForInsertMultipleLines()" + m.getHTMLReadout() );
	}
	
	@org.junit.Test
	public void testGetLinesForChangeLineAndInsertMultipleLines() throws IOException{
		String originalCode = "public class Lift {"+ "\n"
				+ "private int topFloor;"+ "\n"
				+ "private int currentFloor = 0; // default"+ "\n"
				+ "private int capacity = 10;    // default"+ "\n"
				+ "private int numRiders = 0;    // default"+ "\n"
				+ "\n"
				+ "public Lift(int highestFloor) {"+ "\n"
				+ "topFloor = highestFloor;"+ "\n"
				+ "}"+ "\n"
				+ "}";
		
		String mutantCode = "public class Lift {"+ "\n"
				+ "private int topFloor;"+ "\n"
				+ "private int currentFloor = 0; // default"+ "\n"
				+ "private int capacity = 10;    // default"+ "\n"
				+ "private int numRiders = 0;    // default"+ "\n"
				+ "\n"
				+ "public Lift(int highestFloor) { topFloor = 0; "+ "\n" // 7 - Change
				+ "topFloor = highestFloor;"+ "\n"
				+ "topFloor = highestFloor + 1;"+ "\n" // 9 - Insert
				+ "topFloor = highestFloor + 1;"+ "\n" // 10 - Insert
				+ "}"+ "\n"
				+ "}";
		
		File cutJavaFile = temporaryFolder.newFile();
		FileUtils.writeStringToFile(cutJavaFile, originalCode);
		//
		File mutantJavaFile = temporaryFolder.newFile();
		FileUtils.writeStringToFile(mutantJavaFile , mutantCode);
		
		GameClass mockedGameClass = mock(GameClass.class);
		DuelGame mockedDualGame = mock(DuelGame.class);
		//
		when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());

		PowerMockito.mockStatic(DatabaseAccess.class);
		when(DatabaseAccess.getGameForKey("ID", 1)).thenReturn( mockedDualGame);
		when(mockedDualGame.getClassId()).thenReturn(1);
		when(DatabaseAccess.getClassForKey("Class_ID", 1)).thenReturn( mockedGameClass );
		//
		when( mockedGameClass.getJavaFile()).thenReturn( cutJavaFile.getAbsolutePath() );
//		

		//
		Mutant m = new Mutant(1, mutantJavaFile.getAbsolutePath(), null, true, 1);
		
		Patch p = m.getDifferences();
		
		assertEquals(2, p.getDeltas().size()); // Change and insertion
		assertEquals(2, m.getLines().size());  // Change line, and line of insertion
		assertEquals(Arrays.asList(7, 9), m.getLines());

		System.out.println("MutantTest.testGetLinesForInsertMultipleLines()" + m.getHTMLReadout() );
	}
	
	@org.junit.Test
	public void testGetLinesForInsertionMutantOnDisjointLines() throws IOException{
//		int classId =
//				DatabaseAccess.getGameForKey("ID", gameId).getClassId();
//		GameClass sut = DatabaseAccess.getClassForKey("Class_ID", classId);
		
		// Mock the class provide a temmp sourceFile with original content in it
		String originalCode = "public class Lift {"+ "\n"
				+ "private int topFloor;"+ "\n"
				+ "private int currentFloor = 0; // default"+ "\n"
				+ "private int capacity = 10;    // default"+ "\n"
				+ "private int numRiders = 0;    // default"+ "\n"
				+ "\n"
				+ "public Lift(int highestFloor) {"+ "\n"
				+ "topFloor = highestFloor;"+ "\n"
				+ "}"+ "\n"
				+ "\n"
				+ "public Lift(int highestFloor, int maxRiders) {"+ "\n"
				+ "this(highestFloor);"+ "\n"
				+ "capacity = maxRiders;"+ "\n"
				+ "}"+ "\n"
				+ "}";
		
		String mutantCode = "public class Lift {"+ "\n"
				+ "private int topFloor;"+ "\n"
				+ "private int currentFloor = 0; // default"+ "\n"
				+ "private int capacity = 10;    // default"+ "\n"
				+ "private int numRiders = 0;    // default"+ "\n"
				+ "\n"
				+ "public Lift(int highestFloor) {"+ "\n"
				+ "topFloor = highestFloor;"+ "\n"
				+ "topFloor = highestFloor;"+ "\n" // Add line
				+ "}"+ "\n"
				+ "\n"
				+ "public Lift(int highestFloor, int maxRiders) {"+ "\n"
				+ "this(highestFloor);"+ "\n"
				+ "topFloor = highestFloor;"+ "\n" // Add line
				+ "capacity = maxRiders;"+ "\n"
				+ "}"+ "\n"
				+ "}";
		
		File cutJavaFile = temporaryFolder.newFile();
		FileUtils.writeStringToFile(cutJavaFile, originalCode);
		//
		File mutantJavaFile = temporaryFolder.newFile();
		FileUtils.writeStringToFile(mutantJavaFile , mutantCode);
		
		GameClass mockedGameClass = mock(GameClass.class);
		DuelGame mockedDualGame = mock(DuelGame.class);
		//
		when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());

		PowerMockito.mockStatic(DatabaseAccess.class);
		when(DatabaseAccess.getGameForKey("ID", 1)).thenReturn( mockedDualGame);
		when(mockedDualGame.getClassId()).thenReturn(1);
		when(DatabaseAccess.getClassForKey("Class_ID", 1)).thenReturn( mockedGameClass );
		//
		when( mockedGameClass.getJavaFile()).thenReturn( cutJavaFile.getAbsolutePath() );
//		

		//
		Mutant m = new Mutant(1, mutantJavaFile.getAbsolutePath(), null, true, 1);
		
		System.out.println("MutantTest.testGetLinesForInsertionMutant() Lines " + m.getLines());
		System.out.println("MutantTest.testGetLinesForInsertionMutant() Lines " + m.getHTMLReadout());
		
		Patch p = m.getDifferences();

		assertEquals(2, p.getDeltas().size()); // Change and insertion
		assertEquals(Arrays.asList(9, 13), m.getLines());

		for (Delta d : p.getDeltas()) {
			System.out.println("MutantTest.testGetLinesForInsertionMutant() " + d);
		}
		
		
	}

}
