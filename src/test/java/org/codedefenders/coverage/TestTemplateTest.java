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
package org.codedefenders.coverage;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.codedefenders.analysis.gameclass.ClassCodeAnalyser;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.game.AssertionLibrary;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.TestingFramework;
import org.codedefenders.instrumentation.MetricsRegistry;
import org.codedefenders.service.ClassAnalysisService;
import org.codedefenders.util.WeldExtension;
import org.codedefenders.util.WeldSetup;
import org.jboss.weld.junit4.WeldInitiator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;

@ExtendWith(WeldExtension.class)
public class TestTemplateTest {

    // Required for mocking Configuration, which is loaded into a static field of FileUtils, required by GameClass.
    @WeldSetup
    public WeldInitiator weld = WeldInitiator
                .from(TestTemplateTest.class,
                        ClassAnalysisService.class,
                        ClassCodeAnalyser.class)
                .inject(this)
                .activate(ApplicationScoped.class)
                .build();

    @Produces
    public Configuration produceConfiguration() {
        return new Configuration() {};
    }

    @ApplicationScoped
    @Produces
    public MetricsRegistry getMockedMetricsRegistry() {
        return mock(MetricsRegistry.class);
    }

    private void assertEditableLineCorrect(GameClass gc) {
        int editableLineNr = gc.getTestTemplateFirstEditLine();
        String editableLine = gc.getTestTemplate().split("\n")[editableLineNr - 1]; // lines are one-indexed
        assertThat(editableLine, containsString("test here"));
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

        assertThat(gc5.getTestTemplate(), not(containsString("org.jupiter.Assertions")));
        assertThat(gc5.getTestTemplate(), containsString("hamcrest"));
        assertThat(gc5.getTestTemplate(), not(containsString("org.junit.jupiter.api.Assertions")));
    }
}
