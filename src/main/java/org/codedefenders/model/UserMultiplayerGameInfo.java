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
import java.util.Map;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.multiplayer.PlayerScore;

/**
 * This class contains information about a {@link MultiplayerGame} from the view of a single {@link UserEntity}.
 *
 * <p>Additionally to the game itself, this class contains the user's identifier, his role in the game and
 * game creator's name. {@link Type} also specifies whether the user is already active in this game
 * ({@link Type#ACTIVE}) or can join this game ({@link Type#OPEN}).
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
public class UserMultiplayerGameInfo extends GameInfo {

    public MultiplayerGame game;

    /**
     * Use {@link #forOpen(int, MultiplayerGame, String) forActive()},
     * {@link #forActive(int, MultiplayerGame, Role, String) forOpen()} and
     * {@link #forFinished(int, MultiplayerGame, String) forFinished()}
     * methods instead.
     */
    private UserMultiplayerGameInfo() {
    }

    public static UserMultiplayerGameInfo forActive(int userId, MultiplayerGame game, Role role, String creatorName) {
        UserMultiplayerGameInfo info = new UserMultiplayerGameInfo();
        info.type = Type.ACTIVE;
        info.userId = userId;
        info.game = game;
        info.role = role;
        info.creatorName = creatorName;

        return info;
    }

    public static UserMultiplayerGameInfo forOpen(int userId, MultiplayerGame game, String creatorName) {
        UserMultiplayerGameInfo info = new UserMultiplayerGameInfo();
        info.type = Type.OPEN;
        info.userId = userId;
        info.game = game;
        info.creatorName = creatorName;

        return info;
    }

    public static UserMultiplayerGameInfo forFinished(int userId, MultiplayerGame game, String creatorName) {
        UserMultiplayerGameInfo info = new UserMultiplayerGameInfo();
        info.type = Type.FINISHED;
        info.userId = userId;
        info.game = game;
        info.creatorName = creatorName;

        return info;
    }

    public List<Player> attackers() {
        return game.getAttackerPlayers();
    }

    public List<Player> defenders() {
        return game.getDefenderPlayers();
    }

    public Map<Integer, PlayerScore> getMutantScores() {
        return game.getMutantScores();
    }

    public Map<Integer, PlayerScore> getTestScores() {
        return game.getTestScores();
    }

    @Override
    protected AbstractGame getGame() {
        return game;
    }
}
