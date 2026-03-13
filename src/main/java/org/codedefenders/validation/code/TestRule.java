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
import java.util.function.Predicate;

import com.github.javaparser.ast.Node;

/**
 * This rule contains one or more 'sub-rules' that can detect certain features in a test.
 *
 * <p>
 * All sub-rules within a rule should work to detect the same kind of behavior, similar enough that one rule-description
 * and one error message fit all sub-rules.
 *
 * <p>
 * For explanations of the different sub-rule types see their associated Builder methods.
 */
public class TestRule extends ValidationRule {

    private final List<Predicate<TestValidationDTO>> visitorRules;
    private final List<Predicate<Node>> stmtRules;

    private TestRule(String generalDescription, String detailedDescription, String validationMessage,
                     boolean visible,
                     List<Predicate<TestValidationDTO>> visitorRules,
                     List<Predicate<Node>> stmtRules
    ) {
        super(generalDescription, detailedDescription, validationMessage, visible);
        this.visitorRules = visitorRules;
        this.stmtRules = stmtRules;
    }

    boolean fails(TestValidationDTO visitor) {
        return visitorRules.stream().anyMatch(r -> r.test(visitor));
    }

    boolean fails(Node n) {
        return stmtRules.stream().anyMatch(r -> r.test(n));
    }

    static class Builder {
        private final String generalDescription;
        private final String detailedDescription;
        private final String validationMessage;
        private final List<Predicate<TestValidationDTO>> visitorRules = new ArrayList<>();
        private final List<Predicate<Node>> nodeRules = new ArrayList<>();
        private boolean visible = true;

        Builder(String generalDescription, String detailedDescription, String validationMessage) {
            this.generalDescription = generalDescription;
            this.detailedDescription = detailedDescription;
            this.validationMessage = validationMessage;
        }

        /**
         * Adds a rule that takes a {@link TestValidationDTO} as an argument. The predicate will only be checked
         * after the {@link TestValidator} has walked through the AST.
         */
        Builder withVisitor(Predicate<TestValidationDTO> rule) {
            visitorRules.add(rule);
            return this;
        }

        /**
         * Adds a rule that takes a {@link Node} as an argument. The predicate will be checked while the
         * {@link TestValidator} is walking through the AST.
         */
        Builder withNode(Predicate<Node> rule) {
            nodeRules.add(rule);
            return this;
        }

        /**
         * Will make this rule invisible in the rule descriptions.
         */
        Builder hidden() {
            visible = false;
            return this;
        }

        TestRule build() {
            return new TestRule(generalDescription,
                    detailedDescription,
                    validationMessage,
                    visible,
                    visitorRules,
                    nodeRules);
        }
    }
}
