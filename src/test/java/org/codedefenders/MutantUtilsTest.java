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

import java.util.ArrayList;
import java.util.List;

import org.codedefenders.analysis.gameclass.MethodDescription;
import org.codedefenders.game.Mutant;
import org.codedefenders.util.MutantUtils;
import org.junit.jupiter.api.Test;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MutantUtilsTest {

    @Test
    public void testCleanUpMutatedCodeWithManySingleEmptyLinesInsertion() {
        MutantUtils mutantUtils = new MutantUtils();
        String originalCode = """
                public class Test{
                    public void test(){
                        String s = ""; //comment
                        int foo; // comment
                        int foo1;
                        if(x > 0)
                            return x; //x is positive
                    }
                }""".stripIndent();

        // Same as original but few empty blank lines
        String mutatedCode = """
                public class Test{
                    public void test(){
                        String s = ""; //comment

                        int foo; // comment

                        int foo1;
                        if(x > 0)
                            return x; //x is positive
                    }
                }""".stripIndent();

        String cleanedCode = mutantUtils.cleanUpMutatedCode(originalCode, mutatedCode);

        assertEquals(originalCode, cleanedCode);

    }

    @Test
    public void testCleanUpMutatedCodeWithManySingleBlankLinesInsertion() {
        MutantUtils mutantUtils = new MutantUtils();
        String originalCode = """
                public class Test{
                    public void test(){
                        String s = ""; //comment
                        int foo; // comment
                        int foo1;
                        if(x > 0)
                            return x; //x is positive
                    }
                }""".stripIndent();

        // Same as original but few empty blank lines
        String mutatedCode = """
                public class Test{
                    public void test(){
                        String s = ""; //comment

                        int foo; // comment

                        int foo1;
                        if(x > 0)
                            return x; //x is positive
                    }
                }""".stripIndent();

        String cleanedCode = mutantUtils.cleanUpMutatedCode(originalCode, mutatedCode);

        assertEquals(originalCode, cleanedCode);

    }

    @Test
    public void testCleanUpMutatedCodeWithManyMultiBlankLinesInsertion() {
        MutantUtils mutantUtils = new MutantUtils();
        String originalCode = """
                public class Test{
                    public void test(){
                        String s = ""; //comment
                        int foo; // comment
                        int foo1;
                        if(x > 0)
                            return x; //x is positive
                    }
                }""".stripIndent();

        // Same as original but few empty blank lines
        String mutatedCode = """
                public class Test{
                    public void test(){
                        String s = ""; //comment



                        int foo; // comment

                        int foo1;
                        if(x > 0)
                            return x; //x is positive
                    }
                }""".stripIndent();

        String cleanedCode = mutantUtils.cleanUpMutatedCode(originalCode, mutatedCode);

        assertEquals(originalCode, cleanedCode);

    }

    @Test
    public void testCleanUpMutatedCodeWithBlankLinesInBetween() {
        MutantUtils mutantUtils = new MutantUtils();
        String originalCode = """
                public class Complex {

                    public double real, imag;
                    /**
                     * Constructor that defines the <code>real</code> and
                     * <code>imaginary</code> parts of the number.
                     *
                     * @param real The real part of the number.
                     * @param imag The imaginary part of the number.
                     */
                    public Complex(double real, double imag) {
                        this.real = real;
                        this.imag = imag;
                    }
                }""".stripIndent();

        String mutatedCode = """
                public class Complex {

                    public double real, imag;
                    /**
                     * Constructor that defines the <code>real</code> and
                     * <code>imaginary</code> parts of the number.
                     *
                     * @param real The real part of the number.
                     * @param imag The imaginary part of the number.
                     */
                    public Complex(double real, double imag) {
                        this.real =



                                real;
                        this.imag = imag;
                    }
                }""".stripIndent();

        String expectedCode = """
                public class Complex {

                    public double real, imag;
                    /**
                     * Constructor that defines the <code>real</code> and
                     * <code>imaginary</code> parts of the number.
                     *
                     * @param real The real part of the number.
                     * @param imag The imaginary part of the number.
                     */
                    public Complex(double real, double imag) {
                        this.real =
                                real;
                        this.imag = imag;
                    }
                }""".stripIndent();

        String cleanedCode = mutantUtils.cleanUpMutatedCode(originalCode, mutatedCode);

        // Now
        assertEquals(expectedCode, cleanedCode);
    }


    @Test
    public void testIsOutsideOfMethod() {
        String mutantCode;
        String cutCode = """
                public class Cut {
                    public void foo() {
                        int a = 1;
                        int b = 2;
                    }

                    public void bar() {
                        int c = 3;
                        int d = 4;
                    }
                }""";
        MethodDescription fooDescription = new MethodDescription("public void foo()", 2, 5);
        MethodDescription barDescription = new MethodDescription("public void bar()", 7, 10);
        List<MethodDescription> methodDescriptions = List.of(fooDescription, barDescription);

        mutantCode = """
                public class Cut {
                    public void foo() {
                        int a = 1;
                        int b = 2;
                    }
                    int x = 5;
                    public void bar() {
                        int c = 3;
                        int d = 4;
                    }
                }""";
        assertTrue(isOutsideOfMethods(cutCode, mutantCode, methodDescriptions));

        mutantCode = """
                public class Cut {
                    public void foo() {
                        int a = 1;
                        int b = 2;
                    }

                    int x = 5;
                    public void bar() {
                        int c = 3;
                        int d = 4;
                    }
                }""";

        assertTrue(isOutsideOfMethods(cutCode, mutantCode, methodDescriptions));

        mutantCode = """
                public class Cut {
                    public void foo() {
                        int a = 1;
                        int b = 2;
                        int x = 2;
                    }

                    public void bar() {
                        int c = 3;
                        int d = 4;
                    }
                }""";
        assertFalse(isOutsideOfMethods(cutCode, mutantCode, methodDescriptions));

        mutantCode = """
                public class Cut {
                    int x = 5;
                    public void foo() {
                        int a = 1;
                        int b = 2;
                    }

                    public void bar() {
                        int c = 3;
                        int d = 4;
                    }
                }""";
        assertTrue(isOutsideOfMethods(cutCode, mutantCode, methodDescriptions));

        mutantCode = """
                public class Cut {
                    int x = 5;public void foo() {
                        int a = 1;
                        int b = 2;
                    }
                    int x = 5;
                    public void bar() {
                        int c = 3;
                        int d = 4;
                    }
                }""";
        assertTrue(isOutsideOfMethods(cutCode, mutantCode, methodDescriptions));

        //Right now, this is the expected behaviour, even if it isn't 100% correct
        mutantCode = """
                public class Cut {
                    public void foo(int x) {
                        int a = 1;
                        int b = 2;
                    }
                    int x = 5;
                    public void bar() {
                        int c = 3;
                        int d = 4;
                    }
                }""";
        assertTrue(isOutsideOfMethods(cutCode, mutantCode, methodDescriptions));

        mutantCode = """
                public class Cut {
                    public void foo() {
                        int a = 1;
                        int b = 2;
                        int x = 3;
                    }
                    int y = 3;
                    public void bar() {
                        int c = 3;
                        int d = 4;
                    }
                }""";
        assertTrue(isOutsideOfMethods(cutCode, mutantCode, methodDescriptions));

        mutantCode = """
                public class Cut {
                    public void foo() {
                        int a = 1;
                        int b = 2;
                    }
                    int x = 5;
                    public void bar() {
                        int c = 3;
                        int d = 4;
                    }
                }""";
        assertTrue(isOutsideOfMethods(cutCode, mutantCode, methodDescriptions));

        mutantCode = """
                public class Cut {
                    public void foo() {
                        int a = 1;
                        int b = 2;
                    }int x = 5;

                    public void bar() {
                        int c = 3;
                        int d = 4;
                    }
                }""";
        assertTrue(isOutsideOfMethods(cutCode, mutantCode, methodDescriptions));

        mutantCode = """
                public class Cut {
                    public void foo() {
                        int a = 1;
                        int b = 2;
                    }

                    public void bar() {
                        int c = 3;
                        int d = 4;
                    }

                    int y = 5;
                }""";
        assertTrue(isOutsideOfMethods(cutCode, mutantCode, methodDescriptions));
    }

    private boolean isOutsideOfMethods(String cutCode, String mutantCode, List<MethodDescription> methodDescriptions) {
        List<String> cutLines = new ArrayList<>(cutCode.lines().toList());
        cutLines.replaceAll(s -> s.replaceAll(Mutant.unquotedWhitespaceRegex, ""));
        List<String> mutantLines = new ArrayList<>(mutantCode.lines().toList());
        mutantLines.replaceAll(s -> s.replaceAll(Mutant.unquotedWhitespaceRegex, ""));
        Patch<String> difference = DiffUtils.diff(cutLines, mutantLines);
        return MutantUtils.isOutsideOfMethods(methodDescriptions, difference);
    }

}
