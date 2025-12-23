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
import java.util.Optional;
import java.util.stream.Stream;

import com.github.javaparser.ast.Node;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.codedefenders.game.AssertionLibrary;
import org.codedefenders.util.JavaParserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
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
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import static org.codedefenders.game.AssertionLibrary.GOOGLE_TRUTH;
import static org.codedefenders.game.AssertionLibrary.HAMCREST;
/**
 * This class checks test code and checks whether the code is valid or not.
 *
 * <p>Extends {@link VoidVisitorAdapter} but doesn't use the generic extra
 * parameter on {@code visit(Node, __)}, so it's set to {@link Void} here.
 *
 * @author Jose Rojas
 * @author gambi
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
// Does this really need to short-circuit at the first error?
class TestCodeVisitor extends VoidVisitorAdapter<Void> {
    private static final Logger logger = LoggerFactory.getLogger(TestCodeVisitor.class);
    // we can use TypeSolver for the visit to implement a fine grain security mechanism
    private static final int MAX_NUMBER_OF_CLASSES = 1;
    private static final int MAX_NUMBER_OF_METHODS = 1;
    private static final int MIN_NUMBER_OF_STATEMENTS = 0;
    private final List<String> messages = new LinkedList<>();

    private final List<TestRule> rules;
    private final List<ImmutablePair<TestRule, Node>> failedRules = new ArrayList<>();

    List<Node> classes = new ArrayList<>();
    List<Node> methods = new ArrayList<>();
    int stmtCount = 0;
    // Per game configuration
    private final int maxNumberOfAssertions;
    // Per class configuraion
    private final AssertionLibrary assertionLibrary;
    /**
     * @return A list of validation messages. If the list is empty, the validation passed
     */
    static List<String> validFor(CompilationUnit cu, int maxNumberOfAssertions, AssertionLibrary assertionLibrary,
                                 List<TestRule> rules) {
        TestCodeVisitor visitor = new TestCodeVisitor(maxNumberOfAssertions, assertionLibrary, rules);
        // Collect the observations first
        visitor.visit(cu, null);
        for (TestRule r : rules) {
            if (r.fails(visitor)) {
                visitor.messages.add(r.getValidationMessage());
            }
        }
        return visitor.buildValidationMessages();
    }
    private TestCodeVisitor(int maxNumberOfAssertions, AssertionLibrary assertionLibrary, List<TestRule> rules) {
        this.maxNumberOfAssertions = maxNumberOfAssertions;
        this.assertionLibrary = assertionLibrary;
        this.rules = rules;
    }
    public List<String> buildValidationMessages() {//TODO reicht ein String??
        List<String> formattedValidationMessages = new ArrayList<>();
        if (!messages.isEmpty()) {
            formattedValidationMessages.add("The submitted test is not valid:\n" + String.join("\n", "\t-" + messages));
        }
        return formattedValidationMessages;
    }
    @Override
    public void visit(ClassOrInterfaceDeclaration stmt, Void args) {
        classes.add(stmt);
        super.visit(stmt, args);
    }
    @Override
    public void visit(RecordDeclaration decl, Void args) {
        classes.add(decl);
        super.visit(decl, args);
    }
    @Override
    public void visit(MethodDeclaration stmt, Void args) {
        methods.add(stmt);
        super.visit(stmt, args);
    }
    @Override
    public void visit(ExpressionStmt stmt, Void args) {
        for (TestRule rule : rules) {
            if (rule.fails(stmt)) {
                failedRules.add(new ImmutablePair<>(rule, stmt));
            }
        }
        super.visit(stmt, args);
    }
    @Override
    public void visit(NameExpr stmt, Void args) {
        for (TestRule rule : rules) {
            if (rule.fails(stmt)) {
                failedRules.add(new ImmutablePair<>(rule, stmt));
            }
        }
        super.visit(stmt, args);
    }

    @Override
    public void visit(MethodCallExpr stmt, Void args) {

        stmtCount++;
        String stmtString = JavaParserUtils.unparse(stmt);
        if (stmtString.startsWith("System.") || stmtString.startsWith("Random.")) {
            messages.add("Test contains an invalid statement: " + stmtString);
            isValid = false;
            return;
        }
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
        if (anyJunitAssertionMatch || assertThatMatch) {
            assertionCount++;
        }
        /*
         * If forced Hamcrest or Google Truth are on, then there must not be JUnit assertions.
         *
         * TODO What if there's no assertions at all?
         */
        if ((assertionLibrary == HAMCREST || assertionLibrary == GOOGLE_TRUTH) && anyJunitAssertionMatch) {
            messages.add("Test contains a JUnit assertion: " + JavaParserUtils.unparse(stmt));
            isValid = false;
            return;
        }
        if (assertionCount > maxNumberOfAssertions) {
            messages.add("Test contains more than "
                    + maxNumberOfAssertions + (maxNumberOfAssertions < 2 ? "assertion" : " assertions"));
            isValid = false;
            return;
        }
        super.visit(stmt, args);
    }
    @Override
    public void visit(VariableDeclarator stmt, Void args) {

        Optional<Expression> initializer = stmt.getInitializer();
        if (initializer.isPresent()) {
            String initString = JavaParserUtils.unparse(initializer.get());
            if (initString.startsWith("System.") || initString.startsWith("Random.")
                    || initString.contains("Thread")) {
                messages.add("Test contains an invalid variable declaration: " + initString);
                isValid = false;
                return;
            }
        }
        super.visit(stmt, args);
    }
    @Override
    public void visit(final BinaryExpr stmt, Void args) {

        final BinaryExpr.Operator operator = stmt.getOperator();
        if (operator == BinaryExpr.Operator.AND || operator == BinaryExpr.Operator.OR) {
            messages.add("Test contains an invalid statement: " + JavaParserUtils.unparse(stmt));
            isValid = false;
            return;
        }
        super.visit(stmt, args);
    }
    @Override
    public void visit(final AssignExpr stmt, Void args) {

        final AssignExpr.Operator operator = stmt.getOperator();
        boolean isIllegal = Stream.of(
                    AssignExpr.Operator.BINARY_AND,
                    AssignExpr.Operator.BINARY_OR,
                    AssignExpr.Operator.XOR)
                .anyMatch(op -> operator == op);
        if (isIllegal) {
            isValid = false;
            return;
        }
        super.visit(stmt, args);
    }
}
