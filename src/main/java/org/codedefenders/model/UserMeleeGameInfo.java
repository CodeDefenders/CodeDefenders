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
package org.codedefenders.model;

import java.util.List;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;

/**
 * This class contains information about a {@link MeleeGame} from the view of a single {@link UserEntity}.
 *
 * <p>Additionally to the game itself, this class contains the user's identifier, his role in the game and
 * game creator's name. {@link Type} also specifies whether the user is already active in this game
 * ({@link Type#ACTIVE}) or can join this game ({@link Type#OPEN}).
 *
 * @author gambi
 */
public class UserMeleeGameInfo extends GameInfo {

    private MeleeGame game;

    /**
     * Use {@link #forOpen(int, MeleeGame, String) forActive()} and
     * {@link #forActive(int, MeleeGame, Role, String) forOpen()} methods.
     */
    private UserMeleeGameInfo() {
    }

    public static UserMeleeGameInfo forActive(int userId, MeleeGame game, Role role, String creatorName) {
        UserMeleeGameInfo info = new UserMeleeGameInfo();
        info.type = Type.ACTIVE;
        info.userId = userId;
        info.game = game;
        info.creatorName = creatorName;
        info.role = role;
        assert info.role == Role.OBSERVER || info.role == Role.PLAYER;

        return info;
    }

    public static UserMeleeGameInfo forOpen(int userId, MeleeGame game, String creatorName) {
        UserMeleeGameInfo info = new UserMeleeGameInfo();
        info.type = Type.OPEN;
        info.userId = userId;
        info.game = game;
        info.creatorName = creatorName;

        return info;
    }

    public static UserMeleeGameInfo forFinished(int userId, MeleeGame game, String creatorName) {
        UserMeleeGameInfo info = new UserMeleeGameInfo();
        info.type = Type.FINISHED;
        info.userId = userId;
        info.game = game;
        info.creatorName = creatorName;

        return info;
    }

    public List<Player> players() {
        return game.getPlayers();
    }

    @Override
    protected AbstractGame getGame() {
        return game;
    }
}
