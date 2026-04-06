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
package org.codedefenders.model.llm;

import java.util.List;

import org.codedefenders.game.Role;
import org.codedefenders.service.llm.LlmEquivalenceService;
import org.codedefenders.service.llm.LlmSubActionService;
import org.codedefenders.service.llm.MutantStrategyAnnotatedFullClass;
import org.codedefenders.service.llm.MutantStrategyAnnotatedSingleMethod;
import org.codedefenders.service.llm.MutantStrategyDefault;
import org.codedefenders.service.llm.MutantStrategyDefaultWithoutExisting;
import org.codedefenders.service.llm.MutantStrategyRandomSingleMethod;
import org.codedefenders.service.llm.TestStrategyAnnotatedSingleTest;
import org.codedefenders.service.llm.TestStrategyDefault;
import org.codedefenders.service.llm.TestStrategyFullSuite;
import org.codedefenders.service.llm.TestStrategyFullSuitePlusAnnotated;
import org.codedefenders.service.llm.TestStrategyFullSuitePlusDefault;

import static org.codedefenders.game.Role.ATTACKER;
import static org.codedefenders.game.Role.DEFENDER;
import static org.codedefenders.model.llm.LlmPromptType.*;

public enum LlmDefaultStrategy {
    TEST_DEFAULT(TestStrategyDefault.class,
            DEFENDER,
            TEST_DEFAULT_DEFAULT_SYSTEM,
            TEST_DEFAULT_DEPENDENCY_SYSTEM,
            TEST_TEMPLATE_DEFAULT_FOCUS_SYSTEM),
    TEST_ANNOTATED_SINGLE_TEST(TestStrategyAnnotatedSingleTest.class,
            DEFENDER,
            TEST_ANNOTATED_DEFAULT_SYSTEM),
    TEST_FULL_SUITE(TestStrategyFullSuite.class,
            DEFENDER,
            TEST_FULL_SUITE_SYSTEM,
            TEST_FULL_SUITE_CORRECTION_SYSTEM,
            TEST_TEMPLATE_FULL_SUITE_USER,
            TEST_PARAMETER_TEMPLATE_DID_NOT_PASS,
            TEST_PARAMETER_TEMPLATE_RULE_VIOLATION,
            TEST_PARAMETER_TEMPLATE_COMPILATION_FAILED),
    TEST_FULL_SUITE_PLUS_DEFAULT(TestStrategyFullSuitePlusDefault.class,
            DEFENDER,
            TEST_DEFAULT_DEFAULT_SYSTEM,
            TEST_DEFAULT_DEPENDENCY_SYSTEM,
            TEST_TEMPLATE_DEFAULT_FOCUS_SYSTEM,
            TEST_FULL_SUITE_SYSTEM,
            TEST_FULL_SUITE_CORRECTION_SYSTEM,
            TEST_TEMPLATE_FULL_SUITE_USER,
            TEST_PARAMETER_TEMPLATE_DID_NOT_PASS,
            TEST_PARAMETER_TEMPLATE_RULE_VIOLATION,
            TEST_PARAMETER_TEMPLATE_COMPILATION_FAILED),
    TEST_FULL_SUITE_PLUS_ANNOTATED(TestStrategyFullSuitePlusAnnotated.class,
            DEFENDER,
            TEST_ANNOTATED_DEFAULT_SYSTEM,
            TEST_FULL_SUITE_SYSTEM,
            TEST_FULL_SUITE_CORRECTION_SYSTEM,
            TEST_TEMPLATE_FULL_SUITE_USER,
            TEST_PARAMETER_TEMPLATE_DID_NOT_PASS,
            TEST_PARAMETER_TEMPLATE_RULE_VIOLATION,
            TEST_PARAMETER_TEMPLATE_COMPILATION_FAILED),
    MUTANT_ANNOTATED_FULL_CLASS(MutantStrategyAnnotatedFullClass.class,
            ATTACKER,
            MUTANT_ANNOTATED_FULL_CLASS_DEFAULT_SYSTEM),
    MUTANT_ANNOTATED_SINGLE_METHOD(MutantStrategyAnnotatedSingleMethod.class,
            ATTACKER,
            0.5,
            MUTANT_ANNOTATED_SINGLE_METHOD_INITIAL_SYSTEM,
            MUTANT_ANNOTATED_SINGLE_METHOD_NO_SUCH_METHOD_SYSTEM,
            MUTANT_ANNOTATED_SINGLE_METHOD_FOLLOWUP_SYSTEM),
    MUTANT_DEFAULT(MutantStrategyDefault.class,
            ATTACKER,
            MUTANT_DEFAULT_DEFAULT_SYSTEM,
            MUTANT_TEMPLATE_DEFAULT_DIFFS_USER),
    MUTANT_RANDOM_SINGLE_METHOD(MutantStrategyRandomSingleMethod.class,
            ATTACKER,
            MUTANT_RANDOM_DEFAULT_SYSTEM),
    MUTANT_DEFAULT_WITHOUT_EXISTING(MutantStrategyDefaultWithoutExisting.class,
            ATTACKER,
            MUTANT_DEFAULT_WITHOUT_EXISTING_DEFAULT_SYSTEM),
    EQUIVALENCE_DEFAULT(LlmEquivalenceService.class,
            ATTACKER,
            EQUIVALENCE_DEFAULT_DEFAULT_SYSTEM);


    final Class<? extends LlmSubActionService> service;
    final Role role;
    final double timeModifier;
    final List<LlmPromptType> promptTypes;

    LlmDefaultStrategy(Class<? extends LlmSubActionService> service, Role role, double timeModifier,
                       LlmPromptType... promptTypes) {
        this.service = service;
        this.role = role;
        this.timeModifier = timeModifier;
        this.promptTypes = List.of(promptTypes);
    }

    LlmDefaultStrategy(Class<? extends LlmSubActionService> service, Role role, LlmPromptType... promptTypes) {
        this(service, role, 1, promptTypes);
    }

    public List<LlmPromptType> getRelevantPrompts() {
        return promptTypes;
    }
}
