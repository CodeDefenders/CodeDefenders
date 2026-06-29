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
import org.codedefenders.llm.AbstractStrategy;
import org.codedefenders.llm.EquivalenceStrategy;
import org.codedefenders.llm.MutantStrategyAnnotatedFullClass;
import org.codedefenders.llm.MutantStrategyAnnotatedSingleMethod;
import org.codedefenders.llm.MutantStrategyDefault;
import org.codedefenders.llm.MutantStrategyDefaultWithoutExisting;
import org.codedefenders.llm.MutantStrategyRandomSingleMethod;
import org.codedefenders.llm.TestStrategyAnnotatedSingleTest;
import org.codedefenders.llm.TestStrategyDefault;
import org.codedefenders.llm.TestStrategyFullSuite;
import org.codedefenders.llm.TestStrategyFullSuitePlusAnnotated;
import org.codedefenders.llm.TestStrategyFullSuitePlusDefault;

import static org.codedefenders.game.Role.ATTACKER;
import static org.codedefenders.game.Role.DEFENDER;
import static org.codedefenders.model.llm.LlmPromptType.*;

/**
 * This enum lists the hard-coded default strategies. They cannot be customized by the user, but
 * customized variations of them can be created by users.
 *
 * <p>
 * Every enum value contains the {@link Class} of the strategy and a list of all prompt types that can
 * be customized for this strategy.
 */
public enum LlmDefaultStrategies {
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
    EQUIVALENCE_DEFAULT(EquivalenceStrategy.class,
            ATTACKER,
            EQUIVALENCE_DEFAULT_DEFAULT_SYSTEM);


    final transient Class<? extends AbstractStrategy> service;
    final Role role;
    final double timeModifier;
    final List<LlmPromptType> promptTypes;

    LlmDefaultStrategies(Class<? extends AbstractStrategy> service, Role role, double timeModifier,
                         LlmPromptType... promptTypes) {
        this.service = service;
        this.role = role;
        this.timeModifier = timeModifier;
        this.promptTypes = List.of(promptTypes);
    }

    LlmDefaultStrategies(Class<? extends AbstractStrategy> service, Role role, LlmPromptType... promptTypes) {
        this(service, role, 1, promptTypes);
    }

    public List<LlmPromptType> getRelevantPrompts() {
        return promptTypes;
    }
}
