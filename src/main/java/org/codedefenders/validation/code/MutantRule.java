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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

public class MutantRule extends ValidationRule {

    private final List<BiPredicate<CompilationUnit, CompilationUnit>> compilationUnitRules;
    private final List<LineDiffRule> linediffRules;
    private final List<BiPredicate<String, String>> codeRules;
    private final List<Predicate<Node>> insertionNodeRules;
    private final List<String[]> insertionRules;


    private MutantRule(String generalDescription, String detailedDescription, String message, boolean visible,
                       List<String[]> insertionRules,
                       List<Predicate<Node>> insertionNodeRules,
                       List<BiPredicate<String, String>> codeRules,
                       List<LineDiffRule> linediffRules,
                       List<BiPredicate<CompilationUnit, CompilationUnit>> compilationUnitRules) {
        super(generalDescription, detailedDescription, message, visible);
        this.insertionRules = insertionRules;
        this.insertionNodeRules = insertionNodeRules;
        this.codeRules = codeRules;
        this.linediffRules = linediffRules;
        this.compilationUnitRules = compilationUnitRules;
    }

    boolean fails(CompilationUnit original, CompilationUnit changed) {
        return compilationUnitRules.stream().anyMatch(r -> r.test(original, changed));
    }

    CodeValidationResult fails(List<List<String>> original, List<List<String>> changed) {
        CodeValidationResult validationResult = new CodeValidationResult(CodeValidationResult.Type.MUTANT);
        for (LineDiffRule rule : linediffRules) {
            Optional<List<String>> result = rule.apply(original, changed);
            if (result.isPresent()) {
                if (result.get().isEmpty()) {
                    validationResult.add(this);
                } else if (result.get().size() == 1) {
                    validationResult.add(this, result.get().get(0));
                } else {
                    validationResult.add(this, String.join("\n", result.get()));
                }
            }
        }
        return validationResult;
    }

    boolean fails(String original, String changed) {
        return codeRules.stream().anyMatch(r -> r.test(original, changed));
    }

    boolean fails(String diff) {
        return insertionRules.stream().anyMatch(r -> ValidationUtils.containsAny(diff, r));
    }

    boolean fails(Node node) {
        return insertionNodeRules.stream().anyMatch(p -> p.test(node));
    }

    static class Builder {
        private final List<BiPredicate<CompilationUnit, CompilationUnit>> compilationUnitRules = new ArrayList<>();
        private final List<LineDiffRule> linediffRules = new ArrayList<>();
        private final List<BiPredicate<String, String>> codeRules = new ArrayList<>();
        private final List<Predicate<Node>> insertionNodeRules = new ArrayList<>();
        private final List<String[]> insertionRules = new ArrayList<>();
        private boolean visible = true;

        final String generalDescription;
        final String detailedDescription;
        final String message;

        Builder(String generalDescription, String detailedDescription, String message) {
            this.generalDescription = generalDescription;
            this.detailedDescription = detailedDescription;
            this.message = message;
        }

        Builder withCompilation(BiPredicate<CompilationUnit, CompilationUnit> rule) {
            compilationUnitRules.add(rule);
            return this;
        }

        Builder withLinediff(LineDiffRule rule) {
            linediffRules.add(rule);
            return this;
        }

        Builder withCode(BiPredicate<String, String> rule) {
            codeRules.add(rule);
            return this;
        }

        Builder withInsertionNode(Predicate<Node> rule) {
            insertionNodeRules.add(rule);
            return this;
        }

        Builder withInsertion(String... terms) {
            insertionRules.add(terms);
            return this.withLinediff((o, m) -> ValidationUtils.anyHasBeenAdded(o, m, terms));
        }

        Builder hidden() {
            visible = false;
            return this;
        }

        MutantRule build() {
            return new MutantRule(generalDescription, detailedDescription, message, visible,
                    Collections.unmodifiableList(insertionRules),
                    Collections.unmodifiableList(insertionNodeRules),
                    Collections.unmodifiableList(codeRules),
                    Collections.unmodifiableList(linediffRules),
                    Collections.unmodifiableList(compilationUnitRules));
        }
    }
}
