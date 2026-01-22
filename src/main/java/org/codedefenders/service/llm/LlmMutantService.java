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

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.codedefenders.dto.MutantDTO;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.model.llm.PromptType;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class LlmMutantService extends LlmSubActionService {
    private static final Logger logger = LoggerFactory.getLogger(LlmMutantService.class);

    protected abstract void onSubmitSuccess();

    protected abstract void onSubmitFailure(GameManagingUtils.CreateBattlegroundMutantResult result, String testSrc);

    @Override
    protected void submit(String mutantSrc) {
        try {
            GameManagingUtils.CreateBattlegroundMutantResult result;
            if (game instanceof MultiplayerGame multiplayerGame) {
                result = gameManagingUtils.createBattlegroundMutant(multiplayerGame,
                        Constants.AI_ATTACKER_USER_ID, mutantSrc);
            } else if (game instanceof MeleeGame meleeGame) {
                result = gameManagingUtils.createMeleeMutant(meleeGame, Constants.AI_PLAYER_USER_ID, mutantSrc);
            } else {
                throw new RuntimeException("No LLMs in Puzzles allowed!");
            }
            if (result.isSuccess()) {
                onSubmitSuccess();
            } else {
                conversation.addSystemMessage(
                        switch (result.failureReason().orElseThrow()) {
                            case VALIDATION_FAILED -> "Your mutant has violated the following rule: \n"
                                    + result.validationErrorMessage().orElseThrow();
                            case DUPLICATE_MUTANT_FOUND -> "Your mutant already exists. Create another one.";
                            case COMPILATION_FAILED -> "Your mutant failed to compile. Compilation error: "
                                    + result.compilationError().orElseThrow();
                        } + "\n Fix this.", model);
            }
        } catch (IOException | GameManagingUtils.MutantCreationException e) {
            throw new RuntimeException(e);
        }
    }


    protected PromptType getCorrectAttackPromptType() {
        if (model.isAttackerDependencies() && !game.getCUT().getDependencyNames().isEmpty()) {
            return PromptType.ATTACK_DEPENDENCIES;
        } else {
            return PromptType.ATTACK_DEFAULT;
        }
    }

    protected String getExistingMutantDiffsMessage() {
        return String.join("\n####\n",
                gameService.getMutants(user, game).stream().map(MutantDTO::getPatchString).collect(Collectors.toSet()));
    }

    static Class<? extends LlmMutantService> getService(LlmStrategy strategy) {
        List<Class<? extends LlmSubActionService>> l = List.of(
                MutantStrategyDefault.class,
                MutantStrategyAnnotatedFullClass.class,
                MutantStrategyAnnotatedSingleMethod.class,
                MutantStrategyRandomSingleMethod.class);
        return getServiceClass(l, strategy).asSubclass(LlmMutantService.class);
    }
}
