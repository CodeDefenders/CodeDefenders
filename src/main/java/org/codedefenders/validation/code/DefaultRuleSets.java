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
import org.xnap.commons.i18n.I18n;

import static org.codedefenders.validation.code.MutantValidationRules.addOrRenameFields;
import static org.codedefenders.validation.code.MutantValidationRules.addOrRenameMethods;
import static org.codedefenders.validation.code.MutantValidationRules.astEqual;
import static org.codedefenders.validation.code.MutantValidationRules.changesImportStatements;
import static org.codedefenders.validation.code.MutantValidationRules.changesMethodSignatures;
import static org.codedefenders.validation.code.MutantValidationRules.classDeclarations;
import static org.codedefenders.validation.code.MutantValidationRules.instanceofChanges;
import static org.codedefenders.validation.code.MutantValidationRules.logicalOperator;
import static org.codedefenders.validation.code.MutantValidationRules.noChangesToComments;
import static org.codedefenders.validation.code.MutantValidationRules.noCommentsEqual;
import static org.codedefenders.validation.code.MutantValidationRules.noDate;
import static org.codedefenders.validation.code.MutantValidationRules.noIO;
import static org.codedefenders.validation.code.MutantValidationRules.noRandom;
import static org.codedefenders.validation.code.MutantValidationRules.noSystemCalls;
import static org.codedefenders.validation.code.MutantValidationRules.noThreading;
import static org.codedefenders.validation.code.MutantValidationRules.packageDeclarations;
import static org.codedefenders.validation.code.MutantValidationRules.prohibitedBitwiseOperators;
import static org.codedefenders.validation.code.MutantValidationRules.prohibitedConditionals;
import static org.codedefenders.validation.code.MutantValidationRules.prohibitedLoops;
import static org.codedefenders.validation.code.MutantValidationRules.prohibitedModifier;

@Named("defaultRuleSets")
@ApplicationScoped
public class DefaultRuleSets {
    private static final Logger logger = LoggerFactory.getLogger(DefaultRuleSets.class);

    public static final MutantValidationRuleSet RELAXED = new MutantValidationRuleSet(I18n.marktr("Relaxed"))
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

    public static final MutantValidationRuleSet MODERATE = new MutantValidationRuleSet(I18n.marktr("Moderate"), RELAXED)
            .addRule(noChangesToComments)
            .addRule(logicalOperator)
            .addRule(prohibitedConditionals)
            .addRule(prohibitedLoops);

    public static final MutantValidationRuleSet STRICT = new MutantValidationRuleSet(I18n.marktr("Strict"), MODERATE)
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
        name = name.toLowerCase();
        return switch (name) {
            case "relaxed" -> RELAXED;
            case "moderate" -> MODERATE;
            case "strict" -> STRICT;
            default -> throw new IllegalArgumentException("No such ruleset: " + name);
        };
    }
}
