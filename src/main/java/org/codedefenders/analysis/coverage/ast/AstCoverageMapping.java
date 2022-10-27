package org.codedefenders.analysis.coverage.ast;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;

public class AstCoverageMapping {
    private final Map<Node, AstCoverageStatus> statusPerNode;

    public AstCoverageMapping() {
        statusPerNode = new IdentityHashMap<>();
    }

    public AstCoverageStatus get(Node node) {
        return statusPerNode.getOrDefault(node, AstCoverageStatus.EMPTY);
    }

    public void put(Node node, AstCoverageStatus status) {
        statusPerNode.put(node, status);
    }

    public Map<Node, AstCoverageStatus> getMap() {
        return Collections.unmodifiableMap(statusPerNode);
    }
}
