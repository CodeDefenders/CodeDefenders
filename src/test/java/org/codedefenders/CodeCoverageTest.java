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

import org.codedefenders.game.GameClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Alessio Gambi
 */
public class CodeCoverageTest {

	@Test
	public void testTestCoverUnitializedFields() throws IOException {
		// This class has 5 unitialized fields at lines: 12, 14, 16, 18, 20
		GameClass gc = new GameClass("XmlElement", "XmlElement",
				"src/test/resources/itests/sources/XmlElement/XmlElement.java",
				"src/test/resources/itests/sources/XmlElement/XmlElement.class");

		assertEquals(5, gc.getLinesOfNonInitializedFields().size());
	}

	@Test
	public void testTestCoverUnitializedFieldsInnerStaticClass() {
		// This class has 5 unitialized fields at lines: 12, 14, 16, 18, 20
		GameClass gc = new GameClass("IntHashMap", "IntHashMap",
				"src/test/resources/itests/sources/IntHashMap/IntHashMap.java", "");

		assertEquals(8, gc.getLinesOfNonInitializedFields().size());
	}

	@Test
	public void testMethodSignatureAreUncoverableLines() {
		// This class has 5 unitialized fields at lines: 12, 14, 16, 18, 20
		GameClass gc = new GameClass("IntHashMap", "IntHashMap",
				"src/test/resources/itests/sources/IntHashMap/IntHashMap.java", "");
		System.out.println("CodeCoverageTest.testMethodSignatureAreUncoverableLines() " + gc.getLinesOfMethodSignatures());
		// TODO Assert !
//		assertEquals(8, gc.getLinesOfNonCoverableCode().size());
	}
	
	@Test
	public void testClosingBracketsAreUncoverableLines() {
		// This class has 5 unitialized fields at lines: 12, 14, 16, 18, 20
		GameClass gc = new GameClass("Document", "Document",
				"src/test/resources/itests/sources/Document/Document.java", "");
//		System.out.println("CodeCoverageTest.testMethodSignatureAreUncoverableLines() " + gc.getUnreachableClosingBracketsForIfStatements() );
//		System.out.println("CodeCoverageTest.testMethodSignatureAreUncoverableLines() " + gc.getLineOfClosingBracketFor( 54 ) );
		// Not sure... this includes both the condition and the "{" of the then=stmt. Should not be included
		System.out.println("CodeCoverageTest.testMethodSignatureAreUncoverableLines() " + gc.getLineOfClosingBracketFor( 55 ) );
		//
//		System.out.println("CodeCoverageTest.testMethodSignatureAreUncoverableLines() " + gc.getLineOfClosingBracketFor( 56 ) );
//		System.out.println("CodeCoverageTest.testMethodSignatureAreUncoverableLines() " + gc.getLineOfClosingBracketFor( 57 ) );
//		System.out.println("CodeCoverageTest.testMethodSignatureAreUncoverableLines() " + gc.getLineOfClosingBracketFor( 58 ) );
//		System.out.println("CodeCoverageTest.testMethodSignatureAreUncoverableLines() " + gc.getLineOfClosingBracketFor( 59 ) );
		// TODO Assert !
//		assertEquals(8, gc.getLinesOfNonCoverableCode().size());
	}
	
	@Test
	public void testSkipMethodSignatureForInterfaces() {
		// This class has 5 unitialized fields at lines: 12, 14, 16, 18, 20
		GameClass gc = new GameClass("Document", "Document",
				"src/test/resources/itests/sources/Document/Document.java", "");
//		System.out.println("CodeCoverageTest.testMethodSignatureAreUncoverableLines() " + gc.getLinesOfMethodSignatures());
		//
		System.out.println("CodeCoverageTest.testSkipMethodSignatureForInterfaces() " + gc.getLinesOfMethodSignaturesFor( 29  ) );
		System.out.println("CodeCoverageTest.testSkipMethodSignatureForInterfaces() " + gc.getLinesOfMethodSignaturesFor( 30  ) );
		System.out.println("CodeCoverageTest.testSkipMethodSignatureForInterfaces() " + gc.getLinesOfMethodSignaturesFor( 31  ) );
		System.out.println("CodeCoverageTest.testSkipMethodSignatureForInterfaces() " + gc.getLinesOfMethodSignaturesFor( 32  ) );
	}
	
	

}
