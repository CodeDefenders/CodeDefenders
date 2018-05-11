package org.codedefenders;

import org.codedefenders.game.GameClass;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;

public class StaticAnalysesTest {

	@org.junit.Test
	public void testUncoverableLinesIdentification() {
		GameClass gc = new GameClass("XmlElement", "XmlElement",
				"src/test/resources/itests/sources/XmlElement/XmlElement.java",
				"src/test/resources/itests/sources/XmlElement/XmlElement.class");

		for (Integer line : gc.getLinesOfNonCoverableCode()) {
			System.out.println("Uncoverable line " + line );
		}

		// We know that there are 5 non initialized fields from that class
		// There are 35 lines that are method/constructor signatures or annotations
		// CodeDefenders currently identifies 17 closing braces.
		// TODO this may need updating depending on the rules of what CodeDefenders identifies as uncoverable
		assertEquals(57, gc.getLinesOfNonCoverableCode().size());
		// TODO add assertions that check line number corresponds
	}

	@org.junit.Test
	public void testTestHasCompileTimeConstants() {
		GameClass gc = new GameClass("Option", "Option",
				"src/test/resources/itests/sources/Option/Option.java",
				"src/test/resources/itests/sources/Option/Option.class");

		for (Integer line : gc.getLinesOfCompileTimeConstants()) {
			System.out.println("StaticAnalysesTest " + line );
		}

		// We know that there are 5 non initialized fields from that class
		assertEquals(2, gc.getLinesOfCompileTimeConstants().size());
		// TODO add assertions that check line number corresponds
	}
	
	@org.junit.Test
	public void testAutomaticImportOnlyPrimitive() {
		GameClass gc = new GameClass("Lift", "Lift", "src/test/resources/itests/sources/Lift/Lift.java",
				"src/test/resources/itests/sources/Lift/Lift.class");

		String testTemplate = gc.getTestTemplate();
		assertThat(testTemplate,
				allOf(containsString("import static org.junit.Assert.*;"), containsString("import org.junit.*;")));
		// We need -1 to get rid of the last token
		int expectedImports = testTemplate.split("import").length - 1;
		assertEquals("The test template has the wrong number of imports", 2, expectedImports);
	}

	@org.junit.Test
	public void testAutomaticImport() {
		GameClass gc = new GameClass("XmlElement", "XmlElement",
				"src/test/resources/itests/sources/XmlElement/XmlElement.java",
				"src/test/resources/itests/sources/XmlElement/XmlElement.class");

		String testTemplate = gc.getTestTemplate();

		assertThat(testTemplate,
				allOf(containsString("import static org.junit.Assert.*;"), containsString("import org.junit.*;"),
						containsString("import java.util.Enumeration;"), containsString("import java.util.Hashtable;"),
						containsString("import java.util.Iterator;"), containsString("import java.util.List;"),
						containsString("import java.util.Vector;")));

		int expectedImports = testTemplate.split("import").length - 1;
		assertEquals("The test template has the wrong number of imports", 7, expectedImports);
	}
}
