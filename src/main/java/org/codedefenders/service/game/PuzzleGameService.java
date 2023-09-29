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

import org.codedefenders.database.GameRepository;
import org.codedefenders.database.MutantRepository;
import org.codedefenders.database.PlayerRepository;
import org.codedefenders.database.TestRepository;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.model.Player;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;

@ApplicationScoped
public class PuzzleGameService extends AbstractGameService {

    @Inject
    public PuzzleGameService(UserService userService, UserRepository userRepository, TestRepository testRepo,
                             MutantRepository mutantRepo, GameRepository gameRepo, PlayerRepository playerRepo) {
        super(userService, userRepository, testRepo, mutantRepo, gameRepo, playerRepo);
    }

    @Override
    protected boolean isMutantCovered(Mutant mutant, AbstractGame game, Player player) {
        return mutant.isCovered(game.getTests(true));
    }

    // TODO: This doesn't use playerRole. Why not?! Doesn't {@link #determineRole} doesn't work for PuzzleGames?
    @Override
    protected boolean canViewMutant(Mutant mutant, AbstractGame game, SimpleUser user, Player player,
            Role playerRole) {
        if (player != null) {
            return player.getRole() != null && player.getRole() != Role.NONE;
        } else {
            return false;
        }
    }

    @Override
    protected boolean canMarkMutantEquivalent(Mutant mutant, AbstractGame game, SimpleUser user,
            Role playerRole) {
        return false;
    }

    @Override
    protected boolean canViewTest(Test test, AbstractGame game, Player player, Role playerRole) {
        return true;
    }
}
