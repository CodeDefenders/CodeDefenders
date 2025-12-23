package org.codedefenders.validation.code;

import com.github.javaparser.ast.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class TestRule {
    private final String generalDescription;
    private final String detailedDescription;
    private final String validationMessage;

    private final List<Predicate<TestCodeVisitor>> visitorRules;
    private final List<Predicate<Node>> stmtRules;

    private TestRule(String generalDescription, String detailedDescription, String validationMessage,
                    List<Predicate<TestCodeVisitor>> visitorRules,
                    List<Predicate<Node>> stmtRules) {
        this.generalDescription = generalDescription;
        this.detailedDescription = detailedDescription;
        this.validationMessage = validationMessage;
        this.visitorRules = visitorRules;
        this.stmtRules = stmtRules;
    }

    public String getGeneralDescription() {
        return generalDescription;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    void addVisitorRule(Predicate<TestCodeVisitor> rule) {
        visitorRules.add(rule);
    }

    void addStmtRule(Predicate<Node> rule) {
        stmtRules.add(rule);
    }

    boolean fails(TestCodeVisitor visitor) {
        return visitorRules.stream().anyMatch(r -> r.test(visitor));
    }

    boolean fails(Node n) {
        return stmtRules.stream().anyMatch(r -> r.test(n));
    }

    static class Builder {
        private final String generalDescription;
        private final String detailedDescription;
        private final String validationMessage;
        private final List<Predicate<TestCodeVisitor>> visitorRules = new ArrayList<>();
        private final List<Predicate<Node>> nodeRules = new ArrayList<>();

        Builder(String generalDescription, String detailedDescription, String validationMessage) {
            this.generalDescription = generalDescription;
            this.detailedDescription = detailedDescription;
            this.validationMessage = validationMessage;
        }

        Builder withVisitor(Predicate<TestCodeVisitor> rule) {
            visitorRules.add(rule);
            return this;
        }

        Builder withNode(Predicate<Node> rule) {
            nodeRules.add(rule);
            return this;
        }

        TestRule build() {
            return new TestRule(generalDescription, detailedDescription, validationMessage, visitorRules, nodeRules);
        }
    }
}
