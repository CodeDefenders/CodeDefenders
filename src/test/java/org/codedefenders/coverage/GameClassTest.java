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

import java.lang.reflect.Field;

public class GameClassTest {

    private void assertEditableLineCorrect(GameClass gc) {
        int editableLineNr = gc.getTestTemplateFirstEditLine();
        String editableLine = gc.getTestTemplate().split("\n")[editableLineNr - 1]; // lines are one-indexed
        assertThat(editableLine, containsString("test here"));
    }

    @Test
    public void testGetTestTemplateFirstEditLinePlainClass() {
        String name = "Lift";
        String alias = "Lift";
        String javaFile = "src/test/resources/itests/sources/Lift/Lift.java";
        String classFile = "";

        GameClass gc = new GameClass(name, alias, javaFile, classFile);

        assertEditableLineCorrect(gc);
    }

    @Test
    public void testGetTestTemplateFirstEditLineWithPackage() throws Exception {
        // Package information
        String name = "my.little.test.Lift";
        String alias = "Lift";
        String javaFile = "src/test/resources/itests/sources/Lift/Lift.java";
        String classFile = "";

        GameClass gc = new GameClass(name, alias, javaFile, classFile);

        assertEditableLineCorrect(gc);
        assertThat(gc.getTestTemplate(), containsString("package"));
    }

    @Test
    public void testGetTestTemplateFirstEditLineWithMockingEnabled() throws Exception {
        String name = "Lift";
        String alias = "Lift";
        String javaFile = "src/test/resources/itests/sources/Lift/Lift.java";
        String classFile = "";

        GameClass gc = new GameClass(name, alias, javaFile, classFile);

        final Field mockingField = gc.getClass().getDeclaredField("isMockingEnabled");
        mockingField.setAccessible(true);
        mockingField.set(gc, true);

        assertEditableLineCorrect(gc);
        assertThat(gc.getTestTemplate(), containsString("mock"));
    }

    @Test
    public void testGetTestTemplateFirstEditLineWith2Imports() {
        String name = "Option";
        String alias = "Option";
        String javaFile = "src/test/resources/itests/sources/Option/Option.java";
        String classFile = "";

        GameClass gc = new GameClass(name, alias, javaFile, classFile);

        assertEditableLineCorrect(gc);
        assertThat(gc.getTestTemplate(), containsString("import java.util.List;"));
    }

    @Test
    public void testGetTestTemplateFirstEditLineWith4ImportsPackageAndMocking() throws Exception {
        String name = "my.little.test.XmlElement";
        String alias = "XmlElement";
        String javaFile = "src/test/resources/itests/sources/XmlElement/XmlElement.java";
        String classFile = "";

        GameClass gc = new GameClass(name, alias, javaFile, classFile);

        final Field mockingField = gc.getClass().getDeclaredField("isMockingEnabled");
        mockingField.setAccessible(true);
        mockingField.set(gc, true);

        assertEditableLineCorrect(gc);
        assertThat(gc.getTestTemplate(), containsString("import java.util.Vector;"));
    }
}
