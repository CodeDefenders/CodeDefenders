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
package org.codedefenders.validation.code;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
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
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.PrettyPrinterConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This class checks test code and checks whether the code is valid or not.
 * <p>
 * Extends {@link VoidVisitorAdapter} but doesn't use the generic extra
 * parameter on {@code visit(Node, __)}, so it's set to {@link Void} here.
 * <p>
 * Instances of this class can be used as follows:
 * <pre><code>CompilationUnit cu = ...;
 * int maxNumberOfAssertions = ...;
 * boolean result = TestCodeVisitor.validFor(cu, maxNumberOfAssertions);
 * </code></pre>
 *
 * @author Jose Rojas
 * @author gambi
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 */
class TestCodeVisitor extends VoidVisitorAdapter<Void> {
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
    
    // Per game configuration
    private int maxNumberOfAssertions;
    private boolean forceHamcrest;
    
    // TODO Probably we should provide a reason WHY the validation fails?
    static boolean validFor(CompilationUnit cu,int maxNumberOfAssertions, boolean forceHamcrest) {
        TestCodeVisitor visitor = new TestCodeVisitor(maxNumberOfAssertions, forceHamcrest);
        visitor.visit(cu, null);
        return visitor.isValid();
    }

    private TestCodeVisitor(int maxNumberOfAssertions, boolean forceHamcrest) {
        this.maxNumberOfAssertions = maxNumberOfAssertions;
        this.forceHamcrest = forceHamcrest;
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
    public void visit(ClassOrInterfaceDeclaration stmt, Void args) {
        if (!isValid || classCount++ > MAX_NUMBER_OF_CLASSES) {
            isValid = false;
            return;
        }
        super.visit(stmt, args);
    }

    @Override
    public void visit(MethodDeclaration stmt, Void args) {
        if (!isValid || methodCount++ > MAX_NUMBER_OF_METHODS) {
            isValid = false;
            return;
        }
        super.visit(stmt, args);
    }

    @Override
    public void visit(ExpressionStmt stmt, Void args) {
        if (!isValid) {
            return;
        }
        String stringStmt = stmt.toString( new PrettyPrinterConfiguration().setPrintComments(false));
        for (String prohibited : CodeValidator.PROHIBITED_CALLS) {
            // This might be a bit too strict... We shall use typeSolver otherwise.
            if (stringStmt.contains(prohibited)) {
                messages.add("Invalid test contains a call to prohibited " + prohibited);
                isValid = false;
                return;
            }
        }
        stmtCount++;
        super.visit(stmt, args);
    }

    @Override
    public void visit(NameExpr stmt, Void args) {
        final String name = stmt.getNameAsString();
        if (name.equals("System") || name.equals("Random") || name.equals("Thread")) {
            messages.add("Invalid test contains System/Random/Thread uses");
            isValid = false;
        }
        super.visit(stmt, args);
    }

    @Override
    public void visit(ForeachStmt stmt, Void args) {
        messages.add("Invalid test contains a ForeachStmt statement");
        isValid = false;
    }

    @Override
    public void visit(ForStmt stmt, Void args) {
        messages.add("Invalid test contains a ForStmt statement");
        isValid = false;
    }

    @Override
    public void visit(WhileStmt stmt, Void args) {
        messages.add("Invalid test contains a WhileStmt statement");
        isValid = false;
    }

    @Override
    public void visit(DoStmt stmt, Void args) {
        messages.add("Invalid test contains a DoStmt statement");
        isValid = false;
    }

    @Override
    public void visit(SwitchStmt stmt, Void args) {
        messages.add("Invalid test contains a SwitchStmt statement");
        isValid = false;
    }

    @Override
    public void visit(IfStmt stmt, Void args) {
        messages.add("Invalid test contains an IfStmt statement");
        isValid = false;
    }

    @Override
    public void visit(ConditionalExpr stmt, Void args) {
        messages.add("Invalid test contains a conditional statement: " + stmt.toString());
        isValid = false;
    }

    @Override
    public void visit(AssertStmt stmt, Void args) {
        messages.add("Invalid assert statement: " + stmt.toString());
        isValid = false;
    }

    @Override
    public void visit(MethodCallExpr stmt, Void args) {
        if (!isValid) {
            return;
        }
        stmtCount++;
        if (stmt.toString().startsWith("System.") || stmt.toString().startsWith("Random.")) {
            messages.add("There is a call to System/Random.*");
            isValid = false;
            return;
        }

        // JUnit Assertion 
        final boolean anyJunitAssertionMatch = Arrays.stream(new String[]{
                "assertEquals", "assertTrue", "assertFalse", "assertNull",
                "assertNotNull", "assertSame", "assertNotSame", "assertArrayEquals"})
                .anyMatch(s -> stmt.getNameAsString().equals(s));
        
        final boolean hamcrestAssertThatMatch = Arrays.stream(new String[]{"assertThat"})
                .anyMatch(s -> stmt.getNameAsString().equals(s));
        
        /*
         * Count assertions
         */
        if( anyJunitAssertionMatch || hamcrestAssertThatMatch){
            assertionCount++;
        }
        
        /*
         * If forced Hamcrest is on, then there must not be JUnit assertions 
         * 
         * TODO What if there's no assertions at all ? 
         */
        if( forceHamcrest && anyJunitAssertionMatch ){
                messages.add("Invalid test contains JUnit assertion " + stmt.toString());
                isValid = false;
                return;
        }
        
        if (assertionCount > maxNumberOfAssertions) {
            messages.add("Invalid test contains more assertions ("+ assertionCount + ") than the limit ("+ maxNumberOfAssertions + ")");
            isValid = false;
            return;
        }
        
        super.visit(stmt, args);
    }

    @Override
    public void visit(VariableDeclarator stmt, Void args) {
        if (!isValid) {
            return;
        }
        Optional<Expression> initializer = stmt.getInitializer();
        if (initializer.isPresent()) {
            String initString = initializer.get().toString();
            if (initString.startsWith("System.*") || initString.startsWith("Random.*") ||
                    initString.contains("Thread")) {
                messages.add("There is a variable declaration using Thread/System/Random.*");
                isValid = false;
            }
        }
        super.visit(stmt, args);
    }

    @Override
    public void visit(final BinaryExpr stmt, Void args) {
        if (!isValid) {
            return;
        }
        final BinaryExpr.Operator operator = stmt.getOperator();
        if (operator == BinaryExpr.Operator.AND || operator == BinaryExpr.Operator.OR) {
            isValid = false;
        }
        super.visit(stmt, args);
    }

    @Override
    public void visit(final AssignExpr stmt, Void args) {
        if (!isValid) {
            return;
        }
        final AssignExpr.Operator operator = stmt.getOperator();
        if (operator != null && (Stream.of(AssignExpr.Operator.AND, AssignExpr.Operator.OR, AssignExpr.Operator.XOR)
                .anyMatch(op -> operator == op))) {
            isValid = false;
        }
        super.visit(stmt, args);
    }

}
