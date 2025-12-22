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

public class MutantValidationRuleSet {
    private final String name;
    private final List<MutantRule> rules = new ArrayList<>();
    private final MutantValidationRuleSet ancestor;



    public MutantValidationRuleSet(String name) {
        this.name = name;
        ancestor = null;
    }

    public MutantValidationRuleSet(String name, MutantValidationRuleSet from) {
        this.name = name;
        ancestor = from;
        rules.addAll(from.rules);
    }

    public MutantValidationRuleSet addRule(MutantRule rule) {
        rules.add(rule);
        return this;
    }

    public String getName() {
        return name;
    }

    public boolean contains(MutantRule rule) {
        return rules.contains(rule);
    }

    public Set<MutantRule> getRules() {
        return Set.copyOf(rules);
    }

    public List<List<MutantRule>> getTieredRules() {
        List<List<MutantRule>> result = new ArrayList<>();
        Set<MutantRule> unordered = getRules();
        outer:
        for (MutantRule r : unordered) {
            for (List<MutantRule> list : result) {
                if (!list.isEmpty() && list.get(0).generalDescription.equals(r.generalDescription)) {
                    list.add(r);
                    continue outer;
                }
            }
            List<MutantRule> newList = new ArrayList<>();
            newList.add(r);
            result.add(newList);
        }
        return result;
    }
}
