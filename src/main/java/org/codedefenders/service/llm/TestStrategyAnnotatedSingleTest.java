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

public class TestStrategyAnnotatedSingleTest extends LlmTestService {
    static LlmStrategy strategy = LlmStrategy.TEST_ANNOTATED_SINGLE_TEST;
    public static final String systemPrompt = """
            You are a capable java developer playing a game. You want to win by getting as many points as possible.

            Your task is to write a single unit test for a specific java class. This unit test should be able to detect
            changes to the code. These changes are called 'mutants'.
            These mutants are difficult to find, so you have to be crafty.

            If your test fails on a piece of mutated code, but succeeds on the original code, that mutant is killed.
            You get points for every mutant your tests kill. If many other tests have already covered this mutant
            without having killed it, detecting the mutant gets you more points.
            You get no points by detecting a mutant that has already been killed.

            You will see a java class with specific annotations. Every line has a comment in the format
            `//coverage: c, killed: k, alive: a`
            Instead of c, k or a there will be a number.
            c refers to the number of tests that already cover this line.
            k refers to the mutants that have already been killed here.
            a refers to the mutants that are currently alive.

            The test must target this class.

            There is a strict rule of using at most 2 assertions per test. Always abide by it.

            Write nothing but the code of the single test.

            Use JUnit 4.

            Never reply in natural language.
            """;

    @Override
    protected void onSubmitSuccess() {
        finishConversation(true);
    }

    @Override
    protected void onSubmitFailure(GameManagingUtils.CreateBattlegroundTestResult result, String testSrc) {
        standardSubmitFailure(result, testSrc);
    }

    @Override
    protected Optional<String> generate() {
        setConversationType(PromptType.DEFEND_DEFAULT);
        resetConversationAfterTooManyTries();
        if (!conversation.lastMessageWasError()) {
            conversation.addSystemMessage(systemPrompt, model);
            conversation.addUserMessage(LlmUtils.annotatedCut(game), model);
        }
        String reply = promptService.getResponse(model, conversation);
        return Optional.of(LlmUtils.testTemplateFromReply(reply, game));
    }
}
