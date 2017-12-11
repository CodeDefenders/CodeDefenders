package org.codedefenders;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;

public class AutomaticImportTest {

	@org.junit.Test
	public void testAutomaticImportOnlyPrimitive() {
		GameClass gc = new GameClass("Lift", "Lift", "src/test/resources/itests/sources/Lift/Lift.java",
				"src/test/resources/itests/sources/Lift/Lift.class");

		String testTemplate = gc.getTestTemplate();
		assertThat(testTemplate,
				allOf(
						containsString("import static org.junit.Assert.*;"),
						containsString("import org.junit.*;")
					));
		// We need -1 to get rid of the last token
		int expectedImports = testTemplate.split("import").length - 1;
		assertEquals( "The test template has the wrong number of imports", 2, expectedImports );
	}

	@org.junit.Test
	public void testAutomaticImport() {
		GameClass gc = new GameClass("XmlElement", "XmlElement",
				"src/test/resources/itests/sources/XmlElement/XmlElement.java",
				"src/test/resources/itests/sources/XmlElement/XmlElement.class");

		String testTemplate = gc.getTestTemplate();

		assertThat(testTemplate,
				allOf(
						containsString("import static org.junit.Assert.*;"),
						containsString("import org.junit.*;"),
						containsString("import java.util.Enumeration;"),
						containsString("import java.util.Hashtable;"),
						containsString("import java.util.Iterator;"),
						containsString("import java.util.List;"),
						containsString("import java.util.Vector;")
						)
				);

		int expectedImports = testTemplate.split("import").length - 1;
		assertEquals( "The test template has the wrong number of imports", 7, expectedImports );
	}
}
