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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;

import org.codedefenders.game.AssertionLibrary;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.TestingFramework;

public class AutomaticImportTest {

    @org.junit.Test
    public void testAutomaticImportOfMockitoIfEnabled() {
        GameClass gc = GameClass.build()
                .name("Lift")
                .alias("Lift")
                .javaFile("src/test/resources/itests/sources/Lift/Lift.java")
                .classFile("src/test/resources/itests/sources/Lift/Lift.class")
                .mockingEnabled(true)
                .testingFramework(TestingFramework.JUNIT4)
                .assertionLibrary(AssertionLibrary.JUNIT4_HAMCREST)
                .create();

        String testTemplate = gc.getHTMLEscapedTestTemplate();
        assertThat(testTemplate, containsString("mockito"));
    }

    @org.junit.Test
    public void testNoAutomaticImportOfMockitoIfDisabled() {
        GameClass gc = GameClass.build()
                .name("Lift")
                .alias("Lift")
                .javaFile("src/test/resources/itests/sources/Lift/Lift.java")
                .classFile("src/test/resources/itests/sources/Lift/Lift.class")
                .mockingEnabled(false)
                .testingFramework(TestingFramework.JUNIT4)
                .assertionLibrary(AssertionLibrary.JUNIT4_HAMCREST)
                .create();

        String testTemplate = gc.getHTMLEscapedTestTemplate();
        assertThat(testTemplate, not(containsString("mockito")));
    }

    @org.junit.Test
    public void testAutomaticImportOnlyPrimitive() {
        GameClass gc = GameClass.build()
                .name("Lift")
                .alias("Lift")
                .javaFile("src/test/resources/itests/sources/Lift/Lift.java")
                .classFile("src/test/resources/itests/sources/Lift/Lift.class")
                .mockingEnabled(true)
                .testingFramework(TestingFramework.JUNIT4)
                .assertionLibrary(AssertionLibrary.JUNIT4_HAMCREST)
                .create();

        String testTemplate = gc.getHTMLEscapedTestTemplate();
        assertThat(testTemplate, allOf(
                containsString("junit"),
                containsString("hamcrest"),
                containsString("mockito")));
        // We need -1 to get rid of the last token
        int expectedImports = 5;
        int actualImports = testTemplate.split("import").length - 1;
        assertEquals("The test template has the wrong number of imports", expectedImports, actualImports);
    }

    @org.junit.Test
    public void testAutomaticImport() {
        GameClass gc = GameClass.build()
                .name("XmlElement")
                .alias("XmlElement")
                .javaFile("src/test/resources/itests/sources/XmlElement/XmlElement.java")
                .classFile("src/test/resources/itests/sources/XmlElement/XmlElement.class")
                .testingFramework(TestingFramework.JUNIT4)
                .assertionLibrary(AssertionLibrary.JUNIT4_HAMCREST)
                .mockingEnabled(true)
                .create();

        String testTemplate = gc.getHTMLEscapedTestTemplate();

        assertThat(testTemplate, allOf(
                containsString("junit"),
                containsString("hamcrest"),
                containsString("mockito"),
                containsString("import java.util.Enumeration;"),
                containsString("import java.util.Hashtable;"),
                containsString("import java.util.Iterator;"),
                containsString("import java.util.List;"),
                containsString("import java.util.Vector;")));
        int expectedImports = 10;
        int actualImports = testTemplate.split("import").length - 1;

        assertEquals("The test template has the wrong number of imports", expectedImports, actualImports);
    }
}
