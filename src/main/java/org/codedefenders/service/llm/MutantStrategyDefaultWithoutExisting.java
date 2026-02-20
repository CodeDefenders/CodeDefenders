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

import jakarta.enterprise.context.RequestScoped;

import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.model.llm.PromptType;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.LlmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Strategy(LlmStrategy.MUTANT_DEFAULT_WITHOUT_EXISTING)
public class MutantStrategyDefaultWithoutExisting extends LlmMutantService {
    Logger logger = LoggerFactory.getLogger(MutantStrategyDefaultWithoutExisting.class);

    private static final String systemPrompt = """
            Change the following java class in a way that is difficult to test against.
            Your change has to introduce changes to the behaviour, it must not be equivalent to the original code.

            There is a strict rule of not allowing any new control structures, such as if, while, ternary \
            operators, etc.
            Comments must remain as they are.

            Reply only with the modified code, nothing else.

            Never reply with natural language.
            """;

    @Override
    protected Optional<String> generate() {
        PromptType promptType = getCorrectAttackPromptType();
        setConversationType(promptType);
        resetConversationAfterTooManyTries();
        if (conversation.isEmpty()) {
            {
                //conversation.addSystemMessage(getSystemPrompt(model, promptType), model);TODO Eventually add back
                conversation.addSystemMessage(systemPrompt, model);
                String userMessage = getSourceCodeForUserMessage();
                conversation.addUserMessage(userMessage, model);
            }
        }

        String result = promptService.getResponse(model, conversation);
        return Optional.of(LlmUtils.extractMutantFromReply(result, true, game));
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
