/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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

import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.multiplayer.PlayerScore;

import java.util.List;
import java.util.Map;

/**
 * This class contains information about a {@link MultiplayerGame} from the view of a single {@link User}.
 *
 * Additionally to the game itself, this class contains the user's identifier, his role in the game and
 * game creator's name. {@link Type} also specifies whether the user is already active in this game
 * ({@link Type#ACTIVE}) or can join this game ({@link Type#OPEN}).
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
public class UserMultiplayerGameInfo {
    private Type type;

    private int userId;
    private MultiplayerGame game;
    private Role role;
    private String creatorName;

    /**
     * Use {@link #forOpen(int, MultiplayerGame, String) forActive()},
     * {@link #forActive(int, MultiplayerGame, Role, String) forOpen()} and
     * {@link #forFinished(int, MultiplayerGame, String) forFinished()}
     * methods instead
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

    public int userId() {
        return userId;
    }

    /**
     * @return the role of the user in the game. Can only be retrieved when the user is active in
     * this game, i.e. creator or player ({@link Type#ACTIVE}).
     */
    public Role userRole() {
        assert type != Type.OPEN;
        return role;
    }

    public String creatorName() {
        return creatorName;
    }

    public int gameId() {
        return game.getId();
    }

    public int creatorId() {
        return game.getCreatorId();
    }

    public GameState gameState() {
        return game.getState();
    }

    public GameLevel gameLevel() {
        return game.getLevel();
    }

    public List<Player> attackers() {
        return game.getAttackerPlayers();
    }

    public List<Player> defenders() {
        return game.getDefenderPlayers();
    }

    public int cutId() {
        return game.getCUT().getId();
    }

    public String cutAlias() {
        return game.getCUT().getAlias();
    }

    public Map<Integer, PlayerScore> getMutantScores() {
        return game.getMutantScores();
    }

    public Map<Integer, PlayerScore> getTestScores() {
        return game.getTestScores();
    }

    private enum Type {
        /**
         * The user participates in this game.
         */
        ACTIVE,
        /**
         * The user could join this game.
         */
        OPEN,
        /**
         * The user part of this game, but it is now finished.
         */
        FINISHED
    }
}
