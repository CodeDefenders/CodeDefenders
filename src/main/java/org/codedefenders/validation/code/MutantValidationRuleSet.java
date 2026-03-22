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
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * A set of different rules, for example, the {@link DefaultRuleSets#STRICT} rules. Each set has a name, and it can
 * have a relationship with other sets.
 */
public class MutantValidationRuleSet {
    private final String name;
    private final List<MutantRule> rules = new ArrayList<>();
    private final MutantValidationRuleSet parent;
    private final List<MutantValidationRuleSet> children = new ArrayList<>();

    MutantValidationRuleSet(String name) {
        this.name = name;
        parent = null;
    }

    MutantValidationRuleSet(String name, MutantValidationRuleSet from) {
        this.name = name;
        parent = from;
        parent.children.add(this);
        rules.addAll(from.rules);
    }

    MutantValidationRuleSet addRule(MutantRule rule) {
        rules.add(rule);
        return this;
    }

    public String getName() {
        return name;
    }

    public Set<MutantRule> getRules() {
        return Set.copyOf(rules);
    }

    public List<List<MutantRule>> getTieredRules() {
        return ValidationUtils.getTieredRules(rules);
    }

    public List<MutantRule> getSingleRules() {
        return ValidationUtils.getSingleRules(rules);
    }

    MutantValidationRuleSet getParent() {
        return parent;
    }

    List<MutantValidationRuleSet> getChildren() {
        return children;
    }

    /**
     * Returns all descendants of this rule set, including itself.
     */
    List<MutantValidationRuleSet> getDescendants() {
        List<MutantValidationRuleSet> results = new ArrayList<>();
        results.add(this);
        for (MutantValidationRuleSet child : children) {
            results.addAll(child.getDescendants());
        }
        return results;
    }

    /**
     * Returns all ancestors of this rule set, <b>not</b> including itself.
     */
    List<MutantValidationRuleSet> getAncestors() {
        List<MutantValidationRuleSet> result = new ArrayList<>();
        for (MutantValidationRuleSet i = parent; i != null; i = i.parent) {
            result.add(i);
        }
        return result;
    }

    @Override
    public @NotNull String toString() {
        return "MutantRuleSet:" + name;
    }
}
