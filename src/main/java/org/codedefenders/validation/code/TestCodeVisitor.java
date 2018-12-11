/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.printer.PrettyPrinterConfiguration;

/**
 * This class checks test code and checks whether the code is valid or not.
 * <p>
 * Extends {@link ModifierVisitorAdapter} but doesn't use the generic extra
 * parameter on {@code visit(Node, __)}, so it's set to {@link Void} here.
 * <p>
 * Instances of this class can be used as follows:
 * <pre><code>CompilationUnit cu = ...;
 * TestCodeVisitor visitor = new TestCodeVisitor(maxNumberOfAssertions);
 * visitor.visit(cu, null); // Second parameter is never used
 * boolean result = visitor.isValid();
 * </code></pre>
 *
 * @author Jose Rojas
 * @author gambi
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 */

// TODO Why we need a Modifier visitor if we DO NOT modify the class we are visiting...

class TestCodeVisitor extends ModifierVisitor<Void> {
    private static final Logger logger = LoggerFactory.getLogger(TestCodeVisitor.class);
//  we can use TypeSolver for the visit to implement a fine grain security mechanism

    private static final int MAX_NUMBER_OF_CLASSES = 1;
    private static final int MAX_NUMBER_OF_METHODS = 1;
    private static final int MIN_NUMBER_OF_STATEMENTS = 0;

    private boolean isValid = true;
    private List<String> messages = new LinkedList<>();

    private int classCount = 0;
    private int methodCount = 0;
    private int stmtCount = 0;
    private int assertionCount = 0;
    private int maxNumberOfAssertions;

    TestCodeVisitor(int maxNumberOfAssertions) {
        this.maxNumberOfAssertions = maxNumberOfAssertions;
    }

    public boolean isValid() {
        if (!isValid) {
            logger.info("Test validation failed with messages: \nError: {}", String.join("\nError: ", messages));
            return false;
        }
        if (classCount > MAX_NUMBER_OF_CLASSES) {
            logger.info("Invalid test suite contains more than one class declaration.");
            return false;
        }
        if (methodCount > MAX_NUMBER_OF_METHODS) {
            logger.info("Invalid test suite contains more than one method declaration.");
            return false;
        }
        if (stmtCount == MIN_NUMBER_OF_STATEMENTS) {
            logger.info("Invalid test does not contain any valid statement.");
            return false;
        }
        if (assertionCount > maxNumberOfAssertions) {
            logger.info("Invalid test contains more than " + maxNumberOfAssertions + " assertions");
            return false;
        }
        return true;
    }

    @Override
    public Node visit(ClassOrInterfaceDeclaration stmt, Void args) {
        if (!isValid || classCount++ > MAX_NUMBER_OF_CLASSES) {
            isValid = false;
            return stmt;
        }
        super.visit(stmt, args);
        return stmt;
    }

    @Override
    public Node visit(MethodDeclaration stmt, Void args) {
        if (!isValid || methodCount++ > MAX_NUMBER_OF_METHODS) {
            isValid = false;
            return stmt;
        }
        super.visit(stmt, args);
        return stmt;
    }

    @Override
    public Node visit(ExpressionStmt stmt, Void args) {
        if (!isValid) {
            return stmt;
        }
        String stringStmt = stmt.toString( new PrettyPrinterConfiguration().setPrintComments(false));
        for (String prohibited : CodeValidator.PROHIBITED_CALLS) {
            // This might be a bit too strict... We shall use typeSolver otherwise.
            if (stringStmt.contains(prohibited)) {
                messages.add("Invalid test contains a call to prohibited " + prohibited);
                isValid = false;
                return stmt;
            }
        }
        stmtCount++;
        return (Node) super.visit(stmt, args);
    }

    @Override
    public Node visit(NameExpr stmt, Void args) {
        final String name = stmt.getNameAsString();
        if (name.equals("System") || name.equals("Random") || name.equals("Thread")) {
            messages.add("Invalid test contains System/Random/Thread uses");
            isValid = false;
        }
        return stmt;
    }

