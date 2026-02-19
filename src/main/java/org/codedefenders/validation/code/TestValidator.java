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
package org.codedefenders.validation.code;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import org.codedefenders.game.AssertionLibrary;
import org.codedefenders.util.JavaParserUtils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;

/**
 * This class checks test code and checks whether the code is valid or not.
 * While traversing the AST, it checks every node for node test rule violations.
 */
@ApplicationScoped
public class TestValidator {

    public CodeValidationResult validateTestCode(String testCode, int maxNumberOfAssertions,
                                                        AssertionLibrary assertionLibrary) {
        return validateTestCode(testCode, maxNumberOfAssertions, assertionLibrary, TestValidationRules.getRules());
    }

    CodeValidationResult validateTestCode(String testCode, int maxNumberOfAssertions, AssertionLibrary library,
                                                 List<TestRule> rules) {
        CodeValidationResult validationResult = new CodeValidationResult(CodeValidationResult.Type.TEST);
        validationResult.setMaxNumberOfAssertions(maxNumberOfAssertions);

        Optional<CompilationUnit> parseResult = JavaParserUtils.parse(testCode);
        if (parseResult.isPresent()) {
            TestValidationCounter counter = new TestValidationCounter(maxNumberOfAssertions, library);

            // Collect the observations first, check NodeRules
            parseResult.get().walk(n -> {
                for (TestRule rule : rules) {
                    if (rule.fails(n)) {
                        validationResult.add(rule, n);
                    }
                }

                if (n instanceof ClassOrInterfaceDeclaration || n instanceof RecordDeclaration) {
                    counter.addClass(n);
                } else if (n instanceof MethodDeclaration methodDeclaration) {
                    counter.addMethod(methodDeclaration);
                } else if (n instanceof MethodCallExpr methodCallExpr) {
                    counter.addStmt();
                    handleMethodCalls(methodCallExpr, counter);
                } else if (n instanceof ExpressionStmt) {
                    counter.addStmt();
                }
            });

            //Check the collected stats against validation rules
            for (TestRule r : rules) {
                if (r.fails(counter)) {
                    validationResult.add(r);
                }
            }
        } else {
            validationResult.setFailedParsing();
        }
        return validationResult;
    }

    private void handleMethodCalls(MethodCallExpr stmt, TestValidationCounter counter) {
        // JUnit Assertion
        final boolean anyJunitAssertionMatch = Arrays.stream(new String[]{
                "assertEquals", "assertTrue", "assertFalse", "assertNull",
                "assertNotNull", "assertSame", "assertNotSame", "assertArrayEquals"})
                .anyMatch(s -> stmt.getNameAsString().equals(s));
        // This works the same for Hamcrest and Google Truth
        final boolean assertThatMatch = Arrays.stream(new String[]{"assertThat"})
                .anyMatch(s -> stmt.getNameAsString().equals(s));
        /*
         * Count assertions
         */
        if (anyJunitAssertionMatch) {
            counter.addJunitAssertion();
            counter.addAssertion();
        } else if (assertThatMatch) {
            counter.addAssertion();
        }
    }
}
