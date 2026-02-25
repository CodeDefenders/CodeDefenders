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

import org.codedefenders.model.llm.LlmPromptType;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.LlmUtils;

public class TestStrategyAnnotatedSingleTest extends LlmTestService {

    @Override
    protected void onSubmitSuccess(LlmStrategy strategy) {
        finishConversation(true);
    }

    @Override
    protected void onSubmitFailure(GameManagingUtils.CreateBattlegroundTestResult result,
                                   String testSrc,
                                   LlmStrategy strategy) {
        standardSubmitFailure(result, testSrc);
    }

    @Override
    protected Optional<String> generate(LlmStrategy strategy) {
        setConversationType(LlmPromptType.TEST_ANNOTATED_DEFAULT_SYSTEM.displayName());
        resetConversationAfterTooManyTries();
        if (!conversation.lastMessageWasError()) {
            conversation.addSystemMessage(strategy.getPrompt(LlmPromptType.TEST_ANNOTATED_DEFAULT_SYSTEM), model);
            conversation.addUserMessage(LlmUtils.annotatedCut(game), model);
        }
        String reply = promptService.getResponse(model, conversation);
        return Optional.of(LlmUtils.testTemplateFromReply(reply, game));
    }
}
