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
package org.codedefenders.model.creategames;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.codedefenders.game.Role;
import org.codedefenders.model.creategames.gameassignment.GameAssignmentStrategy;
import org.codedefenders.model.creategames.roleassignment.RoleAssignmentStrategy;

import com.google.gson.annotations.Expose;

/**
 * Manages a list of staged games.
 * <br/>
 * <br/>
 * It provides the following consistency for staged games:
 * <ul>
 *     <li>Each staged game has a unique ID in the list.</li>
 *     <li>Each staged game's attacker and defender team are disjoint.</li>
 *     <li>Each user can only be assigned to one staged game in the list.</li>
 * </ul>
 */
public class StagedGameList implements Serializable {
    /**
     * Maps the ID of staged games to the corresponding {@link StagedGame StagedGames}.
     */
    private final Map<Integer, StagedGame> stagedGames = new HashMap<>();

    /**
     * The ID the next created staged game will receive.
     */
    private int currentId = 0;

    /**
     * Returns a mapping of staged games' IDs to the corresponding staged games.
     * @return A mapping of staged games' IDs to the corresponding staged games.
     */
    public Map<Integer, StagedGame> getMap() {
        return Collections.unmodifiableMap(stagedGames);
    }

    /**
     * Returns the staged game with the given ID.
     * @param gameId The ID of the staged game to get.
     * @return The staged game with the given ID, if it exists. {@code null} otherwise.
     */
    public StagedGame getGame(int gameId) {
        return stagedGames.get(gameId);
    }

    /**
     * Removes any users that are not in the given set from any staged games.
     * Useful to update a staged games list when the users (potentially) change.
     */
    public void retainUsers(Collection<Integer> userIds) {
        for (StagedGame stagedGame : stagedGames.values()) {
            stagedGame.retainUsers(userIds);
        }
    }

    /**
     * Adds and returns a new staged game with the given settings.
     * @param gameSettings The settings for the staged game.
     * @return The newly created staged game.
     */
    public StagedGame addStagedGame(GameSettings gameSettings) {
        int id = currentId++;
        StagedGame stagedGame = new StagedGame(id, gameSettings);
        stagedGames.put(id, stagedGame);
        return stagedGame;
    }

    /**
     * Removes a staged game from the list. Players must not be added to or removed from the staged game after removal.
     * @param gameId The ID of the staged game to remove.
     * @return {@code true} if the game was in the list, {@code false} otherwise.
     */
    public boolean removeStagedGame(int gameId) {
        if (stagedGames.remove(gameId) == null) {
            return false;
        }
        if (stagedGames.isEmpty()) {
            currentId = 0;
        }
        return true;
    }

    /**
     * Returns the IDs of all users currently assigned to any staged game.
     * @return The IDs of all users currently assigned to any staged game.
     */
    public Set<Integer> getAssignedUsers() {
        Set<Integer> assignedUsers = new HashSet<>();
        for (StagedGame stagedGame : stagedGames.values()) {
            assignedUsers.addAll(stagedGame.getPlayers());
        }
        return assignedUsers;
    }

