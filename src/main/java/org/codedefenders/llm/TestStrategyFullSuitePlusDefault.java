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

import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.CDIUtil;

public class TestStrategyFullSuitePlusDefault extends AbstractTestStrategy {
    private TestStrategyFullSuite fullSuite;
    private TestStrategyDefault fallback;

    @Override
    protected void run(LlmStrategy strategy) {
        if (conversationBatch.getBaggage() == null) { //initial
            fullSuite().run(strategy);
        } else {
            if (conversationBatch.getBaggage() instanceof TestStrategyFullSuite.FullSuiteBaggage baggage) {
                if (!baggage.isEmpty()) {
                    fullSuite().run(strategy);
                } else {
                    fallback().run(strategy);
                }
            }
        }
    }

    @Override
    protected Optional<String> generate(LlmStrategy strategy) {
        throw new IllegalStateException("Must not be called");
    }

    private TestStrategyFullSuite fullSuite() {
        if (fullSuite == null) {
            fullSuite = CDIUtil.getBeanFromCDI(TestStrategyFullSuite.class);
            fullSuite.init(game, user, Optional.of(model), conversationBatch, random);
        }
        return fullSuite;
    }

    private TestStrategyDefault fallback() {
        if (fallback == null) {
            fallback = CDIUtil.getBeanFromCDI(TestStrategyDefault.class);
            fallback.init(game, user, Optional.of(model), conversationBatch, random);
        }
        return fallback;
    }


    @Override
    protected void onSubmitSuccess(LlmStrategy strategy) {
        throw new IllegalStateException("Must not be called");
    }

    @Override
    protected void onSubmitFailure(GameManagingUtils.CreateBattlegroundTestResult result, String testSrc, LlmStrategy strategy) {
        throw new IllegalStateException("Must not be called");
    }
}
