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

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.validation.code.MutantValidationRules.*;

@Named("defaultRuleSets")
@ApplicationScoped
public class DefaultRuleSets {
    private static final Logger logger = LoggerFactory.getLogger(DefaultRuleSets.class);

    public static final MutantValidationRuleSet RELAXED = new MutantValidationRuleSet("Relaxed")
            .addRule(noCommentsEqual)
            .addRule(packageDeclarations)
            .addRule(classDeclarations)
            .addRule(addOrRenameMethods)
            .addRule(addOrRenameFields)
            .addRule(astEqual)
            .addRule(noSystemCalls)
            .addRule(noThreading)
            .addRule(noIO)
            .addRule(noDate)
            .addRule(noRandom);

    public static final MutantValidationRuleSet MODERATE = new MutantValidationRuleSet("Moderate", RELAXED)
            .addRule(noChangesToComments)
            .addRule(logicalOperator)
            .addRule(prohibitedConditionals)
            .addRule(prohibitedLoops);

    public static final MutantValidationRuleSet STRICT = new MutantValidationRuleSet("Strict", MODERATE)
            .addRule(changesMethodSignatures)
            .addRule(changesImportStatements)
            .addRule(instanceofChanges)
            .addRule(prohibitedModifier)
            .addRule(prohibitedBitwiseOperators);

    @Inject
    public DefaultRuleSets() {

    }

    public static List<MutantValidationRuleSet> getValues() {
        return List.of(RELAXED, MODERATE, STRICT);
    }

    public MutantValidationRuleSet getRelaxed() {
        return RELAXED;
    }

    public MutantValidationRuleSet getModerate() {
        return MODERATE;
    }

    public MutantValidationRuleSet getStrict() {
        return STRICT;
    }

    public static MutantValidationRuleSet getRulesetFromName(String name) {
        logger.info("getRuleSet: {}", name);
        name = name.toLowerCase();
        return switch (name) {
            case "relaxed" -> RELAXED;
            case "moderate" -> MODERATE;
            case "strict" -> STRICT;
            default -> throw new IllegalArgumentException("No such ruleset: " + name);
        };
    }
}