    /**
     * Checks whether a user with the given ID is assigned to any staged game.
     * @return Whether a user with the given ID is assigned to any staged game.
     */
    public boolean isAssigned(int userId) {
        for (StagedGame stagedGame : stagedGames.values()) {
            if (stagedGame.getAttackers().contains(userId)
                    || stagedGame.getDefenders().contains(userId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Stages games according to the given role/game assignment strategies.
     *
     * <p>Users are assigned as follows:
     * <ol>
     *     <li>Users are assigned roles according to the role assignment strategy.</li>
     *     <li>
     *         The number of games to be staged is calculated from the number of users per role,
     *         and the number of requested attackers/defenders per game.
     *     </li>
     *     <li>Users are assigned to games according to the game assignment strategy.</li>
     * </ol>
     *
     *
     * @param userIds The users to become players in the games.
     * @param gameSettings The game settings.
     * @param roleAssignment The role assignment strategy.
     * @param gameAssignment The game assignment strategy.
     * @param attackersPerGame The desired number of attackers per game (or players per game for melee).
     * @param defendersPerGame The desired number of defenders per game.
     * @return The number of staged games.
     */
    public List<StagedGame> stageGamesWithUsers(Set<Integer> userIds, GameSettings gameSettings,
                                                RoleAssignmentStrategy roleAssignment, GameAssignmentStrategy gameAssignment,
                                                int attackersPerGame, int defendersPerGame) {
        /* Split users into attackers and defenders. */
        Set<Integer> attackers = new HashSet<>();
        Set<Integer> defenders = new HashSet<>();
        roleAssignment.assignRoles(userIds, attackersPerGame, defendersPerGame, attackers, defenders);

        int numGames1 = attackersPerGame > 0 ? attackers.size() / attackersPerGame : 0;
        int numGames2 = defendersPerGame > 0 ? defenders.size() / defendersPerGame : 0;
        int numGames = Math.max(numGames1, numGames2);

        /* Always create at least one game. */
        if (numGames == 0) {
            numGames = 1;
        }

        /* Assign attackers and defenders to teams. */
        List<List<Integer>> attackerTeams = gameAssignment.assignGames(attackers, numGames);
        List<List<Integer>> defenderTeams = gameAssignment.assignGames(defenders, numGames);

        /* Create the games. */
        List<StagedGame> newGames = new ArrayList<>();
        for (int i = 0; i < numGames; i++) {
            StagedGame stagedGame = addStagedGame(gameSettings);
            List<Integer> attackerTeam = attackerTeams.get(i);
            List<Integer> defenderTeam = defenderTeams.get(i);
            for (int userId : attackerTeam) {
                stagedGame.addAttacker(userId);
            }
            for (int userId : defenderTeam) {
                stagedGame.addDefender(userId);
            }
            newGames.add(stagedGame);
        }

        return newGames;
    }

    /**
     * Returns the formatted staged game ID corresponding to the given numeric staged game ID.
     * @param gameId The numeric staged game ID.
     * @return The formatted staged game ID.
     */
    public static String numericToFormattedGameId(int gameId) {
        return "T" + gameId;
    }

    /**
     * Returns the numeric staged game ID corresponding to the given formatted staged game ID.
     * @param formattedGameId The formatted staged game ID.
     * @return The numeric staged game ID.
     */
    public static Optional<Integer> formattedToNumericGameId(String formattedGameId) {
        if (!formattedGameId.startsWith("T")) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(formattedGameId.substring(1)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Represents a staged game on a create-games page. This class does not differentiate between multiplayer
     * games and melee games, always adding players as attackers or defenders.
     * For melee games, players should be added as attackers.
     */
    public class StagedGame implements Serializable {
        /**
         * The staged game's ID.
         */
        @Expose private final int id;

        /**
         * The game settings of the staged game.
         */
        @Expose private GameSettings gameSettings;

        /**
         * User IDs of users listed as attackers for the staged game.
         * <p>For melee games, players are assigned as attackers.
         */
        @Expose private final Set<Integer> attackers;

        /**
         * User IDs of users listed as defenders for the staged game.
         */
        @Expose private final Set<Integer> defenders;

        public StagedGame(int id, GameSettings gameSettings) {
            this.id = id;
            this.gameSettings = gameSettings;
            this.attackers = new HashSet<>();
            this.defenders = new HashSet<>();
        }

        /**
         * Returns the ID of the staged game.
         *
         * @return The ID of the staged game.
         */
        public int getId() {
            return id;
        }

        /**
         * Returns the ID of the staged game formatted as a string to differentiate it from IDs of normal games.
         *
         * @return The formatted ID of the staged game.
         */
        public String getFormattedId() {
            return numericToFormattedGameId(getId());
        }

        /**
         * Returns the game settings of the staged game.
         *
         * @return The game settings of the staged game.
         */
        public GameSettings getGameSettings() {
            return gameSettings;
        }

        public void setGameSettings(GameSettings gameSettings) {
            this.gameSettings = gameSettings;
        }

        /**
         * Returns the user IDs of users listed as attackers for the staged game.
         *
         * @return The user IDs of users listed as attackers for the staged game.
         */
        public Set<Integer> getAttackers() {
            return Collections.unmodifiableSet(attackers);
        }

        /**
         * Returns the user IDs of users listed as defenders for the staged game.
         *
         * @return The user IDs of users listed as defenders for the staged game.
         */
        public Set<Integer> getDefenders() {
            return Collections.unmodifiableSet(defenders);
        }

        /**
         * Returns the user IDs of users listed as players for the staged game.
         * This includes both attackers and defenders (and players for melee games).
         *
         * @return The user IDs of users listed as players for the staged game.
         */
        public Set<Integer> getPlayers() {
            Set<Integer> players = new HashSet<>();
            players.addAll(attackers);
            players.addAll(defenders);
            return Collections.unmodifiableSet(players);
        }

        /**
         * Assigns the given user ID to the game as an attacker.
         * <p>For melee games this is used to add a player as {@link Role#PLAYER}.
         *
         * @param userId The user ID to add.
         * @return {@code true} if the user was added successfully,
         * {@code false} if the user is already assigned to a staged game.
         */
        public boolean addAttacker(int userId) {
            if (isAssigned(userId)) {
                return false;
            }
            attackers.add(userId);
            return true;
        }

        /**
         * Assigns the given user ID to the game as a defender.
         *
         * @param userId The user ID to add.
         * @return {@code true} if the user was added successfully,
         * {@code false} if the user is already assigned to a staged game.
         */
        public boolean addDefender(int userId) {
            if (isAssigned(userId)) {
                return false;
            }
            defenders.add(userId);
            return true;
        }

        /**
         * Assigns the given user ID to the game as the given role.
         *
         * @param userId The user ID to add.
         * @param role The role to add the user as.
         * @return {@code true} if the user was added successfully,
         * {@code false} if the user is already assigned to a staged game.
         */
        public boolean addPlayer(int userId, Role role) {
            switch (role) {
                case PLAYER:
                case ATTACKER:
                    return addAttacker(userId);
                case DEFENDER:
                    return addDefender(userId);
                default:
                    return false;
            }
        }

        /**
         * Switches the role of a user.
         *
         * @param userId ID of the user to switch roles of.
         * @return {@code true} if the role was switched successfully
         * {@code false} if the user is not assigned to the game.
         */
        public boolean switchRole(int userId) {
            if (attackers.remove(userId)) {
                return defenders.add(userId);
            } else if (defenders.remove(userId)) {
                return attackers.add(userId);
            } else {
                return false;
            }
        }

        /**
         * Switches the creator's role in the game.
         * @return {@code true} if the creator role could be switched (i.e. a playing role),
         * {@code false} if the creator had a role that can't be switched.
         */
        public boolean switchCreatorRole() {
            Role role = gameSettings.getCreatorRole();
            switch (role) {
                case ATTACKER:
                    gameSettings = GameSettings.from(gameSettings)
                            .setCreatorRole(Role.DEFENDER)
                            .build();
                    return true;
                case DEFENDER:
                    gameSettings = GameSettings.from(gameSettings)
                            .setCreatorRole(Role.ATTACKER)
                            .build();
                    return true;
                case PLAYER:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Removes a user from the game's players.
         *
         * @param userId The ID of the user to remove.
         * @return {@code true} if the user was removed from the game,
         * {@code false} if the user wasn't assigned to begin with.
         */
        public boolean removePlayer(int userId) {
            return attackers.remove(userId)
                    || defenders.remove(userId);
        }

        /**
         * Removes any users that are not in the given set from the players.
         * Useful to update a staged game when the users (possibly) change.
         */
        public void retainUsers(Collection<Integer> userIds) {
            attackers.retainAll(userIds);
            defenders.retainAll(userIds);
        }
    }
}
