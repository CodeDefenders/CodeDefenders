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
