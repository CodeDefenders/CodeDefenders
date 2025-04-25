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
package org.codedefenders.analysis.coverage.ast;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.codedefenders.analysis.coverage.ast.AstCoverageStatus.Status;
import org.codedefenders.analysis.coverage.line.LineCoverageStatus;

import com.github.javaparser.ast.Node;

/**
 * Maps JavaParser AST nodes to their coverage status.
 */
public class AstCoverage {
    private final Map<Node, AstCoverageStatus> statusPerNode;

    public AstCoverage() {
        statusPerNode = new IdentityHashMap<>();
    }

    public AstCoverageStatus get(Node node) {
        return statusPerNode.getOrDefault(node, AstCoverageStatus.empty());
    }

    public void put(Node node, AstCoverageStatus status) {
        statusPerNode.put(node, status);
    }

    /**
     * Transforms a node's status with the given update function.
     */
    public void update(Node node, UnaryOperator<AstCoverageStatus> updater) {
        AstCoverageStatus status = get(node);
        statusPerNode.put(node, updater.apply(status));
    }

    /**
     * Updates a node's status via the {@link AstCoverageStatus#updateStatus(LineCoverageStatus)} method.
     */
    public void updateStatus(Node node, LineCoverageStatus newStatus) {
        AstCoverageStatus status = get(node);
        statusPerNode.put(node, status.updateStatus(newStatus));
    }

    /**
     * Updates a node's status via the {@link AstCoverageStatus#updateStatus(Status)} method.
     */
    public void updateStatus(Node node, Status newStatus) {
        AstCoverageStatus status = get(node);
        statusPerNode.put(node, status.updateStatus(newStatus));
    }

    public Map<Node, AstCoverageStatus> getMap() {
        return Collections.unmodifiableMap(statusPerNode);
    }
}
