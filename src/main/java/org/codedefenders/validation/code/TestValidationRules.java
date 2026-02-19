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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.codedefenders.util.JavaParserUtils;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import static org.codedefenders.game.AssertionLibrary.GOOGLE_TRUTH;
import static org.codedefenders.game.AssertionLibrary.HAMCREST;

@Named("testValidationRules")
@ApplicationScoped
public class TestValidationRules {
    //Categories
    private static final String ASSERTION_LIMITS = "Do not use too many assertions.";
    private static final String NO_NEW_CLASSES_OR_METHODS = "New classes or methods are not allowed";
    private static final String NO_CONTROL_STRUCTURES = "Control structures are not allowed";
    private static final String NO_SYSTEM_CALLS = "Calls to certain packages are not allowed";
    private static final String NOT_EMPTY = "Test may not be empty";

    private static final List<TestRule> rules = List.of(
            new TestRule.Builder(NO_NEW_CLASSES_OR_METHODS,
                    "No new classes",
                    "You cannot create a second class.")
                    .withVisitor(c -> c.getClasses().size() > 1).build(),
            new TestRule.Builder(NO_NEW_CLASSES_OR_METHODS,
                    "No new methods",
                    "You cannot create a new method.")
                    .withVisitor(c -> c.getMethods().size() > 1).build(),
            new TestRule.Builder(NOT_EMPTY,
                    NOT_EMPTY,
                    "The test is empty.")
                    .withVisitor(c -> c.getStmtCount() == 0).build(),
            new TestRule.Builder(NO_CONTROL_STRUCTURES,
                    "No loops",
                    "Loops in the test are not allowed.")
                    .withNode(n ->
                            n instanceof WhileStmt || n instanceof ForEachStmt || n instanceof ForStmt
                                    || n instanceof DoStmt
                    ).build(),
            new TestRule.Builder(NO_CONTROL_STRUCTURES,
                    "No conditional statements (like if, switch etc)",
                    "Conditional statements are not allowed.")
                    .withNode(n ->
                            n instanceof IfStmt || n instanceof SwitchStmt || n instanceof SwitchExpr
                                    || n instanceof ConditionalExpr).build(),
            new TestRule.Builder(NO_CONTROL_STRUCTURES,
                    "No logical operators like '&&' or '||'",
                    "You used an illegal binary operator.")
                    .withNode(n ->
                            n instanceof BinaryExpr binaryExpr && (
                                    binaryExpr.getOperator() == BinaryExpr.Operator.AND
                                            || binaryExpr.getOperator() == BinaryExpr.Operator.OR
                            )
                    ).build(),
            new TestRule.Builder(NO_CONTROL_STRUCTURES,
                    "No bitwise operators like &= or |=",
                    "An operator you used is not allowed.")
                    .withNode(
                            n -> n instanceof AssignExpr assignExpr
                                    && Stream.of(
                                            AssignExpr.Operator.BINARY_AND,
                                            AssignExpr.Operator.BINARY_OR,
                                            AssignExpr.Operator.XOR)
                                    .anyMatch(op -> assignExpr.getOperator() == op)
                    ).build(),
            new TestRule.Builder("No assert()",
                    "No \"assert()\"-Statements",
                    "\"assert()\"-statements are not allowed. "
                            + "Use the Assertions from your test library!")
                    .withNode(n -> n instanceof AssertStmt).build(),

            new TestRule.Builder(NO_SYSTEM_CALLS, //TODO Modularize and standardize, create some tests to check what is
                    //TODO really necessary
                    "No calls to any of these packages: System, Random, Thread",
                    "You have called a package you may not call.")
                    .withNode(n ->
                            n instanceof ExpressionStmt
                                    && Stream.of("Date(", "Random(", "Random.", "System.", "Thread.", "java.io",
                                                    "java.net", "java.nio", "java.sql", "random(", "randomUUID("
                                    )
                                    .anyMatch(prohibited -> JavaParserUtils.unparse(n)
                                            .contains(prohibited)))
                    .withNode(
                            n -> { //TODO Is this necessary? Adapted it from the old code, but seems to be handled
                                //TODO already by the code above and below
                                if (n instanceof MethodCallExpr methodCallExpr) {
                                    String stmtString = JavaParserUtils.unparse(methodCallExpr);
                                    return stmtString.startsWith("System.") || stmtString.startsWith("Random.");
                                } else {
                                    return false;
                                }
                            }
                    )
                    .withNode(
                            n -> {
                                if (n instanceof VariableDeclarator variableDeclarator) {
                                    Optional<Expression> initializer = variableDeclarator.getInitializer();
                                    if (initializer.isPresent()) {
                                        String initString = JavaParserUtils.unparse(initializer.get());
                                        return initString.startsWith("System.")
                                                || initString.startsWith("Random.")
                                                || initString.contains("Thread");
                                    }
                                }
                                return false;
                            }
                    )
                    .withNode(n ->
                            n instanceof NameExpr name && (
                                    name.getNameAsString().equals("System")
                                            || name.getNameAsString().equals("Random")
                                            || name.getNameAsString().equals("Thread"))
                    ).build(),
            new TestRule.Builder(ASSERTION_LIMITS,
                    "Keep the assertion limit of your game!",
                    "You used more than ${MAX_ASSERTIONS} assertions.")
                    .withVisitor(c -> c.getAssertionCount() > c.getMaxNumberOfAssertions())
                    .hidden()
                    .build(),

            new TestRule.Builder(ASSERTION_LIMITS,
                    "Only use the assertions of the correct test library",
                    "Your assertion does not belong to the correct test library.")
                    .withVisitor(c -> (c.getAssertionLibrary() == HAMCREST
                            || c.getAssertionLibrary() == GOOGLE_TRUTH)
                            && c.getJunitAssertionCount() > 0
                    ).build()


    );

    private static final List<List<TestRule>> tieredRules = ValidationUtils.getTieredRules(rules);
    private static final List<TestRule> singleRules = ValidationUtils.getSingleRules(rules);


    public static List<TestRule> getRules() {
        return rules;
    }

    /**
     * Returns all categories, that is, collections of rules with the same {@link TestRule#getGeneralDescription()},
     * that have at least two rules.
     */
    public List<List<TestRule>> getTieredRules() {
        return tieredRules;
    }

    /**
     * Returns all rules that have a unique {@link TestRule#getGeneralDescription()}.
     */
    public List<TestRule> getSingleRules() {
        return singleRules;
    }
}
