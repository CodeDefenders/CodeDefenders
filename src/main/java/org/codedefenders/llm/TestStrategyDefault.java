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
package org.codedefenders.llm;

import java.util.Optional;

import org.codedefenders.model.llm.LlmPromptType;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.LlmUtils;

public class TestStrategyDefault extends AbstractTestStrategy {

    @Override
    public Optional<String> generate() {
        LlmPromptType promptType = getCorrectDefendPromptType();
        setConversationType(promptType.displayName());
        resetConversationAfterTooManyTries();
        if (!conversation.lastMessageWasError()) {
            {
                String systemMessage = context.strategy().getPrompt(promptType);
                if (promptType == LlmPromptType.TEST_TEMPLATE_DEFAULT_FOCUS_SYSTEM) {
                    Optional<String> methodName = getRandomMethodWithLivingMutant();
                    if (methodName.isPresent()) {
                        systemMessage = systemMessage.replace("${focused_method}", methodName.get());
                    }
                }
                conversation.addSystemMessage(systemMessage, context.model());

                conversation.addUserMessage(
                        getSourceCodeForUserMessage(false), context.model());//TODO dependencies
            }
            String reply = promptService.getResponse(context.model(), conversation);
            return Optional.of(LlmUtils.testTemplateFromReply(reply, context.game()));
        } else {
            String response = promptService.getResponse(context.model(), conversation);
            return Optional.of(LlmUtils.testTemplateFromReply(response, context.game()));
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

    private LlmPromptType getCorrectDefendPromptType() {
        if (hasLivingMutants()) {
            return LlmPromptType.TEST_TEMPLATE_DEFAULT_FOCUS_SYSTEM;
        }
        if (!context.game().getCUT().getDependencyNames().isEmpty()) {
            return LlmPromptType.TEST_DEFAULT_DEPENDENCY_SYSTEM;
        }
        return LlmPromptType.TEST_DEFAULT_DEFAULT_SYSTEM;
    }
}
