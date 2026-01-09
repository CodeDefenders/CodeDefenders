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
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.LlmUtils;


public class MutantStrategyAnnotatedFullClass extends LlmMutantService {

    static LlmStrategy strategy = LlmStrategy.MUTANT_ANNOTATED_FULL_CLASS;

    public static final String initialPrompt = """
            You are a capable java developer playing a game. You want to win by getting as many points as possible.
            You get points by writing bugs in source code that are difficult to detect by unit tests.
            Every unit test that covers your bug without failing gets you a point.
            Once your bug is detected, it will stop gathering points.

            You will see a java class with specific annotations. Every line has a comment in the format
            `//coverage: c, killed: k, alive: a`
            Instead of c, k or a there will be a number.
            c refers to the number of tests that already cover this line.
            k refers to the mutants that have already been killed here.
            a refers to the mutants that are currently alive.

            Write a mutated version of this class to get points.
            It is crucial that you do not include these comments with the specific annotations. Your submission
            will be rejected if any comments of this format are included.

            Write nothing but the mutated test class.
            Never use natural language.
            """;

    @Override
    protected void onSubmitSuccess() {

    }

    @Override
    protected void onSubmitFailure(GameManagingUtils.CreateBattlegroundMutantResult result, String testSrc) {

    }

    @Override
    protected Optional<String> generate() {
        if (conversation.isEmpty()) {
            conversation.addSystemMessage(MutantStrategyAnnotatedFullClass.initialPrompt, model);
            conversation.addUserMessage(LlmUtils.annotatedCut(game), model);
        }
        String result = promptService.getResponse(model, conversation);
        return Optional.of(LlmUtils.extractMutantFromReply(result, true, game));
    }
}
