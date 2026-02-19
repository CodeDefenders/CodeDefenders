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
import java.util.function.Function;
import java.util.function.Predicate;

import com.github.difflib.patch.AbstractDelta;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

/**
 * This rule contains one or more 'sub-rules' that can detect certain features in a mutant.
 *
 * <p>
 * All sub-rules within a rule should work to detect the same kind of behavior, similar enough that one rule-description
 * and one error message fit all sub-rules.
 *
 * <p>
 * For explanations of the different sub-rule types see their associated Builder methods.
 */
public class MutantRule extends ValidationRule {

    private final List<BiPredicate<CompilationUnit, CompilationUnit>> compilationUnitRules;
    private final List<LineDiffRule> lineDiffRules;
    private final List<BiPredicate<String, String>> codeRules;
    private final List<Predicate<Node>> insertionNodeRules;
    private final List<String[]> insertionRules;


    private MutantRule(String generalDescription, String detailedDescription, String message, boolean visible,
                       List<String[]> insertionRules,
                       List<Predicate<Node>> insertionNodeRules,
                       List<BiPredicate<String, String>> codeRules,
                       List<LineDiffRule> lineDiffRules,
                       List<BiPredicate<CompilationUnit, CompilationUnit>> compilationUnitRules) {
        super(generalDescription, detailedDescription, message, visible);
        this.insertionRules = insertionRules;
        this.insertionNodeRules = insertionNodeRules;
        this.codeRules = codeRules;
        this.lineDiffRules = lineDiffRules;
        this.compilationUnitRules = compilationUnitRules;
    }

    boolean fails(CompilationUnit original, CompilationUnit changed) {
        return compilationUnitRules.stream().anyMatch(r -> r.test(original, changed));
    }

    CodeValidationResult fails(List<AbstractDelta<String>> diff) {
        CodeValidationResult validationResult = new CodeValidationResult(CodeValidationResult.Type.MUTANT);
        for (LineDiffRule rule : lineDiffRules) {
            Optional<List<String>> result = rule.apply(diff);
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
        private final List<LineDiffRule> lineDiffRules = new ArrayList<>();
        private final List<BiPredicate<String, String>> codeRules = new ArrayList<>();
        private final List<Predicate<Node>> insertionNodeRules = new ArrayList<>();
        private final List<String[]> insertionRules = new ArrayList<>();
        private boolean visible = true;

        final String generalDescription;
        final String detailedDescription;
        final String message;

        /**
         * Creates the basis for a new mutant rule.
         * @param generalDescription The 'category' of rule. Rules with the same category will be grouped together under
         *                           this string in the GUI.
         * @param detailedDescription A more detailed description of this specific rule.
         * @param message The error message that is displayed if the rule is violated.
         */
        Builder(String generalDescription, String detailedDescription, String message) {
            this.generalDescription = generalDescription;
            this.detailedDescription = detailedDescription;
            this.message = message;
        }

        /**
         * Adds a compilation rule.
         * @param rule This predicate takes the {@link CompilationUnit} of the original (first param) and the mutated
         *             (2nd param) code and returns {@code true} if the rule is violated.
         */
        Builder withCompilation(BiPredicate<CompilationUnit, CompilationUnit> rule) {
            compilationUnitRules.add(rule);
            return this;
        }

        /**
         * Adds a line diff rule.
         *
         * <p>
         * If you only want to prevent the insertion of certain terms, use {@link Builder#withInsertion} instead.
         * @param rule A line diff rule is a function that takes a list of {@link AbstractDelta}<{@link String}>,
         *             that was generated from the difference between the original CuT and the mutant, and should
         *             return a list of lines from the mutant that violate this rule.
         *             If the rule is not violated, an empty {@link Optional} is returned.
         *             See {@link ValidationUtils#checkLineDiff(List, Predicate)} and its uses for examples.
         */
        Builder withLineDiff(LineDiffRule rule) {
            lineDiffRules.add(rule);
            return this;
        }

        /**
         * Adds a code rule.
         * @param rule A code rule simply takes the original code of the CuT as the first and the mutated code as
         *             the second argument and should return true if the mutant violates this rule.
         */
        Builder withCode(BiPredicate<String, String> rule) {
            codeRules.add(rule);
            return this;
        }

        /**
         * Adds an Insertion Node rule.
         * @param rule For parts of the diff that are labeled as 'Insertions' by the diff algorithm, we can parse the
         *             inserted code as an AST. For every node on this AST, this predicate is run, and it should return
         *             true if the insertion of such a node violates this rule.
         */
        Builder withInsertionNode(Predicate<Node> rule) {
            insertionNodeRules.add(rule);
            return this;
        }

        /**
         * Diffs are checked against the insertion of these items. This is used as a shorthand to avoid having to
         * write complicated lambdas in the line diff rules.
         * @param terms The terms that are forbidden from being inserted.
         */
        Builder withInsertion(String... terms) {
            insertionRules.add(terms);
            return this.withLineDiff(diff -> ValidationUtils.anyHasBeenAdded(diff, terms));
        }

        /**
         * Call this before building to hide the rule from ruleset descriptions. It will still be active,
         * and users will still see the validation messages if they violate the rule.
         */
        Builder hidden() {
            visible = false;
            return this;
        }

        MutantRule build() {
            return new MutantRule(generalDescription, detailedDescription, message, visible,
                    Collections.unmodifiableList(insertionRules),
                    Collections.unmodifiableList(insertionNodeRules),
                    Collections.unmodifiableList(codeRules),
                    Collections.unmodifiableList(lineDiffRules),
                    Collections.unmodifiableList(compilationUnitRules));
        }
    }

    @FunctionalInterface
    interface LineDiffRule extends Function<List<AbstractDelta<String>>, Optional<List<String>>> {

    }
}
