package org.codedefenders.validation.code;

import java.util.ArrayList;
import java.util.List;

import org.codedefenders.game.AssertionLibrary;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;

class TestValidationCounter {
    private final int maxNumberOfAssertions;
    private final AssertionLibrary assertionLibrary;

    private final List<Node> classes = new ArrayList<>();
    private final List<Node> methods = new ArrayList<>();
    private int stmtCount = 0;
    private int assertionCount = 0;
    private int junitAssertionCount = 0;

    TestValidationCounter(int maxNumberOfAssertions, AssertionLibrary assertionLibrary) {
        this.assertionLibrary = assertionLibrary;
        this.maxNumberOfAssertions = maxNumberOfAssertions;
    }

    int getMaxNumberOfAssertions() {
        return maxNumberOfAssertions;
    }

    AssertionLibrary getAssertionLibrary() {
        return assertionLibrary;
    }

    List<Node> getClasses() {
        return classes;
    }

    List<Node> getMethods() {
        return methods;
    }

    int getStmtCount() {
        return stmtCount;
    }

    int getAssertionCount() {
        return assertionCount;
    }

    int getJunitAssertionCount() {
        return junitAssertionCount;
    }

    void addClass(Node node) {
        classes.add(node);
    }

    void addMethod(MethodDeclaration node) {
        methods.add(node);
    }

    void addStmt() {
        stmtCount++;
    }

    void addAssertion() {
        assertionCount++;
    }

    void addJunitAssertion() {
        junitAssertionCount++;
    }
}
