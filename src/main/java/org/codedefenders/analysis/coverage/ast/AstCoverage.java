package org.codedefenders.analysis.coverage.ast;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.codedefenders.analysis.coverage.line.LineCoverageStatus;

import com.github.javaparser.ast.Node;

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

    public void update(Node node, UnaryOperator<AstCoverageStatus> updater) {
        AstCoverageStatus status = get(node);
        statusPerNode.put(node, updater.apply(status));
    }

    public void updateStatus(Node node, LineCoverageStatus newStatus) {
        AstCoverageStatus status = get(node);
        statusPerNode.put(node, status.updateStatus(newStatus));
    }

    public Map<Node, AstCoverageStatus> getMap() {
        return Collections.unmodifiableMap(statusPerNode);
    }
}
