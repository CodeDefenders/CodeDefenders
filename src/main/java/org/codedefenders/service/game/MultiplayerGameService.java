/*
 * Copyright (C) 2020 Code Defenders contributors
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

package org.codedefenders.service.game;

import org.codedefenders.dto.MutantDTO;
import org.codedefenders.dto.TestDTO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.model.Player;
import org.codedefenders.model.User;
import org.codedefenders.util.Constants;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MultiplayerGameService extends AbstractGameService {

    @Override
    protected MutantDTO convertMutant(Mutant mutant, User user, Player player, AbstractGame game) {
        Role playerRole = determineRole(user, player, game);

        return new MutantDTO(mutant)
                .setCovered(mutant.isCovered())
                // TODO: This could use some tests
                .setViewable(playerRole != Role.NONE // User must participate in the Game
                        // Defender can see Mutants in easy Game
                        && (game.getLevel() == GameLevel.EASY
                        || (game.getLevel() == GameLevel.HARD
                        // In Hard Games the User must either be an Attacker or Observer
                        && (playerRole.equals(Role.ATTACKER) || playerRole.equals(Role.OBSERVER)
                        // Or the mutant must be killed or equivalent
                        || mutant.getState().equals(Mutant.State.KILLED)
                        || mutant.getState().equals(Mutant.State.EQUIVALENT)))))
                .setCanMarkEquivalent(game.getState().equals(GameState.ACTIVE)
                        && mutant.getState().equals(Mutant.State.ALIVE)
                        && mutant.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO)
                        && mutant.getCreatorId() != Constants.DUMMY_ATTACKER_USER_ID
                        && mutant.getCreatorId() != user.getId()
                        && mutant.getLines().size() >= 1);
    }

    @Override
    protected TestDTO convertTest(Test test, User user, Player player, AbstractGame game) {
        Role playerRole = determineRole(user, player, game);

        return new TestDTO(test)
                .setMutantData(game.getMutants())
                .setViewable(game.getState() == GameState.FINISHED
                        || playerRole == Role.OBSERVER
                        || playerRole == Role.DEFENDER
                        || test.getPlayerId() == player.getId());
    }
}
