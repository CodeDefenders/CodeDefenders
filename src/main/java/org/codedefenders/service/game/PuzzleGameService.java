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
package org.codedefenders.service.game;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.game.puzzle.PuzzleType;
import org.codedefenders.model.Player;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.persistence.database.TestRepository;
import org.codedefenders.persistence.database.TestSmellRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.persistence.database.WhitelistRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.servlets.games.GameManagingUtils;

@ApplicationScoped
public class PuzzleGameService extends AbstractGameService {

    @Inject
    public PuzzleGameService(UserService userService, UserRepository userRepository, TestRepository testRepo,
                             MutantRepository mutantRepo, GameRepository gameRepo, PlayerRepository playerRepo,
                             TestSmellRepository testSmellRepo, GameManagingUtils gameManagingUtils,
                             WhitelistRepository whitelistRepo) {
        super(gameManagingUtils, userService, userRepository, testRepo, mutantRepo, gameRepo, playerRepo,
                testSmellRepo, whitelistRepo);
    }

    @Override
    protected boolean isMutantCovered(Mutant mutant, AbstractGame game, Player player) {
        return mutant.isCovered(game.getTests(true));
    }

    @Override
    protected boolean canViewMutant(Mutant mutant, AbstractGame game, SimpleUser user, Player player,
            Role playerRole) {
        PuzzleGame puzzleGame = (PuzzleGame) game;
        return mutant.getCreatorId() == user.getId()
                || game.isFinished()
                || game.getLevel() == GameLevel.EASY
                || puzzleGame.getPuzzle().getType() == PuzzleType.ATTACKER
                || puzzleGame.getPuzzle().getType() == PuzzleType.EQUIVALENCE
                || mutant.getState() == Mutant.State.KILLED;
    }

    @Override
    protected boolean canMarkMutantEquivalent(Mutant mutant, AbstractGame game, SimpleUser user,
            Role playerRole) {
        return false;
    }

    @Override
    protected boolean canViewTest(Test test, AbstractGame game, Player player, Role playerRole) {
        PuzzleGame puzzleGame = (PuzzleGame) game;
        return test.getPlayerId() == player.getId()
                || game.isFinished()
                || game.getLevel() == GameLevel.EASY
                || puzzleGame.getPuzzle().getType() == PuzzleType.DEFENDER;
    }
}
