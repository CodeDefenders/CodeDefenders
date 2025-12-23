package org.codedefenders.validation.code;

import com.github.javaparser.ast.expr.ConditionalExpr;
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
import org.codedefenders.util.JavaParserUtils;

import java.util.Arrays;
import java.util.List;

public class TestValidationRules {
    //Categories
    private static final String NO_NEW_CLASSES_OR_METHODS = "New classes or methods are not allowed";
    private static final String NO_CONTROL_STRUCTURES = "Control structures are not allowed";
    private static final String NO_SYSTEM_CALLS = "Calls to certain packages are not allowed";
    private static final String NOT_EMPTY = "Test may not be empty";

    private static List<TestRule> rules = List.of(
            new TestRule.Builder(NO_NEW_CLASSES_OR_METHODS,
                    "New class definitions are not allowed",
                    "You cannot create a second class.")
                    .withVisitor(v -> v.classes.size() > 1).build(),
            new TestRule.Builder(NO_NEW_CLASSES_OR_METHODS,
                    "New methods are not allowed",
                    "You cannot create a new method.")
                    .withVisitor(v -> v.methods.size() > 1).build(),
            new TestRule.Builder(NOT_EMPTY,
                    NOT_EMPTY,
                    "The test is empty.")
                    .withVisitor(v -> v.stmtCount > 0).build(),
            new TestRule.Builder(NO_CONTROL_STRUCTURES,
                    "Loops are not allowed",
                    "Loops in the test are not allowed. Offending statement: ")
                    .withNode(n ->
                            n instanceof WhileStmt || n instanceof ForEachStmt || n instanceof ForStmt
                                    || n instanceof DoStmt
                    ).build(),
            new TestRule.Builder(NO_CONTROL_STRUCTURES,
                    "If-Statements are not allowed",
                    "If-like statements are not allowed. Offending statement: ")
                    .withNode(n ->
                            n instanceof IfStmt || n instanceof SwitchStmt || n instanceof SwitchExpr
                                    || n instanceof ConditionalExpr).build(),
            new TestRule.Builder("No assert()",
                    "\"assert()\"-Statements are not allowed.",
                    "\"assert()\"-statements are not allowed. " +
                            "Use the Assertions from your test library! Offending statement: ")
                    .withNode(n -> n instanceof AssertStmt).build(),

            new TestRule.Builder(NO_SYSTEM_CALLS,
                    "Calls to these packages are not allowed: " + "TODO",
                    "You have called a package you may not call. Offending statement: ")
                    .withNode(n ->
                            n instanceof ExpressionStmt &&
                            Arrays.stream(CodeValidator.PROHIBITED_CALLS)
                            .anyMatch(prohibited -> JavaParserUtils.unparse(n)
                                    .contains(prohibited)))
                    .withNode(n ->
                            n instanceof NameExpr name && (
                            name.getNameAsString().equals("System")
                            || name.getNameAsString().equals("Random")
                            || name.getNameAsString().equals("Thread"))
                    ).build()


    );

    //No classes
    //No methods
    //No loops
    //No if-statements (incl. ternary, switch)
    //no asserts
    //No empty tests
    //Max number of assertions - independent
    //No calls to System etc.


    public static List<TestRule> getRules() {
        return rules;
    }
}
