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

import java.io.IOException;
import java.util.stream.Collectors;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractMutantStrategy extends AbstractStrategy {
    private static final Logger logger = LoggerFactory.getLogger(AbstractMutantStrategy.class);

    protected abstract void onSubmitSuccess();

    protected abstract void onSubmitFailure(GameManagingUtils.CreateBattlegroundMutantResult result, String testSrc);

    @Override
    protected void submit(String mutantSrc, LlmStrategy strategy) {
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
                conversation.setMutantId(result.mutant().orElseThrow().getId());
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
        } catch (UncheckedSQLException e) {
            if (e.isDataTooLong()) {
                conversation.addSystemMessage(
                        "Your mutant changed to many lines. Stick closer to the original code", model);
            }
        } catch (IOException | GameManagingUtils.MutantCreationException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getExistingMutantDiffsMessage() {
        return String.join("\n####\n",
                gameService.getMutants(user, game).stream().map(MutantDTO::getPatchString).collect(Collectors.toSet()));
    }
}
