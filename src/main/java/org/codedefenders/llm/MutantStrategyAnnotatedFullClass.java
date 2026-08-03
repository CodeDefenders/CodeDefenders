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
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.LlmUtils;


public class MutantStrategyAnnotatedFullClass extends AbstractMutantStrategy {

    @Override
    protected void onSubmitSuccess() {
        finishConversation(true);
    }

    @Override
    protected void onSubmitFailure(GameManagingUtils.CreateBattlegroundMutantResult result, String testSrc) {

    }

    @Override
    protected Optional<String> generate() {
        setConversationType(LlmPromptType.MUTANT_ANNOTATED_FULL_CLASS_DEFAULT_SYSTEM.displayName());
        resetConversationAfterTooManyTries();
        if (conversation.isEmpty()) {
            conversation.addSystemMessage(
                    context.strategy().getPrompt(LlmPromptType.MUTANT_ANNOTATED_FULL_CLASS_DEFAULT_SYSTEM),
                    context.model()
            );
            conversation.addUserMessage(LlmUtils.annotatedCut(context.game()), context.model());
        }
        String result = promptService.getResponse(context.model(), conversation);
        return Optional.of(LlmUtils.extractMutantFromReply(result, true, context.game()));
    }
}
