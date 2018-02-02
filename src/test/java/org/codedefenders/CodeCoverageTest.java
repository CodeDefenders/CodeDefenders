package org.codedefenders;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

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
		System.out.println("CodeCoverageTest.testMethodSignatureAreUncoverableLines() " + gc.getUnreachableClosingBracketsForIfStatements() );
		// TODO Assert !
//		assertEquals(8, gc.getLinesOfNonCoverableCode().size());
	}
	
	@Test
	public void testSkipMethodSignatureForInterfaces() {
		// This class has 5 unitialized fields at lines: 12, 14, 16, 18, 20
		GameClass gc = new GameClass("Document", "Document",
				"src/test/resources/itests/sources/Document/Document.java", "");
		System.out.println("CodeCoverageTest.testMethodSignatureAreUncoverableLines() " + gc.getLinesOfMethodSignatures());
	}
	
	
	

}
