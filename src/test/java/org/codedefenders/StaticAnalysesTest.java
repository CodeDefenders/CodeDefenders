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
