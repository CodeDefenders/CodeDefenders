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

import java.util.ArrayList;
import java.util.List;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.servlets.games.GameManagingUtils;

public class FullTestSuiteStrategy extends LlmStrategy {
    private List<String> tests = new ArrayList<>();

    public final String fullSuitePrompt = """
            You will see a class of java code. Write a complete test suite for it, using JUnit4.

            There is a strict rule of using at most 2 assertions per test. Always abide by it.

            Write nothing but the code of the test class.

            Never reply in natural language.
            """;

    public final String correctionSystemPrompt = """
            You will see 3 things:
            1. The code of a java class under test.
            2. The code of a test method. This test has at least one issue.
            3. An explanation of the issue the test has.

            Your task is to fix the issue. The response should consist of nothing but the fixed test code.
            If the test cannot be fixed, it is acceptable to write a new test.

            A test should never have more than 2 assertions.
            A test should only use JUnit4.

            Write nothing but the test code.
            Never use natural language.
            """;

    public FullTestSuiteStrategy() {
        super("FULL_TEST_SUITE");
    }

    public String getOneTest() {
        return tests.remove(0);
    }

    public boolean isEmpty() {
        return tests.isEmpty();
    }

    public void addTest(String test) {
        if (test == null) {
            throw new NullPointerException("Test may not be null");
        }
        tests.add(test);
    }

    public String getCorrectionUserMessage(AbstractGame game, String testCode,
                                           GameManagingUtils.CreateBattlegroundTestResult result) {

        StringBuilder sb = new StringBuilder();
        sb.append("1: Class under test:\n");
        sb.append(game.getCUT().getSourceCode());
        sb.append("\n\n2: Test method:\n");
        sb.append(testCode);
        sb.append("\n\n3: Issues:\n");

        switch (result.failureReason().orElseThrow()) {
            case TEST_DID_NOT_PASS_ON_CUT ->
                    sb.append("It did not pass on the original code for the following reason: ")
                            .append(result.testCutError().orElseThrow());
            case COMPILATION_FAILED ->
                    sb.append("It has failed to compile for this reason: ").append(result.compilationError());
            case VALIDATION_FAILED -> {
                sb.append("It has violated these rules: \n");
                result.validationErrorMessages().orElseThrow().forEach(
                        sb::append
                );

            }
        }
        return sb.toString();
    }
}
