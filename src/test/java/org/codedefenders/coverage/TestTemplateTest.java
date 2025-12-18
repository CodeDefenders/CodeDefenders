/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
package org.codedefenders.coverage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.codedefenders.game.AssertionLibrary;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.TestingFramework;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestTemplateTest {
    private void assertEditableLineCorrect(GameClass gc) {
        int editableLineNr = gc.getTestTemplateFirstEditLine();
        String editableLine = gc.getTestTemplate().split("\n")[editableLineNr - 1]; // lines are one-indexed
        assertThat(editableLine, containsString("test here"));
    }

    public record MockGameClassAndImports(GameClass gameClass, List<String> imports) {}
    private MockGameClassAndImports mockWithAdditionalImports(GameClass gc) {
        try {
            List<@NotNull String> imports = Files.readAllLines(Path.of(gc.getJavaFile()))
                    .stream()
                    .filter(line -> line.startsWith("import "))
                    .map(line -> line + "\n")
                    .toList();
            gc = Mockito.spy(gc);
            Mockito.doReturn(imports).when(gc).getAdditionalImports();
            return new MockGameClassAndImports(gc, imports);
        } catch (IOException e) {
            fail();
            throw new RuntimeException();
        }
    }

    @Test
    public void testPlainClass() {
        GameClass gc = GameClass.build()
                .name("Lift")
                .alias("Lift")
                .javaFile("src/test/resources/itests/sources/Lift/Lift.java")
                .classFile("")
                .testingFramework(TestingFramework.JUNIT4)
                .assertionLibrary(AssertionLibrary.JUNIT4_HAMCREST)
                .create();
        gc = mockWithAdditionalImports(gc).gameClass;
        assertEditableLineCorrect(gc);
    }

    @Test
    public void testPackage() throws Exception {
        GameClass gc = GameClass.build()
                .name("my.little.test.Lift")
                .alias("Lift")
                .javaFile("src/test/resources/itests/sources/Lift/Lift.java")
                .classFile("")
                .testingFramework(TestingFramework.JUNIT4)
                .assertionLibrary(AssertionLibrary.JUNIT4_HAMCREST)
                .create();
        gc = mockWithAdditionalImports(gc).gameClass;

        assertEditableLineCorrect(gc);
        assertThat(gc.getTestTemplate(), containsString("package my.little.test;"));
    }

    @Test
    public void testMockingEnabled() throws Exception {
        GameClass gc = GameClass.build()
                .name("Lift")
                .alias("Lift")
                .javaFile("src/test/resources/itests/sources/Lift/Lift.java")
                .classFile("")
                .mockingEnabled(true)
                .testingFramework(TestingFramework.JUNIT4)
                .assertionLibrary(AssertionLibrary.JUNIT4_HAMCREST)
                .create();
        gc = mockWithAdditionalImports(gc).gameClass;

        assertEditableLineCorrect(gc);
        assertThat(gc.getTestTemplate(), containsString("mock"));
    }

    @Test
    public void testImports() {
        GameClass gc = GameClass.build()
                .name("Option")
                .alias("Option")
                .javaFile("src/test/resources/itests/sources/Option/Option.java")
                .classFile("")
                .mockingEnabled(true)
                .testingFramework(TestingFramework.JUNIT4)
                .assertionLibrary(AssertionLibrary.JUNIT4_HAMCREST)
                .create();
        gc = mockWithAdditionalImports(gc).gameClass;

        assertEditableLineCorrect(gc);
        assertThat(gc.getTestTemplate(), containsString("import java.util.List;"));
    }

    @Test
    public void test5Imports() throws Exception {
        GameClass gc = GameClass.build()
                .name("my.little.test.XmlElement")
                .alias("XmlElement")
                .javaFile("src/test/resources/itests/sources/XmlElement/XmlElement.java")
                .classFile("")
                .mockingEnabled(true)
                .testingFramework(TestingFramework.JUNIT4)
                .assertionLibrary(AssertionLibrary.JUNIT4_HAMCREST)
                .create();
        gc = mockWithAdditionalImports(gc).gameClass;

        assertEditableLineCorrect(gc);
        assertThat(gc.getTestTemplate(), allOf(
                containsString("import java.util.Enumeration;"),
                containsString("import java.util.Hashtable;"),
                containsString("import java.util.Iterator;"),
                containsString("import java.util.List;"),
                containsString("import java.util.Vector;")));
    }

    @Test
    public void testAssertionLibraryVersions() throws Exception {
        GameClass gc1 = GameClass.build()
                .name("my.little.test.XmlElement")
                .alias("XmlElement")
                .javaFile("src/test/resources/itests/sources/XmlElement/XmlElement.java")
                .classFile("")
                .mockingEnabled(true)
                .testingFramework(TestingFramework.JUNIT4)
                .assertionLibrary(AssertionLibrary.JUNIT4)
                .create();
        gc1 = mockWithAdditionalImports(gc1).gameClass;

        assertThat(gc1.getTestTemplate(), containsString("org.junit.Assert"));
        assertThat(gc1.getTestTemplate(), not(containsString("hamcrest")));
        assertThat(gc1.getTestTemplate(), not(containsString("org.junit.jupiter.api.Assertions")));

        GameClass gc2 = GameClass.build()
                .name("my.little.test.XmlElement")
                .alias("XmlElement")
                .javaFile("src/test/resources/itests/sources/XmlElement/XmlElement.java")
                .classFile("")
                .mockingEnabled(true)
                .testingFramework(TestingFramework.JUNIT4)
                .assertionLibrary(AssertionLibrary.JUNIT4_HAMCREST)
                .create();
        gc2 = mockWithAdditionalImports(gc2).gameClass;

        assertThat(gc2.getTestTemplate(), containsString("org.junit.Assert"));
        assertThat(gc2.getTestTemplate(), containsString("hamcrest"));
        assertThat(gc2.getTestTemplate(), not(containsString("org.junit.jupiter.api.Assertions")));

        GameClass gc3 = GameClass.build()
                .name("my.little.test.XmlElement")
                .alias("XmlElement")
                .javaFile("src/test/resources/itests/sources/XmlElement/XmlElement.java")
                .classFile("")
                .mockingEnabled(true)
                .testingFramework(TestingFramework.JUNIT4)
                .assertionLibrary(AssertionLibrary.JUNIT5)
                .create();
        gc3 = mockWithAdditionalImports(gc3).gameClass;

        assertThat(gc3.getTestTemplate(), not(containsString("org.junit.jupiter.Assertions")));
        assertThat(gc3.getTestTemplate(), not(containsString("hamcrest")));
        assertThat(gc3.getTestTemplate(), containsString("org.junit.jupiter.api.Assertions"));

        GameClass gc4 = GameClass.build()
                .name("my.little.test.XmlElement")
                .alias("XmlElement")
                .javaFile("src/test/resources/itests/sources/XmlElement/XmlElement.java")
                .classFile("")
                .mockingEnabled(true)
                .testingFramework(TestingFramework.JUNIT4)
                .assertionLibrary(AssertionLibrary.JUNIT5_HAMCREST)
                .create();
        gc4 = mockWithAdditionalImports(gc4).gameClass;

        assertThat(gc4.getTestTemplate(), not(containsString("org.jupiter.Assertions")));
        assertThat(gc4.getTestTemplate(), containsString("hamcrest"));
        assertThat(gc4.getTestTemplate(), containsString("org.junit.jupiter.api.Assertions"));

        GameClass gc5 = GameClass.build()
                .name("my.little.test.XmlElement")
                .alias("XmlElement")
                .javaFile("src/test/resources/itests/sources/XmlElement/XmlElement.java")
                .classFile("")
                .mockingEnabled(true)
                .testingFramework(TestingFramework.JUNIT4)
                .assertionLibrary(AssertionLibrary.HAMCREST)
                .create();
        gc5 = mockWithAdditionalImports(gc5).gameClass;

        assertThat(gc5.getTestTemplate(), not(containsString("org.jupiter.Assertions")));
        assertThat(gc5.getTestTemplate(), containsString("hamcrest"));
        assertThat(gc5.getTestTemplate(), not(containsString("org.junit.jupiter.api.Assertions")));
    }

    @Test
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
        gc = mockWithAdditionalImports(gc).gameClass;

        String testTemplate = gc.getHTMLEscapedTestTemplate();
        assertThat(testTemplate, containsString("mockito"));
    }

    @Test
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
        gc = mockWithAdditionalImports(gc).gameClass;

        String testTemplate = gc.getHTMLEscapedTestTemplate();
        assertThat(testTemplate, not(containsString("mockito")));
    }

    @Test
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
        gc = mockWithAdditionalImports(gc).gameClass;

        String testTemplate = gc.getHTMLEscapedTestTemplate();
        assertThat(testTemplate, allOf(
                containsString("junit"),
                containsString("hamcrest"),
                containsString("mockito")));
        // We need -1 to get rid of the last token
        int expectedImports = 5;
        int actualImports = testTemplate.split("import").length - 1;
        assertEquals(expectedImports, actualImports,
                "The test template has the wrong number of imports");
    }

    @Test
    public void testAutomaticImport() throws IOException {
        GameClass gc = GameClass.build()
                .name("XmlElement")
                .alias("XmlElement")
                .javaFile("src/test/resources/itests/sources/XmlElement/XmlElement.java")
                .classFile("src/test/resources/itests/sources/XmlElement/XmlElement.class")
                .testingFramework(TestingFramework.JUNIT4)
                .assertionLibrary(AssertionLibrary.JUNIT4_HAMCREST)
                .mockingEnabled(true)
                .create();
        var mock = mockWithAdditionalImports(gc);
        gc = mock.gameClass;

        String testTemplate = gc.getHTMLEscapedTestTemplate();

        assertThat(testTemplate, allOf(
                containsString("junit"),
                containsString("hamcrest"),
                containsString("mockito")));
        for (var import_ : mock.imports()) {
            assertThat(testTemplate, containsString(import_));
        }
        int expectedImports = 10;
        int actualImports = testTemplate.split("import").length - 1;

        assertEquals(expectedImports, actualImports,
                "The test template has the wrong number of imports");
    }
}
