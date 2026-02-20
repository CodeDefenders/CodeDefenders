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

import java.util.Optional;

import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.model.llm.PromptType;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.LlmUtils;

@Strategy(LlmStrategy.TEST_DEFAULT)
public class TestStrategyDefault extends LlmTestService {

    private static final String defaultPrompt = """
            You are an experienced Java developer.

            Your task is to write a single unit test for a specific java class. This unit test should be able to detect
            changes to the code. These changes are called 'mutants'.
            These mutants are difficult to find, so you have to be crafty.

            You will see a class of Java code.
            The test must target this class.

            There is a strict rule of using at most 2 assertions. Always abide by it.

            Write nothing but the code of the single test.

            Use JUnit 4.

            Never reply in natural language.
            """;
    private static final String dependencyPrompt = """
            Write a single test for the first class of the following Java code using a maximum of 2 assertions.
            The other classes are dependencies of the first class, you don't need to test them.
            Write only the content of the test method, without including formatting, comments,
            the header or the method declaration. Use JUnit 4.""";//TODO Not in use for the experiment
    private static final String focusPrompt = """
            You are an experienced Java developer.

            Your task is to write a single unit test for a specific java class. This unit test should be able to detect
            changes to the code. These changes are called 'mutants'.
            These mutants are difficult to find, so you have to be crafty.

            You will see a class of Java code.
            The test must target the method %s.

            There is a strict rule of using at most 2 assertions. Always abide by it.

            Write nothing but the code of the single test.

            Never reply in natural language.
            """;

    @Override
    public Optional<String> generate() {
        PromptType promptType = getCorrectDefendPromptType();
        setConversationType(promptType);
        resetConversationAfterTooManyTries();
        if (!conversation.lastMessageWasError()) {
            {
                String systemMessage = getSystemPrompt(promptType);
                if (promptType == PromptType.DEFEND_FOCUS) {
                    Optional<String> methodName = getRandomMethodWithLivingMutant();
                    if (methodName.isPresent()) {
                        systemMessage = String.format(systemMessage, methodName.get());
                    }
                }
                conversation.addSystemMessage(systemMessage, model);

                conversation.addUserMessage(
                        getSourceCodeForUserMessage(), model);
            }
            String reply = promptService.getResponse(model, conversation);
            return Optional.of(LlmUtils.testTemplateFromReply(reply, game));
        } else {
            String response = promptService.getResponse(model, conversation);
            return Optional.of(LlmUtils.testTemplateFromReply(response, game));
        }
    }

    @Override
    protected void onSubmitSuccess() {
        finishConversation(true);
    }

    @Override
    protected void onSubmitFailure(GameManagingUtils.CreateBattlegroundTestResult result, String testSrc) {
        standardSubmitFailure(result, testSrc);
    }

    private String getSystemPrompt(PromptType promptType) {
        return switch (promptType) {
            case DEFEND_DEFAULT -> defaultPrompt;
            case DEFEND_DEPENDENCIES -> dependencyPrompt;
            case DEFEND_FOCUS -> focusPrompt;
            default -> throw new IllegalArgumentException("This prompt type is not allowed in a TestStrategyDefault: "
                    + promptType);
        };
    }

    private PromptType getCorrectDefendPromptType() {
        if (hasLivingMutants()) {
            return PromptType.DEFEND_FOCUS;
        }
        if (!game.getCUT().getDependencyNames().isEmpty()) {
            return PromptType.DEFEND_DEPENDENCIES;
        }
        return PromptType.DEFEND_DEFAULT;
    }
}
