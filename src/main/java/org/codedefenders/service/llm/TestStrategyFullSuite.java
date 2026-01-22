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
package org.codedefenders.service.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.model.llm.PromptType;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.LlmUtils;

@RequestScoped
public class TestStrategyFullSuite extends LlmTestService {
    //private final List<String> tests = new ArrayList<>();
    static LlmStrategy strategy = LlmStrategy.TEST_FULL_SUITE;

    private static final String fullSuitePrompt = """
            You are an experienced Java developer.

            You will see a class of java code. Write a complete test suite for it.

            There is a strict rule of using at most 2 assertions per test. Always abide by it.

            Every test must be self-contained, do not use any setup or utility methods.

            Write nothing but the code of the test class.

            Use JUnit 4.

            Never reply in natural language.
            """;

    private static final String correctionSystemPrompt = """
            You are an experienced Java developer.

            You will see 3 things:
            1. The code of a java class under test.
            2. The code of a test method. This test has at least one issue.
            3. An explanation of the issue the test has.

            Your task is to fix the issue. The response should consist of nothing but the fixed test code.
            If the test cannot be fixed, it is acceptable to write a new test.

            There is a strict rule of using at most 2 assertions per test. Always abide by it.

            Write nothing but the test code.

            Use JUnit 4.

            Never reply in natural language.
            """;

    private FullSuiteBaggage baggage() {
        if (conversationBatch.getBaggage() == null) {
            conversationBatch.setBaggage(new FullSuiteBaggage());
        }
        return (FullSuiteBaggage)conversationBatch.getBaggage();
    }

    public String getOneTest() {
        return baggage().tests.remove(0);
    }

    public boolean isEmpty() {
        return baggage().tests.isEmpty();
    }

    public void addTest(String test) {
        if (test == null) {
            throw new NullPointerException("Test may not be null");
        }
        baggage().tests.add(test);
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

    @Override
    public Optional<String> generate() {
        if (isEmpty()) {
            setConversationType(PromptType.DEFEND_DEFAULT);
        } else {
            setConversationType(PromptType.DEFEND_ONE_FROM_MANY);
        }
        if (conversation.numberOfTries() > numberOfRepairAttempts) {
            finishConversation(false);
            conversation = conversationBatch.getConversation(PromptType.DEFEND_DEFAULT);
        }
        if (conversation.getType() == PromptType.DEFEND_DEFAULT) {
            conversation.addSystemMessage(fullSuitePrompt, model);
            conversation.addUserMessage(getSourceCodeForUserMessage(), model);
            String reply = promptService.getResponse(model, conversation);
            LlmUtils.suiteOfTestTemplatesFromReply(reply, game).forEach(this::addTest);
            if (isEmpty()) {
                //Not a single valid test was generated
                finishConversation(false);
                return Optional.empty();
            } else {
                finishConversation(true);
                setConversationType(PromptType.DEFEND_ONE_FROM_MANY);
            }
        }
        if (!conversation.isEmpty()) {
            //There was an error on the last submission
            String reply = promptService.getResponse(model, conversation);
            return Optional.of(LlmUtils.testTemplateFromReply(reply, game));
        } else {
            finishConversation(true);
            setConversationType(PromptType.DEFEND_ONE_FROM_MANY);
            return Optional.of(getOneTest());
        }
    }

    @Override
    protected void onSubmitSuccess() {
        if (isEmpty()  || !conversation.isEmpty()) {
            finishConversation(true);
        }
    }

    @Override
    protected void onSubmitFailure(GameManagingUtils.CreateBattlegroundTestResult result, String testSrc) {
        if (!conversation.hasSystemMessage()) {
            conversation.addSystemMessage(correctionSystemPrompt, model);
        }
        String testContent = LlmUtils.extractTestContentFromReply(testSrc);
        String userMessage = getCorrectionUserMessage(game, testContent, result);
        conversation.addUserMessage(userMessage, model);
    }

    static class FullSuiteBaggage {
        private final List<String> tests = new ArrayList<>();
        boolean isEmpty() {
            return tests.isEmpty();
        }
    }
}
