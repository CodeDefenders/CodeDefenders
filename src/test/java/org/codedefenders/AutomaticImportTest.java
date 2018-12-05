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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;

public class AutomaticImportTest {

	@org.junit.Test
	public void testAutomaticImportOfMockitoIfEnabled() {
		GameClass gc = new GameClass("Lift", "Lift", "src/test/resources/itests/sources/Lift/Lift.java",
				"src/test/resources/itests/sources/Lift/Lift.class", true);

		String testTemplate = gc.getHTMLEscapedTestTemplate();
		assertThat(testTemplate, containsString("import static org.mockito.Mockito.*;"));
	}

	@org.junit.Test
	public void testNoAutomaticImportOfMockitoIfDisabled() {
		GameClass gc = new GameClass("Lift", "Lift", "src/test/resources/itests/sources/Lift/Lift.java",
				"src/test/resources/itests/sources/Lift/Lift.class", false);

		String testTemplate = gc.getHTMLEscapedTestTemplate();
		assertThat(testTemplate, not(containsString("import static org.mockito.Mockito.*;")));
	}

	@org.junit.Test
	public void testAutomaticImportOnlyPrimitive() {
		GameClass gc = new GameClass("Lift", "Lift", "src/test/resources/itests/sources/Lift/Lift.java",
				"src/test/resources/itests/sources/Lift/Lift.class",
				true); // Including mocking

		String testTemplate = gc.getHTMLEscapedTestTemplate();
		assertThat(testTemplate,
				allOf(
						containsString("import static org.mockito.Mockito.*;"),
						containsString("import static org.junit.Assert.*;"),
						containsString("import org.junit.*;")
				));
		// We need -1 to get rid of the last token
		int expectedImports = testTemplate.split("import").length - 1;
		assertEquals( "The test template has the wrong number of imports", 3, expectedImports );
	}

	@org.junit.Test
	public void testAutomaticImport() {
		GameClass gc = new GameClass("XmlElement", "XmlElement",
				"src/test/resources/itests/sources/XmlElement/XmlElement.java",
				"src/test/resources/itests/sources/XmlElement/XmlElement.class",
				true); // Including mocking

		String testTemplate = gc.getHTMLEscapedTestTemplate();

		assertThat(testTemplate,
				allOf(
						containsString("import static org.mockito.Mockito.*;"),
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
		assertEquals( "The test template has the wrong number of imports", 8, expectedImports );
	}
}
