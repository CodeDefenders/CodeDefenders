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

import java.util.Arrays;
import java.util.List;

import org.codedefenders.dto.MutantDTO;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmConversation;
import org.codedefenders.model.llm.LlmDefaultStrategies;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.model.llm.LlmType;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.LlmConversationRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.service.LlmPromptService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.servlets.games.GameManagingUtils.CreateBattlegroundMutantResult;
import org.codedefenders.servlets.games.GameManagingUtils.CreateBattlegroundTestResult;
import org.codedefenders.servlets.games.GameManagingUtils.RejectBattlegroundEquivalenceResult;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.Constants;
import org.junit.jupiter.api.Test;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class LlmCorrectionTest {
    @Test
    void rejectedMutantProducesAUserTurnWithTheValidationError() throws Exception {
        var strategy = new MutantStrategyAnnotatedFullClass();
        var conversation = prepare(strategy, LlmDefaultStrategies.MUTANT_ANNOTATED_FULL_CLASS);
        var game = (MultiplayerGame) strategy.context.game();
        String code = "class Sample { int value = 2; }";
        String validationError = "Mutation changes the public API";
        when(strategy.gameManagingUtils.createBattlegroundMutant(game, Constants.AI_ATTACKER_USER_ID, code))
                .thenReturn(CreateBattlegroundMutantResult.failure(
                        CreateBattlegroundMutantResult.FailureReason.VALIDATION_FAILED, validationError, null));

        strategy.submit(code);

        assertRetryIncludes(conversation, validationError);
    }

    @Test
    void rejectedTestProducesAUserTurnWithTheFailureDetails() throws Exception {
        var strategy = new TestStrategyDefault();
        var conversation = prepare(strategy, LlmDefaultStrategies.TEST_DEFAULT);
        String code = "assertEquals(2, new Sample().value);";
        String testFailure = "Expected 2 but got 1";
        var result = CreateBattlegroundTestResult.failure(null,
                CreateBattlegroundTestResult.FailureReason.TEST_DID_NOT_PASS_ON_CUT, null, null, testFailure);
        when(strategy.gameManagingUtils.createBattlegroundTest(
                strategy.context.game(), Constants.AI_DEFENDER_USER_ID, code)).thenReturn(result);

        strategy.submit(code);

        assertRetryIncludes(conversation, testFailure);
    }

    @Test
    void rejectedEquivalenceTestProducesAUserTurnWithTheFailureDetails() throws Exception {
        var strategy = new EquivalenceStrategy();
        var model = new LlModel("test-model", LlmType.OPENAI);
        var game = mock(MultiplayerGame.class);
        var context = new LlmContext(model, LlmStrategy.of(LlmDefaultStrategies.EQUIVALENCE_DEFAULT), game);
        strategy.context = context;
        strategy.gameService = mock(org.codedefenders.service.game.GameService.class);
        strategy.promptService = mock(LlmPromptService.class);
        strategy.conversationRepository = mock(LlmConversationRepository.class);
        strategy.gameManagingUtils = mock(GameManagingUtils.class);
        strategy.mutantRepository = mock(MutantRepository.class);
        when(game.getId()).thenReturn(103);
        GameClass cut = mock(GameClass.class);
        when(game.getCUT()).thenReturn(cut);
        when(cut.getSourceCode()).thenReturn("class Sample { int value = 1; }");
        when(cut.getTestTemplate()).thenReturn("class SampleTest {\n// test here!\n}");
        var flagged = mock(MutantDTO.class);
        var mutant = mock(Mutant.class);
        when(flagged.getId()).thenReturn(193);
        when(flagged.getPatchString()).thenReturn("- int value = 1;\n+ int value = 2;");
        when(strategy.mutantRepository.getMutantById(193)).thenReturn(mutant);
        when(strategy.gameService.getFlaggedMutants(context.user(), game)).thenReturn(List.of(flagged));
        String testFailure = "Expected 2 but got 1";
        var rejected = RejectBattlegroundEquivalenceResult.testInvalid(null,
                RejectBattlegroundEquivalenceResult.FailureReason.TEST_DID_NOT_PASS_ON_CUT,
                null, null, testFailure);
        var accepted = RejectBattlegroundEquivalenceResult.testValid(
                mock(org.codedefenders.game.Test.class), false, 0, false);
        when(strategy.gameManagingUtils.rejectBattlegroundEquivalence(
                eq(context.game()), eq(context.user().getId()), eq(mutant), anyString()))
                .thenReturn(rejected, accepted);
        when(strategy.promptService.getResponse(model, context.conversationBatch().getConversation("EQUIVALENCE")))
                .thenAnswer(invocation -> {
                    var conversation = invocation.getArgument(1, LlmConversation.class);
                    String response = "class GeneratedTest {\n@Test\nvoid generated() {\nassertEquals(1, 1);\n}\n}";
                    conversation.addAiMessage(AiMessage.from(response),
                            model, 0, 0);
                    return response;
                });
        var gameRepository = mock(GameRepository.class);
        when(gameRepository.getGame(103)).thenReturn(game);

        LlmConversation conversation = context.conversationBatch().getConversation("EQUIVALENCE");
        try (var ignored = mockStatic(CDIUtil.class)) {
            ignored.when(() -> CDIUtil.getBeanFromCDI(GameRepository.class)).thenReturn(gameRepository);
            strategy.run(context);
        }

        assertRetryIncludes(conversation, testFailure);
    }

    private LlmConversation prepare(AbstractStrategy strategy, LlmDefaultStrategies type) {
        return prepare(strategy, type, "test");
    }

    private LlmConversation prepare(AbstractStrategy strategy, LlmDefaultStrategies type, String conversationType) {
        var model = new LlModel("test-model", LlmType.OPENAI);
        strategy.context = new LlmContext(model, LlmStrategy.of(type), mock(MultiplayerGame.class));
        GameClass cut = mock(GameClass.class);
        when(strategy.context.game().getCUT()).thenReturn(cut);
        when(cut.getSourceCode()).thenReturn("class Sample { int value = 1; }");
        strategy.gameManagingUtils = mock(GameManagingUtils.class);
        var conversation = strategy.context.conversationBatch().getConversation(conversationType);
        strategy.conversation = conversation;
        conversation.addSystemMessage("Generate valid Java code", model);
        conversation.addUserMessage("Source code", model);
        conversation.add(AiMessage.from("invalid"), model);
        return conversation;
    }

    private void assertRetryIncludes(LlmConversation conversation, String failureDetails) {
        var messages = conversation.toArray();
        assertTrue(Arrays.stream(messages)
                .filter(UserMessage.class::isInstance)
                .map(UserMessage.class::cast)
                .anyMatch(message -> message.singleText().contains(failureDetails)));
        assertTrue(Arrays.stream(messages).skip(1).noneMatch(SystemMessage.class::isInstance),
                "The retry must not contain a system message after the opening turn");
    }
}
