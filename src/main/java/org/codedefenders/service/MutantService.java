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

package org.codedefenders.service;

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.model.Player;
import org.codedefenders.model.User;
import org.codedefenders.util.Constants;

import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
public class MutantService {

    // TODO: Implement
    public MutantDTO getMutant(int mutantId) {
        // Get gameId for mutantId
        // int gameId = MutantDAO.getGameId(mutantId);

        // Get mutant for game;
        // MutantDTO = GameDAO.getGame
        return null;
    }

    public List<MutantDTO> getMutantsForGame(int userId, int gameId) {
        User user = UserDAO.getUserById(userId);
        AbstractGame game = GameDAO.getGame(gameId);
        return getMutantsForGame(user, game);
    }

    public List<MutantDTO> getMutantsForGame(User user, AbstractGame game) {
        Player player = PlayerDAO.getPlayerForUserAndGame(user.getId(), game.getId());
        // I need to use this method because it returns Observer if the User is the creator of the game.
        Role playerRole = DatabaseAccess.getRole(user.getId(), game.getId());
        List<Player> players = GameDAO.getAllPlayersForGame(game.getId());
        assert game != null;
        GameMode mode = game.getMode();

        return game.getMutants().stream()
                .map(mutant -> {
                    switch (mode) {
                        case PARTY:
                            return new MutantDTO(mutant)
                                    .setCovered(mutant.isCovered())
                                    // TODO: This could use some tests
                                    .setCanView(playerRole != Role.NONE // User must participate in the Game
                                            // Defender can see Mutants in easy Game
                                            && (game.getLevel() == GameLevel.EASY
                                            || (game.getLevel() == GameLevel.HARD
                                            // In Hard Games the User must either be an Attacker or Observer
                                            && (playerRole.equals(Role.ATTACKER) || playerRole.equals(Role.OBSERVER)))))
                                    .setCanMarkEquivalent(game.getState().equals(GameState.ACTIVE)
                                            && mutant.getState().equals(Mutant.State.ALIVE)
                                            && mutant.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO)
                                            && mutant.getCreatorId() != Constants.DUMMY_ATTACKER_USER_ID
                                            && mutant.getCreatorId() != user.getId()
                                            && mutant.getLines().size() >= 1);
                        case MELEE:
                            return new MutantDTO(mutant)
                                    .setCovered(mutant.getCoveringTests().stream()
                                            .anyMatch(t -> UserDAO.getUserForPlayer(t.getPlayerId()).getId() == user.getId()))
                                    .setCanView(playerRole != Role.NONE
                                            && (game.getLevel() == GameLevel.EASY
                                            || (game.getLevel() == GameLevel.HARD
                                            && (mutant.getCreatorId() == user.getId()
                                            || playerRole.equals(Role.OBSERVER)))))
                                    .setCanMarkEquivalent(game.getState().equals(GameState.ACTIVE)
                                            && mutant.getState().equals(Mutant.State.ALIVE)
                                            && mutant.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO)
                                            && mutant.getCreatorId() != Constants.DUMMY_ATTACKER_USER_ID
                                            && mutant.getCreatorId() != user.getId()
                                            && mutant.getLines().size() >= 1);
                        case PUZZLE:
                            return new MutantDTO(mutant)
                                    .setCovered(mutant.isCovered())
                                    .setCanView(playerRole != Role.NONE);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
