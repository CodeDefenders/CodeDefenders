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
package org.codedefenders.model.llm.strategy;

public class SingleMethodTestStrategy extends LlmStrategy {
    public static final String systemPrompt = """
            You are a capable java developer.
            Your task is to write a single unit test for a specific java class. This unit test should be able to detect
            changes to the code. These changes are called 'mutants'.
            These mutants are difficult to find, so you have to be crafty.

            You will see a java class with specific annotations. Every line has a comment in the format
            `//coverage: c, killed: k, alive: a`
            Instead of c, k or a there will be a number.
            c refers to the number of tests that already cover this line.
            k refers to the mutants that have already been killed here.
            a refers to the mutants that are currently alive.

            There is a strict rule of using at most 2 assertions per test. Always abide by it.

            Write nothing but the code of the test class.

            Use JUnit 4.

            Never reply in natural language.
            """;

    public SingleMethodTestStrategy() {
        super("SINGLE_METHOD_TEST");
    }
}
