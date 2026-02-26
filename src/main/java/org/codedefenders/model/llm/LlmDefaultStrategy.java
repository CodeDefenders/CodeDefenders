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

public enum LlmDefaultStrategy {
    TEST_DEFAULT(TestStrategyDefault.class),
    TEST_ANNOTATED_SINGLE_TEST(TestStrategyAnnotatedSingleTest.class),
    TEST_FULL_SUITE(TestStrategyFullSuite.class),
    TEST_FULL_SUITE_PLUS_DEFAULT(TestStrategyFullSuitePlusDefault.class),
    TEST_FULL_SUITE_PLUS_ANNOTATED(TestStrategyFullSuitePlusAnnotated.class),
    MUTANT_ANNOTATED_FULL_CLASS(MutantStrategyAnnotatedFullClass.class),
    MUTANT_ANNOTATED_SINGLE_METHOD(MutantStrategyAnnotatedSingleMethod.class, 0.5),
    MUTANT_DEFAULT(MutantStrategyDefault.class),
    MUTANT_RANDOM_SINGLE_METHOD(MutantStrategyRandomSingleMethod.class),
    MUTANT_DEFAULT_WITHOUT_EXISTING(MutantStrategyDefaultWithoutExisting.class),
    EQUIVALENCE_DEFAULT(LlmEquivalenceService.class);


    final Class<? extends LlmSubActionService> service;
    final double timeModifier;

    LlmDefaultStrategy(Class<? extends LlmSubActionService> service, double timeModifier) {
        this.service = service;
        this.timeModifier = timeModifier;
    }

    LlmDefaultStrategy(Class<? extends LlmSubActionService> service) {
        this(service, 1);
    }

    public List<LlmPromptType> getRelevantPrompts() {
        //TODO Adapt to only show relevant prompts - automatically would be nice, otherwise constructor-supplied
        return List.of(LlmPromptType.values());
    }
}
