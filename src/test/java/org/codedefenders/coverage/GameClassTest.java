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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import org.codedefenders.game.GameClass;
import org.junit.Test;

public class GameClassTest {

    private void assertEditableLineCorrect(GameClass gc) {
        int editableLineNr = gc.getTestTemplateFirstEditLine();
        String editableLine = gc.getTestTemplate().split("\n")[editableLineNr - 1]; // lines are one-indexed
        assertThat(editableLine, containsString("test here"));
    }

    @Test
    public void testGetTestTemplateFirstEditLinePlainClass() {
        GameClass gc = GameClass.build()
                .name("Lift")
                .alias("Lift")
                .javaFile("src/test/resources/itests/sources/Lift/Lift.java")
                .classFile("")
                .create();

        assertEditableLineCorrect(gc);
    }

    @Test
    public void testGetTestTemplateFirstEditLineWithPackage() throws Exception {
        GameClass gc = GameClass.build()
                .name("my.little.test.Lift")
                .alias("Lift")
                .javaFile("src/test/resources/itests/sources/Lift/Lift.java")
                .classFile("")
                .create();

        assertEditableLineCorrect(gc);
        assertThat(gc.getTestTemplate(), containsString("package"));
    }

    @Test
    public void testGetTestTemplateFirstEditLineWithMockingEnabled() throws Exception {
        GameClass gc = GameClass.build()
                .name("Lift")
                .alias("Lift")
                .javaFile("src/test/resources/itests/sources/Lift/Lift.java")
                .classFile("")
                .mockingEnabled(true)
                .create();

        assertEditableLineCorrect(gc);
        assertThat(gc.getTestTemplate(), containsString("mock"));
    }

    @Test
    public void testGetTestTemplateFirstEditLineWith2Imports() {
        GameClass gc = GameClass.build()
                .name("Option")
                .alias("Option")
                .javaFile("src/test/resources/itests/sources/Option/Option.java")
                .classFile("")
                .mockingEnabled(true)
                .create();

        assertEditableLineCorrect(gc);
        assertThat(gc.getTestTemplate(), containsString("import java.util.List;"));
    }

    @Test
    public void testGetTestTemplateFirstEditLineWith4ImportsPackageAndMocking() throws Exception {
        GameClass gc = GameClass.build()
                .name("my.little.test.XmlElement")
                .alias("XmlElement")
                .javaFile("src/test/resources/itests/sources/XmlElement/XmlElement.java")
                .classFile("")
                .mockingEnabled(true)
                .create();

        assertEditableLineCorrect(gc);
        assertThat(gc.getTestTemplate(), containsString("import java.util.Vector;"));
    }
}
