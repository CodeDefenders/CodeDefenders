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

public enum LlmStrategy {
    TEST_DEFAULT,
    TEST_ANNOTATED_SINGLE_TEST,
    TEST_FULL_SUITE,
    TEST_FULL_SUITE_PLUS_DEFAULT,
    TEST_FULL_SUITE_PLUS_ANNOTATED,
    MUTANT_ANNOTATED_FULL_CLASS,
    MUTANT_ANNOTATED_SINGLE_METHOD(0.5),
    MUTANT_DEFAULT,
    MUTANT_RANDOM_SINGLE_METHOD,
    EQUIVALENCE_DEFAULT,
    INVALID;

    /**
     * The time between LLM actions is reduced by this multiplier.
     */
    final double timeModifier;

    LlmStrategy(double timeModifier) {
        this.timeModifier = timeModifier;
    }

    LlmStrategy() {
        this(1);
    }

    public double getTimeModifier() {
        return timeModifier;
    }

    /**
     * Returns the enum with the specified name, or {@link LlmStrategy#INVALID} if such a name does not exist.
     */
    public static LlmStrategy of(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return INVALID;
        }
    }
}
