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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.database.GameDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.execution.KillMap;
import org.codedefenders.execution.KillMapProcessor;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.model.Player;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.util.Constants;

@ApplicationScoped
public class MultiplayerGameService extends AbstractGameService {

    @Inject
    public MultiplayerGameService(UserService userService, UserRepository userRepository) {
        super(userService, userRepository);
    }

    @Override
    protected boolean isMutantCovered(Mutant mutant, AbstractGame game, Player player) {
        return mutant.isCovered(game.getTests(true));
    }

    // TODO: This could use some tests
    @Override
    protected boolean canViewMutant(Mutant mutant, AbstractGame game, SimpleUser user, Player player,
            Role playerRole) {
        return playerRole != Role.NONE // User must participate in the Game
                // Defender can see Mutants if the game is over or its an easy Game
                && (game.isFinished() || game.getLevel() == GameLevel.EASY
                || (game.getLevel() == GameLevel.HARD
                // In Hard Games the User must either be an Attacker or Observer
                && (playerRole.equals(Role.ATTACKER) || playerRole.equals(Role.OBSERVER)
                // Or the mutant must be killed or equivalent
                || mutant.getState().equals(Mutant.State.KILLED)
                || mutant.getState().equals(Mutant.State.EQUIVALENT))));
    }

    @Override
    protected boolean canMarkMutantEquivalent(Mutant mutant, AbstractGame game, SimpleUser user,
            Role playerRole) {
        return game.getState().equals(GameState.ACTIVE)
                && mutant.getState().equals(Mutant.State.ALIVE)
                && mutant.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO)
                && playerRole.equals(Role.DEFENDER)
                && mutant.getCreatorId() != Constants.DUMMY_ATTACKER_USER_ID
                && mutant.getCreatorId() != user.getId()
                && mutant.getLines().size() >= 1;
    }

    @Override
    protected boolean canViewTest(Test test, AbstractGame game, Player player, Role playerRole) {
        return game.isFinished()
                || playerRole == Role.OBSERVER
                || playerRole == Role.DEFENDER
                || game.getLevel() == GameLevel.EASY
                || test.getPlayerId() == player.getId();
    }

    /**
     * Closes the given game and enqueues the kill-map computation for it.
     *
     * @param game The game to close.
     * @return {@code true} if the game was closed, {@code false} otherwise.
     */
    @Override
    public boolean closeGame(AbstractGame game) {
        boolean closed = super.closeGame(game);
        if (closed) {
            KillmapDAO.enqueueJob(new KillMapProcessor.KillMapJob(KillMap.KillMapType.GAME, game.getId()));
        }
        return closed;
    }
}
