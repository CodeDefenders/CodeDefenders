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

import static org.codedefenders.validation.code.MutantValidationRules.*;

public class DefaultRuleSets {
    public static MutantValidationRuleSet relaxed = new MutantValidationRuleSet("Relaxed")
            .addCompRule(noCommentsEqual)
            .addCompRule(packageDeclarations)
            .addCompRule(classDeclarations)
            .addCompRule(addOrRenameMethodsOrFields)
            .addCompRule(astEqual)
            .addInsertionRule(prohibitedCalls);

    public static MutantValidationRuleSet moderate = new MutantValidationRuleSet("Moderate", relaxed)
            .addCompRule(noChangesToComments)
            .addDiffRule(ternaryOperators)
            .addDiffRule(logicalOperator)
            .addInsertionRule(prohibitedControlStructures)
            .addInsertionRule(commentTokens);

    public static MutantValidationRuleSet strict = new MutantValidationRuleSet("Strict", moderate)
            .addCompRule(changesMethodSignatures)
            .addCompRule(changesImportStatements)
            .addCompRule(instanceofChanges)
            .addCodeRule(prohibitedModifier)
            .addInsertionRule(prohibitedBitwiseOperators);

}
