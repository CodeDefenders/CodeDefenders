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
package org.codedefenders.util.analysis;

import org.apache.commons.lang3.Range;
import org.codedefenders.game.AssertionLibrary;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.TestingFramework;
import org.junit.Assume;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 * Tests {@link GameClass} test template generation and {@link ClassCodeAnalyser} implementation.
 */
public class StaticAnalysisTest {

    @Test
    public void testVisitCodeWithNonParsableCode() {
        final String name = "Test";
        final String sourceCode = "This is not valid java code.";

        final CodeAnalysisResult result = ClassCodeAnalyser.visitCode(name, sourceCode);
        assertTrue(result.getAdditionalImports().isEmpty());
        assertTrue(result.getClosingBrackets().isEmpty());
        assertTrue(result.getCompileTimeConstants().isEmpty());
        assertTrue(result.getMethods().isEmpty());
        assertTrue(result.getMethodSignatures().isEmpty());
        assertTrue(result.getNonCoverableCode().isEmpty());
    }

    @Test
    public void testVisitCodeWithParsableCode() {
        final String name = "IntHashMap";
        final String sourceCode;
        try {
            sourceCode = new String(Files.readAllBytes(Paths.get("src/test/resources/itests/sources/IntHashMap/IntHashMap.java")));
        } catch (IOException e) {
            Assume.assumeNoException("IntHashMap.java file should exists", e);
            return;
        }

        final CodeAnalysisResult result = ClassCodeAnalyser.visitCode(name, sourceCode);
        assertTrue(result.getAdditionalImports().isEmpty());
        assertFalse(result.getClosingBrackets().isEmpty());
        assertTrue(result.getCompileTimeConstants().isEmpty());
        assertFalse(result.getMethods().isEmpty());
        assertFalse(result.getMethodSignatures().isEmpty());
        assertFalse(result.getNonCoverableCode().isEmpty());
    }

    @Test
    public void testCompileTimeConstants() {
        String name = "Test";
        String sourceCode = String.join("\n",
                "public class Test {",
                "   public static final String hello = \"WORLD\";",
                "   public static final int onetwofour = 124;",
                "   public static final Integer muchmore = 123123;", // no compile time constant
                "}");
        final CodeAnalysisResult result = ClassCodeAnalyser.visitCode(name, sourceCode);

        Integer[] expected = {2,3};
        assertArrayEquals(expected, result.getCompileTimeConstants().toArray());
    }

    @Test
    public void testAdditionalImports() {
        String name = "Test";
        final String collections = "import java.util.Collections;";
        final String list = "import java.util.List;";
        final String arrayList = "import java.util.ArrayList;";
        String sourceCode = String.join("\n",
                collections,
                list,
                arrayList,
                "public class Test {",
                "   public static void main(String[] args) {}",
                "}");
        final CodeAnalysisResult result = ClassCodeAnalyser.visitCode(name, sourceCode);

        String[] expected = {collections + "\n", list + "\n", arrayList + "\n"};
        assertArrayEquals(expected, result.getAdditionalImports().toArray());
    }

    @Test
    public void testClosingBracketIf() {
        String name = "Test";
        String sourceCode = String.join("\n",
                "public class Test {",
                "   public static void main(String[] args) {",
                "       if (2 == 2) {",
                "           System.out.println(\"Hello World\");",
                "       }",
                "   }",
                "}");
        final CodeAnalysisResult result = ClassCodeAnalyser.visitCode(name, sourceCode);
        final Range<Integer> next = result.getClosingBrackets().iterator().next();
        assertEquals(3, next.getMinimum().intValue());
        assertEquals(5, next.getMaximum().intValue());
    }

    @Test
    public void testClosingBracketIfElse() {
        String name = "Test";
        String sourceCode = String.join("\n",
                "public class Test {",
                "   public static void main(String[] args) {",
                "       if (2 == 2) {",
                "           System.out.println(\"Hello World\");",
                "       } ",
                "       else {",
                "           System.out.println(\"World Hello\");",
                "       }",
                "   }",
                "}");
        final CodeAnalysisResult result = ClassCodeAnalyser.visitCode(name, sourceCode);
        final Iterator<Range<Integer>> iterator = result.getClosingBrackets().iterator();
        final Range<Integer> then = iterator.next();
        assertEquals(3, then.getMinimum().intValue());
        assertEquals(5, then.getMaximum().intValue());

        Integer[] expected = {2,5,6,8,9,10};
        assertArrayEquals(expected, result.getNonCoverableCode().toArray());
    }

