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

import org.apache.commons.lang3.StringUtils;
import org.codedefenders.model.llm.LlmPromptType;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.LlmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MutantStrategyDefault extends AbstractMutantStrategy {
    Logger logger = LoggerFactory.getLogger(MutantStrategyDefault.class);

    @Override
    protected Optional<String> generate() {
        //TODO Dependencies with game.getCUT().getDependencyNames().isEmpty()
        LlmPromptType promptType = LlmPromptType.MUTANT_DEFAULT_DEFAULT_SYSTEM;
        setConversationType(promptType.displayName());
        resetConversationAfterTooManyTries();
        if (conversation.isEmpty()) {
            conversation.addSystemMessage(context.strategy().getPrompt(LlmPromptType.MUTANT_DEFAULT_DEFAULT_SYSTEM),
                    context.model());
            String userMessage;
            if (context.random().nextBoolean()) {
                String userTemplate = context.strategy().getPrompt(LlmPromptType.MUTANT_TEMPLATE_DEFAULT_DIFFS_USER);
                userMessage = StringUtils.replaceEach(userTemplate,
                        new String[]{"${cut_source}", "${mutant_diffs}"},
                        new String[]{getSourceCodeForUserMessage(false), getExistingMutantDiffsMessage()});
            } else {
                userMessage = getSourceCodeForUserMessage(false);
            }
            conversation.addUserMessage(userMessage, context.model());

        }

        String result = promptService.getResponse(context.model(), conversation);
        return Optional.of(LlmUtils.extractMutantFromReply(result, true, context.game()));
    }

    @Override
    protected void onSubmitSuccess() {
        finishConversation(true);
        logger.info("LLM successfully submitted mutant.");
    }

    @Override
    protected void onSubmitFailure(GameManagingUtils.CreateBattlegroundMutantResult result, String testSrc) {

    }
}
