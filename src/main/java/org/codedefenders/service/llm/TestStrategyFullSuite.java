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

import org.apache.commons.lang3.StringUtils;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.llm.LlmPromptType;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.LlmUtils;

public class TestStrategyFullSuite extends LlmTestService {

    private static final String GENERATE_FULL_SUITE = "GENERATE_FULL_SUITE";
    private static final String CORRECTION = "CORRECTION";
    private static final String ONE_FROM_MANY = "ONE_FROM_MANY";

    private FullSuiteBaggage baggage() {
        if (conversationBatch.getBaggage() == null
                || !(conversationBatch.getBaggage() instanceof FullSuiteBaggage)) {
            conversationBatch.setBaggage(new FullSuiteBaggage());
        }
        return (FullSuiteBaggage) conversationBatch.getBaggage();
    }

    public String getOneTest() {
        return baggage().tests.remove(0);
    }

    public boolean isEmpty() {
        return baggage().tests.isEmpty();
    }

    private void addTest(String test) {
        if (test == null) {
            throw new NullPointerException("Test may not be null");
        }
        baggage().tests.add(test);
    }

    private String getCorrectionUserMessage(AbstractGame game,
                                            String testCode,
                                            GameManagingUtils.CreateBattlegroundTestResult result,
                                            LlmStrategy strategy) {

        String userTemplate = strategy.getPrompt(LlmPromptType.TEST_TEMPLATE_FULL_SUITE_USER);

        String issueString = switch (result.failureReason().orElseThrow()) {
            case TEST_DID_NOT_PASS_ON_CUT -> strategy.getPrompt(LlmPromptType.TEST_PARAMETER_TEMPLATE_DID_NOT_PASS)
                    .replace("${failure_reason}", result.testCutError().orElseThrow());
            case COMPILATION_FAILED -> strategy.getPrompt(LlmPromptType.TEST_PARAMETER_TEMPLATE_COMPILATION_FAILED)
                    .replace("${failure_reason}", result.compilationError().orElseThrow());
            case VALIDATION_FAILED -> strategy.getPrompt(LlmPromptType.TEST_PARAMETER_TEMPLATE_RULE_VIOLATION)
                    .replace("{failure_reason}",
                            String.join("\n", result.validationErrorMessages().orElseThrow()));
        };

        return StringUtils.replaceEach(userTemplate,
                new String[]{"${cut_source}", "${test_code}", "${issue}"},
                new String[]{game.getCUT().getSourceCode(), testCode, issueString});
    }

    @Override
    public Optional<String> generate(LlmStrategy strategy) {
        if (isEmpty()) {
            setConversationType(GENERATE_FULL_SUITE);
        } else {
            setConversationType(CORRECTION);
        }
        if (conversation.numberOfTries() > numberOfRepairAttempts) {
            finishConversation(false);
            conversation = conversationBatch.getConversation(GENERATE_FULL_SUITE); //TODO Dependencies
        }
        if (conversation.getType().equals(GENERATE_FULL_SUITE)) {
            conversation.addSystemMessage(strategy.getPrompt(LlmPromptType.TEST_FULL_SUITE_SYSTEM), model);
            conversation.addUserMessage(getSourceCodeForUserMessage(false), model); //TODO customize??
            String reply = promptService.getResponse(model, conversation);
            LlmUtils.suiteOfTestTemplatesFromReply(reply, game).forEach(this::addTest);
            if (isEmpty()) {
                //Not a single valid test was generated
                finishConversation(false);
                return Optional.empty();
            } else {
                finishConversation(true);
                setConversationType(ONE_FROM_MANY);
            }
        }
        if (!conversation.isEmpty()) {
            //There was an error on the last submission
            String reply = promptService.getResponse(model, conversation);
            return Optional.of(LlmUtils.testTemplateFromReply(reply, game));
        } else {
            finishConversation(true);
            setConversationType(ONE_FROM_MANY);
            return Optional.of(getOneTest());
        }
    }

    @Override
    protected void onSubmitSuccess(LlmStrategy strategy) {
        if (isEmpty() || !conversation.isEmpty()) {
            finishConversation(true);
        }
    }

    @Override
    protected void onSubmitFailure(GameManagingUtils.CreateBattlegroundTestResult result, String testSrc,
                                   LlmStrategy strategy) {
        if (!conversation.hasSystemMessage()) {
            conversation.addSystemMessage(strategy.getPrompt(LlmPromptType.TEST_FULL_SUITE_CORRECTION_SYSTEM), model);
        }
        String testContent = LlmUtils.extractTestContentFromReply(testSrc);
        String userMessage = getCorrectionUserMessage(game, testContent, result, strategy);
        conversation.addUserMessage(userMessage, model);
    }

    static class FullSuiteBaggage {
        private final List<String> tests = new ArrayList<>();

        boolean isEmpty() {
            return tests.isEmpty();
        }
    }
}
