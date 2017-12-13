package org.codedefenders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CodeCoverageTest {
	
	@Test
	public void testTestCoverUnitializedFields() {
		// This class has 5 unitialized fields at lines: 12, 14, 16, 18, 20
		GameClass gc = new GameClass("XmlElement", "XmlElement", "src/test/resources/itests/sources/XmlElement/XmlElement.java",
				"src/test/resources/itests/sources/XmlElement/XmlElement.class");

		// TODO We need to run the test
		
		// TODO We know that there are 5 non initialized fields from that class
		assertEquals(5, gc.getLinesOfNonInitializedFields().size());
		// TODO add assertions that check line number corresponds

	}
}