    @Override
    public Node visit(ForeachStmt stmt, Void args) {
        messages.add("Invalid test contains a ForeachStmt statement");
        isValid = false;
        return stmt;
    }

    @Override
    public Node visit(ForStmt stmt, Void args) {
        messages.add("Invalid test contains a ForStmt statement");
        isValid = false;
        return stmt;
    }

    @Override
    public Node visit(WhileStmt stmt, Void args) {
        messages.add("Invalid test contains a WhileStmt statement");
        isValid = false;
        return stmt;
    }

    @Override
    public Node visit(DoStmt stmt, Void args) {
        messages.add("Invalid test contains a DoStmt statement");
        isValid = false;
        return stmt;
    }

    @Override
    public Node visit(SwitchStmt stmt, Void args) {
        messages.add("Invalid test contains a SwitchStmt statement");
        isValid = false;
        return stmt;
    }

    @Override
    public Node visit(IfStmt stmt, Void args) {
        messages.add("Invalid test contains an IfStmt statement");
        isValid = false;
        return stmt;
    }

    @Override
    public Node visit(ConditionalExpr stmt, Void args) {
        messages.add("Invalid test contains a conditional statement: " + stmt.toString());
        isValid = false;
        return stmt;
    }

    // TODO I am not sure assert and Assert shall be trated the same way...
    @Override
    public Node visit(AssertStmt stmt, Void args) {
        stmtCount++;
        if (!isValid || assertionCount++ > maxNumberOfAssertions) {
            isValid = false;
            return stmt;
        }
        super.visit(stmt, args);
        return stmt;
    }

    @Override
    public Node visit(MethodCallExpr stmt, Void args) {
        if (!isValid) {
            return stmt;
        }
        stmtCount++;
        if (stmt.toString().startsWith("System.") || stmt.toString().startsWith("Random.")) {
            messages.add("There is a call to System/Random.*");
            isValid = false;
        }
        // This is missing assertThat and all the other assertMethods ?
        final boolean anyMatch = Arrays.stream(new String[]{"assertEquals", "assertTrue", "assertFalse", "assertNull",
                "assertNotNull", "assertSame", "assertNotSame", "assertArrayEquals", "assertThat"})
                .anyMatch(s -> stmt.getNameAsString().equals(s));
        if (anyMatch) {
            if (assertionCount++ > maxNumberOfAssertions) {
                isValid = false;
                return stmt;
            }
        }
        super.visit(stmt, args);
        return stmt;
    }

    @Override
    public Node visit(VariableDeclarator stmt, Void args) {
        if (!isValid) {
            return stmt;
        }
        super.visit(stmt, args);
        if (stmt.getInitializer() != null && (stmt.getInitializer().toString().startsWith("System.*") || stmt.getInitializer().toString().startsWith("Random.*") ||
                stmt.getInitializer().toString().contains("Thread"))) {
            messages.add("There is a variable declaration using Thread/System/Random.*");
            isValid = false;
        }
        return stmt;
    }

    @Override
    public Node visit(final BinaryExpr stmt, Void arg) {
        if (!isValid) {
            return stmt;
        }
        final BinaryExpr.Operator operator = stmt.getOperator();
        if (operator == BinaryExpr.Operator.AND || operator == BinaryExpr.Operator.OR) {
            isValid = false;
        }
        return stmt;
    }

    @Override
    public Node visit(final AssignExpr expr, Void arg) {
        if (!isValid) {
            return expr;
        }
        final AssignExpr.Operator operator = expr.getOperator();
        if (operator != null && (Stream.of(AssignExpr.Operator.AND, AssignExpr.Operator.OR, AssignExpr.Operator.XOR)
                .anyMatch(op -> operator == op))) {
            isValid = false;
        }
        return expr;
    }

}