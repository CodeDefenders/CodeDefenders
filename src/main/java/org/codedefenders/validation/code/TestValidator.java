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
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.codedefenders.game.AssertionLibrary;

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
 * "result variables", that are checked for visitor test rule violations.
 */
// Does this really need to short-circuit at the first error?
class TestValidator {
    // we can use TypeSolver for the visit to implement a fine grain security mechanism
    private final List<String> messages = new LinkedList<>();

    private final List<TestRule> rules;
    private final List<ImmutablePair<TestRule, Node>> failedRules = new ArrayList<>();

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

    /**
     * Returns a list of validation messages. If the list is empty, the validation passed
     *
     * <p>
     * TODO If I'm not mistaken, this can only return a list of either 0 or 1 elements. Either we make use of this
     * TODO being a list, or this should be an optional.
     */
    static List<String> validFor(CompilationUnit cu, int maxNumberOfAssertions, AssertionLibrary assertionLibrary,
                                 List<TestRule> rules) {
        TestValidator visitor = new TestValidator(maxNumberOfAssertions, assertionLibrary, rules);
        // Collect the observations first
        //visitor.visit(cu, null);
        visitor.validate(cu);


        List<TestRule> violatingRules = new ArrayList<>();
        for (ImmutablePair<TestRule, Node> pair : visitor.failedRules) {
            if (!violatingRules.contains(pair.left)) {
                violatingRules.add(pair.left);
                String prettyPrintedOffender = ("\n" + pair.right.toString()).replace("\n", "\n\t\t");


                visitor.messages.add(pair.left.getValidationMessage() + prettyPrintedOffender);
            }
        }

        for (TestRule r : rules) {
            if (r.fails(visitor)) {
                if (!violatingRules.contains(r)) {
                    violatingRules.add(r);
                }
                visitor.messages.add(r.getValidationMessage());
            }
        }
        return visitor.buildValidationMessages();
    }

    private void validate(CompilationUnit cu) {
        cu.walk(n -> {
            for (TestRule rule : rules) {
                if (rule.fails(n)) {
                    failedRules.add(new ImmutablePair<>(rule, n));
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
    }

    private TestValidator(int maxNumberOfAssertions, AssertionLibrary assertionLibrary, List<TestRule> rules) {
        this.maxNumberOfAssertions = maxNumberOfAssertions;
        this.assertionLibrary = assertionLibrary;
        this.rules = rules;
    }

    public List<String> buildValidationMessages() { //TODO reicht ein String??
        List<String> formattedValidationMessages = new ArrayList<>();

        if (!messages.isEmpty()) {
            StringBuilder sb = new StringBuilder("The submitted test is not valid:");
            for (int i = 0; i < messages.size(); i++) {
                sb.append("\n\t").append(i + 1).append(": ").append(messages.get(i));
            }
            //formattedValidationMessages.add("The submitted test is not valid:\n" + String.join("\n", "\t-" + messages));
            formattedValidationMessages.add(sb.toString());
        }
        return formattedValidationMessages;
    }


    public void handleMethodCalls(MethodCallExpr stmt) {
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
