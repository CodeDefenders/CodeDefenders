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

import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.model.Player;
import org.codedefenders.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class AbstractGameService implements IGameService {


    @Override
    public MutantDTO getMutant(int userId, int mutantId) {
        return getMutant(userId, MutantDAO.getMutantById(mutantId));
    }

    @Override
    public MutantDTO getMutant(int userId, Mutant mutant) {
        AbstractGame game = GameDAO.getGame(mutant.getGameId());
        Player player = PlayerDAO.getPlayerForUserAndGame(userId, mutant.getGameId());
        if (game != null) {
            return convertMutant(mutant, player.getUser(), player, game);
        } else {
            return null;
        }
    }

    @Override
    public MutantDTO getMutant(int userId, int mutantId, int gameId) {
        Mutant mutant = MutantDAO.getMutantById(mutantId);
        return getMutant(userId, mutant);
    }

    @Override
    public List<MutantDTO> getMutants(int userId, int gameId) {
        User user = UserDAO.getUserById(userId);
        AbstractGame game = GameDAO.getGame(gameId);
        if (game != null) {
            return getMutants(user, game);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public List<MutantDTO> getMutants(User user, AbstractGame game) {
        Player player = PlayerDAO.getPlayerForUserAndGame(user.getId(), game.getId());
        return game.getMutants().stream()
                .map(mutant -> convertMutant(mutant, user, player, game))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // NOTE: This could be split into several methods. Like: canFlag(Mutant mutant, Player player, AbstractGame game);
    //  So the actual building of the MutantDTO could happen in this class.
    abstract protected MutantDTO convertMutant(Mutant mutant, User user, Player player, AbstractGame game);

    // TODO:
    protected Role determineRole(User user, Player player, AbstractGame game) {
        Role result = null;
        if (game != null) {
            if (player != null) {
                result = player.getRole();
            }
            if (player == null || result == null) {
                if (game.getCreatorId() == user.getId()) {
                    result = Role.OBSERVER;
                } else {
                    result = Role.NONE;
                }
            }
        }
        return result;
    }
}
