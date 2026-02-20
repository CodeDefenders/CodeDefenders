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

public enum PromptType {
    /**
     * The standard attack prompt. The code of the CuT is supplied as the user message for attacker prompts.
     */
    ATTACK_DEFAULT,
    /**
     * This String replaces the normal prompt if there are any dependency classes (and if
     * {@link LlModel#isAttackerDependencies()} is true).
     * The code of the dependencies will be appended to the code of the CuT as part of the user message.
     */
    ATTACK_DEPENDENCIES,
    /**
     * This prompt is used to generate a test to kill a suspected mutant. The user message consists of the CuT,
     * the dependencies if {@link LlModel#isAttackerDependencies()} is true, and the diff of the suspected mutant.
     */
    ATTACK_EQUIVALENCE,
    /**
     * The standard defender prompt. The code of the CuT is supplied as the user message.
     */
    DEFEND_DEFAULT,
    /**
     * This String replaces the normal defender if there are any dependency classes (and if
     * {@link LlModel#isDefenderDependencies()} is true).
     * The code of the dependencies will be appended to the code of the CuT as part of the user message.
     */
    DEFEND_DEPENDENCIES,
    /**
     * This String format replaces the normal or dependency prompt to focus on a specific method.
     * This should only be used with {@link String#format(String, Object...)} with the method name
     * as the single argument.
     */
    DEFEND_FOCUS,

    /**
     * Used for Whole-test-suite-strategies.
     */
    DEFEND_ONE_FROM_MANY
}
