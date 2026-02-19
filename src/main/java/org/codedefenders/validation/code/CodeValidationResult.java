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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;

import com.github.javaparser.ast.Node;

public class CodeValidationResult {
    private final Type type;

    private final List<ImmutablePair<ValidationRule, Node>> nodeErrors = new ArrayList<>();
    private final List<ImmutablePair<ValidationRule, String>> stringErrors = new ArrayList<>();
    private final List<ValidationRule> anonymousErrors = new ArrayList<>();
    private int maxNumberOfAssertions = 0;

    private boolean failedParsing = false;

    CodeValidationResult(Type type) {
        this.type = type;
    }

    void add(ValidationRule rule, Node origin) {
        if (nodeErrors.stream().noneMatch(p -> p.left == rule)) {
            nodeErrors.add(new ImmutablePair<>(rule, origin));
            stringErrors.removeIf(p -> p.left == rule);
            anonymousErrors.removeIf(r -> r == rule);
        }
    }

    void add(ValidationRule rule, String origin) {
        if (nodeErrors.stream().noneMatch(p -> p.left == rule)
                && stringErrors.stream().noneMatch(p -> p.left == rule)) {
            stringErrors.add(new ImmutablePair<>(rule, origin));
            anonymousErrors.removeIf(r -> r == rule);
        }
    }

    void add(ValidationRule rule) {
        if (nodeErrors.stream().noneMatch(p -> p.left == rule)
                && stringErrors.stream().noneMatch(p -> p.left == rule)
                && !anonymousErrors.contains(rule)) {
            anonymousErrors.add(rule);
        }
    }

    void add(CodeValidationResult toAdd) {
        if (type != toAdd.type) {
            throw new IllegalArgumentException("Trying to add resultType " + toAdd.type + " to resultType " + type);
        }

        for (var p : toAdd.nodeErrors) {
            add(p.left, p.right);
        }
        for (var p : toAdd.stringErrors) {
            add(p.left, p.right);
        }
        for (var r : toAdd.anonymousErrors) {
            add(r);
        }

        failedParsing |= toAdd.failedParsing;
    }

    void setMaxNumberOfAssertions(int maxNumberOfAssertions) {
        this.maxNumberOfAssertions = maxNumberOfAssertions;
    }

    /**
     * Only relevant for tests.
     */
    void setFailedParsing() {
        failedParsing = true;
    }


    @Override
    public @NotNull String toString() {
        if (isValid()) {
            return failedParsing ? ValidationMessage.VALIDATION_FAILED_PARSING : ValidationMessage.VALIDATION_SUCCESS;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Your ").append(type == Type.TEST ? "test" : "mutant")
                .append(" is not valid, sorry! It failed for the following reasons:\n");

        int counter = 1;

        for (ImmutablePair<ValidationRule, Node> error : nodeErrors) {
            sb.append(counter++)
                    .append(":\t")
                    .append(error.left.getValidationMessage())
                    .append(" - Offending statement:\n\t\t")
                    .append(error.right.toString().replace("\n", "\n\t\t"))
                    .append('\n');
        }

        for (ImmutablePair<ValidationRule, String> error : stringErrors) {
            sb.append(counter++)
                    .append(":\t")
                    .append(error.left.getValidationMessage())
                    .append(" - Offending statement:\n\t\t")
                    .append(error.right.replace("\n", "\n\t\t"))
                    .append('\n');
        }

        for (ValidationRule error : anonymousErrors) {
            sb.append(counter++)
                    .append(":\t")
                    .append(error.getValidationMessage())
                    .append('\n');
        }
        return sb.toString().replace("${MAX_ASSERTIONS}", String.valueOf(maxNumberOfAssertions));
    }

    /**
     * Returns true in two cases: Either, validation succeeded without issues, or the code could not be parsed.
     * In both cases, we attempt to compile the code, as the compiler will yield a more useful error message than
     * what we could create ourselves.
     */
    public boolean isValid() {
        return nodeErrors.isEmpty() && stringErrors.isEmpty() && anonymousErrors.isEmpty();
    }

    public List<ValidationRule> getViolatingRules() {
        List<ValidationRule> result = new ArrayList<>();
        for (var x : nodeErrors) {
            result.add(x.left);
        }
        for (var x : stringErrors) {
            result.add(x.left);
        }
        result.addAll(anonymousErrors);
        return result;
    }

    enum Type {
        TEST, MUTANT
    }
}
