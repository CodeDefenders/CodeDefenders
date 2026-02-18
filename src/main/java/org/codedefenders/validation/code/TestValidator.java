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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.codedefenders.game.AssertionLibrary;
import org.codedefenders.util.JavaParserUtils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;

/**
 * This class checks test code and checks whether the code is valid or not.
 * While traversing the AST, it checks every node for node test rule violations. It also collects some
 * "result variables", that can later be checked for visitor test rule violations.
 *
 * <p>
 * One TestValidator should only exist for the validation of a single test.
 */
class TestValidator {
    private final List<TestRule> rules;
    private final CodeValidationResult validationResult = new CodeValidationResult(CodeValidationResult.Type.TEST);

    //Result variables to be checked by VisitorRules
    List<Node> classes = new ArrayList<>();
    List<Node> methods = new ArrayList<>();
    int stmtCount = 0;
    int assertionCount = 0;
    int junitAssertionCount = 0;

    // Per game configuration
    final int maxNumberOfAssertions;
    // Per class configuration
    final AssertionLibrary assertionLibrary;

    TestValidator(int maxNumberOfAssertions, AssertionLibrary assertionLibrary, List<TestRule> rules) {
        this.maxNumberOfAssertions = maxNumberOfAssertions;
        this.assertionLibrary = assertionLibrary;
        this.rules = rules;

        validationResult.setMaxNumberOfAssertions(maxNumberOfAssertions);
    }

    CodeValidationResult validFor(String testCode) {
        Optional<CompilationUnit> parseResult = JavaParserUtils.parse(testCode);
        if (parseResult.isPresent()) {

            // Collect the observations first, check NodeRules
            parseResult.get().walk(n -> {
                for (TestRule rule : rules) {
                    if (rule.fails(n)) {
                        validationResult.add(rule, n);
                    }
                }

                if (n instanceof ClassOrInterfaceDeclaration || n instanceof RecordDeclaration) {
                    classes.add(n);
                } else if (n instanceof MethodDeclaration) {
                    methods.add(n);
                } else if (n instanceof MethodCallExpr methodCallExpr) {
                    stmtCount++;
                    handleMethodCalls(methodCallExpr);
                } else if (n instanceof ExpressionStmt) {
                    stmtCount++;
                }
            });


            for (TestRule r : rules) {
                if (r.fails(this)) {
                    validationResult.add(r);
                }
            }
        } else {
            validationResult.setFailedParsing();
        }
        return validationResult;
    }

    private void handleMethodCalls(MethodCallExpr stmt) {
        //TODO assertThrows is missing? Very hard-coded
        // JUnit Assertion
        final boolean anyJunitAssertionMatch = Arrays.stream(new String[]{
                "assertEquals", "assertTrue", "assertFalse", "assertNull",
                "assertNotNull", "assertSame", "assertNotSame", "assertArrayEquals"})
                .anyMatch(s -> stmt.getNameAsString().equals(s));
        // TODO This works the same for Hamcrest and Google Truth
        final boolean assertThatMatch = Arrays.stream(new String[]{"assertThat"})
                .anyMatch(s -> stmt.getNameAsString().equals(s));
        /*
         * Count assertions
         */
        if (anyJunitAssertionMatch) {
            junitAssertionCount++;
            assertionCount++;
        } else if (assertThatMatch) {
            assertionCount++;
        }
    }
}