    @Test
    public void testTestCoverUnitializedFields() {
        GameClass gc = GameClass.build()
                .name("XmlElement")
                .alias("XmlElement")
                .javaFile("src/test/resources/itests/sources/XmlElement/XmlElement.java")
                .classFile("src/test/resources/itests/sources/XmlElement/XmlElement.class")
                .create();

        Integer[] expected = {12, 14, 16, 18, 20};
        assertArrayEquals(expected, gc.getNonInitializedFields().toArray());
    }

    @Test
    public void testTestCoverUnitializedFieldsInnerStaticClass() {
        GameClass gc = GameClass.build()
                .name("IntHashMap")
                .alias("IntHashMap")
                .javaFile("src/test/resources/itests/sources/IntHashMap/IntHashMap.java")
                .classFile("")
                .create();

        Integer[] expected = {9, 14, 20, 25, 32, 33, 34, 35};
        assertArrayEquals(expected, gc.getNonInitializedFields().toArray());
    }

    @Test
    public void testMethodSignatureAreUncoverableLines() {
        GameClass gc = GameClass.build()
                .name("IntHashMap")
                .alias("IntHashMap")
                .javaFile("src/test/resources/itests/sources/IntHashMap/IntHashMap.java")
                .classFile("")
                .create();

        Integer[] expected = {45, 57, 69, 82, 104, 114, 135, 162, 175, 196, 217, 254, 293, 316};
        assertArrayEquals(expected, gc.getMethodSignatures().toArray());
    }

    @Test
    public void testMethodsSignaturesForLine() {
        GameClass gc = GameClass.build()
                .name("IntHashMap")
                .alias("IntHashMap")
                .javaFile("src/test/resources/itests/sources/IntHashMap/IntHashMap.java")
                .classFile("")
                .create();

        assertTrue(gc.getMethodSignaturesForLine(9).isEmpty());
        assertArrayEquals(new Integer[]{45}, gc.getMethodSignaturesForLine(47).toArray()); // constructor of inner static class
        assertArrayEquals(new Integer[]{82}, gc.getMethodSignaturesForLine(87).toArray()); // constructor of outer class
        assertArrayEquals(new Integer[]{254}, gc.getMethodSignaturesForLine(272).toArray()); // normal method
    }

    @Test
    public void testClosingBracketsForLineInIfStatement() {
        GameClass gc = GameClass.build()
                .name("IntHashMap")
                .alias("IntHashMap")
                .javaFile("src/test/resources/itests/sources/IntHashMap/IntHashMap.java")
                .classFile("")
                .create();

        assertArrayEquals(new Integer[]{92}, gc.getClosingBracketForLine(91).toArray());
        assertArrayEquals(new Integer[]{264}, gc.getClosingBracketForLine(261).toArray());
        assertArrayEquals(new Integer[]{273}, gc.getClosingBracketForLine(268).toArray());

    }

    @Test
    public void testTestTemplateAutomaticImportOnlyPrimitive() {
        GameClass gc = GameClass.build()
                .name("Lift")
                .alias("Lift")
                .javaFile("src/test/resources/itests/sources/Lift/Lift.java")
                .classFile("src/test/resources/itests/sources/Lift/Lift.class")
                .testingFramework(TestingFramework.JUNIT4)
                .assertionLibrary(AssertionLibrary.JUNIT4_HAMCREST)
                .create();

        assertEquals("The test template has the wrong number of imports",
                gc.getAdditionalImports().size(), 0);
    }

    @Test
    public void testTestTemplateAutomaticImport() {
        GameClass gc = GameClass.build()
                .name("XmlElement")
                .alias("XmlElement")
                .javaFile("src/test/resources/itests/sources/XmlElement/XmlElement.java")
                .classFile("src/test/resources/itests/sources/XmlElement/XmlElement.class")
                .create();

        assertEquals("The test template has the wrong number of imports",
                gc.getAdditionalImports().size(), 5);
    }
}
