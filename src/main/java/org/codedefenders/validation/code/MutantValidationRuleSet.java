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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;

public class MutantValidationRuleSet {
    private final String name;
    private final List<MutantComparisonRule<CompilationUnit>> compilationUnitRules = new ArrayList<>();
    private final List<MutantComparisonRule<List<List<String>>>> linediffRules = new ArrayList<>();
    private final List<MutantComparisonRule<String>> codeRules = new ArrayList<>();
    private final List<MutantInsertionRule> insertionRules = new ArrayList<>();



    public MutantValidationRuleSet(String name) {
        this.name = name;
    }

    public MutantValidationRuleSet(String name, MutantValidationRuleSet from) {
        this.name = name;
        compilationUnitRules.addAll(from.compilationUnitRules);
        linediffRules.addAll(from.linediffRules);
        codeRules.addAll(from.codeRules);
        insertionRules.addAll(from.insertionRules);
    }

    public MutantValidationRuleSet addCompRule(MutantComparisonRule<CompilationUnit> rule) {
        compilationUnitRules.add(rule);
        return this;
    }

    public MutantValidationRuleSet addDiffRule(MutantComparisonRule<List<List<String>>> rule) {
        linediffRules.add(rule);
        return this;
    }

    public MutantValidationRuleSet addCodeRule(MutantComparisonRule<String> rule) {
        codeRules.add(rule);
        return this;
    }

    public MutantValidationRuleSet addInsertionRule(MutantInsertionRule rule) {
        insertionRules.add(rule);
        return this;
    }

    public String getName() {
        return name;
    }

    public List<MutantComparisonRule<CompilationUnit>> getCompilationUnitRules() {
        return compilationUnitRules;
    }

    public List<MutantComparisonRule<List<List<String>>>> getLinediffRules() {
        return linediffRules;
    }

    public List<MutantComparisonRule<String>> getCodeRules() {
        return codeRules;
    }

    public List<MutantInsertionRule> getInsertionRules() {
        return insertionRules;
    }

    public boolean contains(MutantRule rule) {
        if (rule instanceof MutantComparisonRule<?> comparisonRule) {
            return compilationUnitRules.contains(comparisonRule) || linediffRules.contains(comparisonRule)
                    || codeRules.contains(comparisonRule);
        } else if (rule instanceof MutantInsertionRule insertionRule) {
            return insertionRules.contains(insertionRule);
        } else throw new RuntimeException("Rules of this type are not supported: " + rule);
    }

    public Set<MutantRule> getAllRules() {
        Set<MutantRule> result = new HashSet<>(compilationUnitRules);
        result.addAll(codeRules);
        result.addAll(linediffRules);
        result.addAll(insertionRules);
        return Set.copyOf(result);
    }

    public Set<String> getGeneralDescriptions() {
        Set<String> descriptions = new HashSet<>();
        for (MutantRule rule : getAllRules()) {
            descriptions.add(rule.getGeneralDescription());
        }
        return descriptions;
    }
}
