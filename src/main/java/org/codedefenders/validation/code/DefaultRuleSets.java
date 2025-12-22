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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.validation.code.MutantValidationRules.*;

@Named("defaultRuleSets") //TODO Das muss wahrscheinlich alles woanders hin??
@ApplicationScoped
public class DefaultRuleSets {
    Logger logger = LoggerFactory.getLogger(DefaultRuleSets.class);

    private final MutantValidationRuleSet relaxed = new MutantValidationRuleSet("Relaxed")
            .addRule(noCommentsEqual)
            .addRule(packageDeclarations)
            .addRule(classDeclarations)
            .addRule(addOrRenameMethodsOrFields)
            .addRule(astEqual)
            .addRule(prohibitedCalls);

    private final MutantValidationRuleSet moderate = new MutantValidationRuleSet("Moderate", relaxed)
            .addRule(noChangesToComments)
            .addRule(logicalOperator)
            .addRule(prohibitedControlStructures);

    private final MutantValidationRuleSet strict = new MutantValidationRuleSet("Strict", moderate)
            .addRule(changesMethodSignatures)
            .addRule(changesImportStatements)
            .addRule(instanceofChanges)
            .addRule(prohibitedModifier)
            .addRule(prohibitedBitwiseOperators);

    @Inject
    public DefaultRuleSets() {

    }

    public MutantValidationRuleSet getRelaxed() {
        return relaxed;
    }

    public MutantValidationRuleSet getModerate() {
        return moderate;
    }

    public MutantValidationRuleSet getStrict() {
        return strict;
    }

    public MutantValidationRuleSet getRulesetFromName(String name) {
        logger.info("getRuleSet: {}", name);
        name = name.toLowerCase();
        return switch (name) {
            case "relaxed" -> relaxed;
            case "moderate" -> moderate;
            case "strict" -> strict;
            default -> throw new IllegalArgumentException("No such ruleset: " + name);
        };
    }

    public MutantValidationRuleSet getRuleSetFromEnum(CodeValidatorLevel level) {
        return switch (level) {
            case RELAXED -> relaxed;
            case MODERATE -> moderate;
            case STRICT -> strict;
        };
    }
}
