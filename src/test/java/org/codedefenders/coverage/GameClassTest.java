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

import org.codedefenders.game.GameClass;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

public class GameClassTest {

    @Test
    public void testGetTestTemplateFirstEditLinePlainClass() {
        String name = "Lift";
        String alias = "Lift";
        String jFile = "src/test/resources/itests/sources/Lift/Lift.java";
        String cFile = "";

        GameClass gc = new GameClass(name, alias, jFile, cFile);

        int expectedFirstEditableLine = GameClass.BASIC_IMPORTS.size() + gc.getAdditionalImports().size();

        // Zero-index offset, 2 blank lines, class declaration, @Test annotation, test method signature
        expectedFirstEditableLine += 5;

        assertEquals(expectedFirstEditableLine, gc.getTestTemplateFirstEditLine());
    }

    @Test
    public void testGetTestTemplateFirstEditLineWithPackage() throws Exception {

        // Package information
        String name = "my.little.test.Lift";
        String alias = "Lift";
        String jFile = "src/test/resources/itests/sources/Lift/Lift.java";
        String cFile = "";

        GameClass gc = new GameClass(name, alias, jFile, cFile);

        int expectedFirstEditableLine = GameClass.BASIC_IMPORTS.size() + gc.getAdditionalImports().size();

        // Zero-index offset, 2 blank lines, class declaration, @Test annotation, test method signature
        expectedFirstEditableLine += 5;

        // Package + blank line
        expectedFirstEditableLine += 2;

        assertEquals(expectedFirstEditableLine, gc.getTestTemplateFirstEditLine());
    }

    @Test
    public void testGetTestTemplateFirstEditLineWithMockingEnabled() throws Exception {

        String name = "Lift";
        String alias = "Lift";
        String jFile = "src/test/resources/itests/sources/Lift/Lift.java";
        String cFile = "";

        GameClass gc = new GameClass(name, alias, jFile, cFile);

        int expectedFirstEditableLine = GameClass.BASIC_IMPORTS.size() + gc.getAdditionalImports().size();

        // Zero-index offset, 2 blank lines, class declaration, @Test annotation, test method signature
        expectedFirstEditableLine += 5;

        // set private field isMockingEnabled to true
        final Field mockingField = gc.getClass().getDeclaredField("isMockingEnabled");
        mockingField.setAccessible(true);
        mockingField.set(gc, true);
        expectedFirstEditableLine += GameClass.MOCKITO_IMPORTS.size();

        assertEquals(expectedFirstEditableLine, gc.getTestTemplateFirstEditLine());
    }

    @Test
    public void testGetTestTemplateFirstEditLineWith2Imports() {
        String name = "Option";
        String alias = "Option";
        String jFile = "src/test/resources/itests/sources/Option/Option.java";
        String cFile = "";

        GameClass gc = new GameClass(name, alias, jFile, cFile);

        int expectedFirstEditableLine = GameClass.BASIC_IMPORTS.size() + gc.getAdditionalImports().size();

        // Zero-index offset, 2 blank lines, class declaration, @Test annotation, test method signature
        expectedFirstEditableLine += 5;

        assertEquals(expectedFirstEditableLine, gc.getTestTemplateFirstEditLine());
    }

    @Test
    public void testGetTestTemplateFirstEditLineWith4ImportsPackageAndMocking() throws Exception {
        String name = "my.little.test.XmlElement";
        String alias = "XmlElement";
        String jFile = "src/test/resources/itests/sources/XmlElement/XmlElement.java";
        String cFile = "";

        GameClass gc = new GameClass(name, alias, jFile, cFile);

        int expectedFirstEditableLine = GameClass.BASIC_IMPORTS.size() + gc.getAdditionalImports().size();

        // Zero-index offset, 2 blank lines, class declaration, @Test annotation, test method signature
        expectedFirstEditableLine += 5;
        // Package + blank line
        expectedFirstEditableLine += 2;

        // set private field isMockingEnabled to true
        final Field mockingField = gc.getClass().getDeclaredField("isMockingEnabled");
        mockingField.setAccessible(true);
        mockingField.set(gc, true);
        expectedFirstEditableLine += GameClass.MOCKITO_IMPORTS.size();

        assertEquals(expectedFirstEditableLine, gc.getTestTemplateFirstEditLine());
    }
}
