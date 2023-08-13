package org.codedefenders.model.creategames;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.codedefenders.game.Role;
import org.codedefenders.model.creategames.roleassignment.RoleAssignment;
import org.codedefenders.model.creategames.teamassignment.GameAssignment;

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
    public Map<Integer, StagedGame> getStagedGames() {
        return Collections.unmodifiableMap(stagedGames);
    }

    /**
     * Returns the staged game with the given ID.
     * @param gameId The ID of the staged game to get.
     * @return The staged game with the given ID, if it exists. {@code null} otherwise.
     */
    public StagedGame getStagedGame(int gameId) {
        return stagedGames.get(gameId);
    }

    /**
     * Removes any users that are not in the given set from any staged games.
     * Useful to update a staged games list when the users change.
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

    public int stageGamesWithUsers(Set<Integer> userIds, GameSettings gameSettings,
                                   RoleAssignment roleAssignment, GameAssignment gameAssignment,
                                   int attackersPerGame, int defendersPerGame) {
        /* Split users into attackers and defenders. */
        Set<Integer> attackers = new HashSet<>();
        Set<Integer> defenders = new HashSet<>();
        roleAssignment.assignRoles(userIds, attackersPerGame, defendersPerGame, attackers, defenders);

        int numGames = userIds.size() / (attackersPerGame + defendersPerGame);

        /* Avoid empty games. */
        if (numGames > attackers.size() && numGames > defenders.size())  {
            int numGames1 = attackersPerGame > 0 ? attackers.size() / attackersPerGame : 0;
            int numGames2 = defendersPerGame > 0 ? defenders.size() / defendersPerGame : 0;
            numGames = Math.max(numGames1, numGames2);
        }

        /* Always create at least one game. */
        if (numGames == 0) {
            numGames = 1;
        }

        /* Assign attackers and defenders to teams. */
        List<List<Integer>> attackerTeams = gameAssignment.assignGames(attackers, numGames);
        List<List<Integer>> defenderTeams = gameAssignment.assignGames(defenders, numGames);

        /* Create the games. */
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
        }

        return numGames;
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
     * Represents a staged game on the admin create-games page. This class does not differentiate between multiplayer
     * games and melee games, always adding players as attackers or defenders. It is up to the servlet and JSP page to
     * differentiate between staged multiplayer and melee games according to the game's settings.
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
         * This includes both attackers and defenders.
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

        public boolean switchRole(int userId) {
            if (attackers.remove(userId)) {
                return defenders.add(userId);
            } else if (defenders.remove(userId)) {
                return attackers.add(userId);
            } else {
                return false;
            }
        }

        public boolean switchCreatorRole() {
            Role role = gameSettings.getCreatorRole();
            switch (role) {
                case ATTACKER:
                    gameSettings = GameSettings.builder()
                            .withSettings(gameSettings)
                            .setCreatorRole(Role.DEFENDER)
                            .build();
                    return true;
                case DEFENDER:
                    gameSettings = GameSettings.builder()
                            .withSettings(gameSettings)
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
         * Removes a user ID from the game's players.
         *
         * @param userId The user ID to remove.
         * @return {@code true} if the user was assigned to the game, {@code false} if the user wasn't.
         */
        public boolean removePlayer(int userId) {
            return attackers.remove(userId)
                    || defenders.remove(userId);
        }

        /**
         * Removes any users that are not in the given set from the players.
         * Useful to update a staged game when the users change.
         */
        public void retainUsers(Collection<Integer> userIds) {
            attackers.retainAll(userIds);
            defenders.retainAll(userIds);
        }
    }
}
