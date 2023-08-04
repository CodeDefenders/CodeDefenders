package org.codedefenders.beans.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameType;
import org.codedefenders.game.Role;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.validation.code.CodeValidatorLevel;

import com.google.gson.annotations.Expose;

import static org.codedefenders.game.GameLevel.HARD;
import static org.codedefenders.game.GameType.MULTIPLAYER;
import static org.codedefenders.game.Role.OBSERVER;
import static org.codedefenders.validation.code.CodeValidatorLevel.MODERATE;

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
     * User IDs of users assigned to any staged game.
     */
    private final Set<Integer> assignedUsers = new HashSet<>();

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
     * Returns the formatted staged game ID corresponding to the given numeric staged game ID.
     * @param gameId The numeric staged game ID.
     * @return The formatted staged game ID.
     */
    public String numericToFormattedGameId(int gameId) {
        return "T" + gameId;
    }

    /**
     * Returns the numeric staged game ID corresponding to the given formatted staged game ID.
     * @param formattedGameId The formatted staged game ID.
     * @return The numeric staged game ID.
     */
    public Optional<Integer> formattedToNumericGameId(String formattedGameId) {
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
     * Adds and returns a new staged game with the given settings.
     * @param gameSettings The settings for the staged game.
     * @return The newly created staged game.
     */
    public StagedGame addStagedGame(GameSettings gameSettings) {
        int id = currentId++;
        StagedGame stagedGame = new StagedGame(id, new GameSettings(gameSettings));
        stagedGames.put(id, stagedGame);
        return stagedGame;
    }

    /**
     * Removes a staged game from the list. Players must not be added to or removed from the staged game after removal.
     * @param gameId The ID of the staged game to remove.
     * @return {@code true} if the game was in the list, {@code false} otherwise.
     */
    public boolean removeStagedGame(int gameId) {
        StagedGame stagedGame = stagedGames.get(gameId);
        if (stagedGame == null) {
            return false;
        }

        assignedUsers.removeAll(stagedGame.getPlayers());
        stagedGames.remove(gameId);
        stagedGame.delete();
        return true;
    }

    /**
     * Returns the IDs of all users currently assigned to any staged game.
     * @return The IDs of all users currently assigned to any staged game.
     */
    public Set<Integer> getAssignedUsers() {
        return Collections.unmodifiableSet(assignedUsers);
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
        @Expose private final GameSettings gameSettings;

        /**
         * User IDs of users listed as attackers for the staged game.
         */
        @Expose private final List<Integer> attackers;

        /**
         * User IDs of users listed as defenders for the staged game.
         */
        @Expose private final List<Integer> defenders;

        /**
         * If this staged game is still part of the list of the list of staged games.
         */
        private boolean exists;

        private StagedGame(int id, GameSettings gameSettings) {
            this.id = id;
            this.gameSettings = gameSettings;
            this.attackers = new ArrayList<>();
            this.defenders = new ArrayList<>();
            this.exists = true;
        }

        /**
         * Returns the ID of the staged game.
         * @return The ID of the staged game.
         */
        public int getId() {
            return id;
        }

        /**
         * Returns the ID of the staged game formatted as a string to differentiate it from IDs of normal games.
         * @return The formatted ID of the staged game.
         */
        public String getFormattedId() {
            return numericToFormattedGameId(getId());
        }

        /**
         * Returns the game settings of the staged game.
         * @return The game settings of the staged game.
         */
        public GameSettings getGameSettings() {
            return gameSettings;
        }

        /**
         * Returns the user IDs of users listed as attackers for the staged game.
         * @return The user IDs of users listed as attackers for the staged game.
         */
        public List<Integer> getAttackers() {
            return Collections.unmodifiableList(attackers);
        }

        /**
         * Returns the user IDs of users listed as defenders for the staged game.
         * @return The user IDs of users listed as defenders for the staged game.
         */
        public List<Integer> getDefenders() {
            return Collections.unmodifiableList(defenders);
        }

        /**
         * Returns the user IDs of users listed as players for the staged game.
         * This includes both attackers and defenders.
         * @return The user IDs of users listed as players for the staged game.
         */
        public List<Integer> getPlayers() {
            List<Integer> players = new ArrayList<>();
            players.addAll(attackers);
            players.addAll(defenders);
            return players;
        }

        /**
         * Assigns the given user ID to the game as an attacker.
         * @param userId The user ID to add.
         * @return {@code true} if the user was added successfully,
         *         {@code false} if the user is already assigned to a staged game, or the user doesn't exist.
         * @throws IllegalStateException If the game is no longer part of the list of staged games.
         */
        public boolean addAttacker(int userId) {
            if (!exists) {
                throw new IllegalStateException("Staged game no longer exists.");
            }
            if (assignedUsers.contains(userId)) {
                return false;
            }
            attackers.add(userId);
            assignedUsers.add(userId);
            return true;
        }

        /**
         * Assigns the given user ID to the game as an defender.
         * @param userId The user ID to add.
         * @return {@code true} if the user was added successfully,
         *         {@code false} if the user is already assigned to a staged game, or the user doesn't exist.
         * @throws IllegalStateException If the game is no longer part of the list of staged games.
         */
        public boolean addDefender(int userId) {
            if (!exists) {
                throw new IllegalStateException("Staged game no longer exists.");
            }
            if (assignedUsers.contains(userId)) {
                return false;
            }
            defenders.add(userId);
            assignedUsers.add(userId);
            return true;
        }

        /**
         * Removes a user ID from the game's players.
         * @param userId The user ID to remove.
         * @return {@code true} if the user was assigned to the game, {@code false} if the user wasn't.
         * @throws IllegalStateException If the game is no longer part of the list of staged games.
         */
        public boolean removePlayer(int userId) {
            if (!exists) {
                throw new IllegalStateException("Staged game no longer exists.");
            }
            if (attackers.remove((Integer) userId) || defenders.remove((Integer) userId)) {
                assignedUsers.remove(userId);
                return true;
            }
            return false;
        }

        /**
         * Marks the staged game as no longer part of the staged games list.
         */
        private void delete() {
            this.exists = false;
        }
    }

    public static class GameSettings implements Serializable {
        @Expose private GameType gameType;

        @Expose private GameClass cut;
        @Expose private Boolean withMutants;
        @Expose private Boolean withTests;

        @Expose private Integer maxAssertionsPerTest;
        @Expose private CodeValidatorLevel mutantValidatorLevel;
        @Expose private Boolean chatEnabled;
        @Expose private Boolean captureIntentions;
        @Expose private Integer equivalenceThreshold;
        @Expose private GameLevel level;
        @Expose private Role creatorRole;
        @Expose private Integer gameDurationMinutes;

        @Expose private Boolean startGame;

        @Expose private Integer classroomId;

        /**
         * Creates a new GameSettings object with empty settings.
         */
        public GameSettings() {

        }

        /**
         * Creates a copy of the given GameSettings object.
         * @param other The settings to copy.
         */
        public GameSettings(GameSettings other) {
            this.gameType = other.gameType;
            this.cut = other.cut;
            this.withMutants = other.withMutants;
            this.withTests = other.withTests;
            this.maxAssertionsPerTest = other.maxAssertionsPerTest;
            this.mutantValidatorLevel = other.mutantValidatorLevel;
            this.chatEnabled = other.chatEnabled;
            this.captureIntentions = other.captureIntentions;
            this.equivalenceThreshold = other.equivalenceThreshold;
            this.level = other.level;
            this.creatorRole = other.creatorRole;
            this.startGame = other.startGame;
            this.gameDurationMinutes = other.gameDurationMinutes;
            this.classroomId = other.classroomId;
        }

        public GameType getGameType() {
            return gameType;
        }

        public void setGameType(GameType gameType) {
            this.gameType = gameType;
        }

        public GameClass getCut() {
            return cut;
        }

        public void setCut(GameClass cut) {
            this.cut = cut;
        }

        public boolean isWithMutants() {
            return withMutants;
        }

        public void setWithMutants(boolean withMutants) {
            this.withMutants = withMutants;
        }

        public boolean isWithTests() {
            return withTests;
        }

        public void setWithTests(boolean withTests) {
            this.withTests = withTests;
        }

        public int getMaxAssertionsPerTest() {
            return maxAssertionsPerTest;
        }

        public void setMaxAssertionsPerTest(int maxAssertionsPerTest) {
            this.maxAssertionsPerTest = maxAssertionsPerTest;
        }

        public CodeValidatorLevel getMutantValidatorLevel() {
            return mutantValidatorLevel;
        }

        public void setMutantValidatorLevel(CodeValidatorLevel mutantValidatorLevel) {
            this.mutantValidatorLevel = mutantValidatorLevel;
        }

        public boolean isChatEnabled() {
            return chatEnabled;
        }

        public void setChatEnabled(boolean chatEnabled) {
            this.chatEnabled = chatEnabled;
        }

        public boolean isCaptureIntentions() {
            return captureIntentions;
        }

        public void setCaptureIntentions(boolean captureIntentions) {
            this.captureIntentions = captureIntentions;
        }

        public int getEquivalenceThreshold() {
            return equivalenceThreshold;
        }

        public void setEquivalenceThreshold(int equivalenceThreshold) {
            this.equivalenceThreshold = equivalenceThreshold;
        }

        public GameLevel getLevel() {
            return level;
        }

        public void setLevel(GameLevel level) {
            this.level = level;
        }

        public Role getCreatorRole() {
            return creatorRole;
        }

        public void setCreatorRole(Role creatorRole) {
            this.creatorRole = creatorRole;
        }

        public int getGameDurationMinutes() {
            return gameDurationMinutes;
        }

        public void setGameDurationMinutes(final int duration) {
            this.gameDurationMinutes = duration;
        }

        public boolean isStartGame() {
            return startGame;
        }

        public void setStartGame(boolean startGame) {
            this.startGame = startGame;
        }

        public Optional<Integer> getClassroomId() {
            return Optional.ofNullable(classroomId);
        }

        public void setClassroomId(Integer classroomId) {
            this.classroomId = classroomId;
        }

        public static GameSettings getDefault() {
            GameSettings gameSettings = new GameSettings();
            gameSettings.setGameType(MULTIPLAYER);
            gameSettings.setWithMutants(false);
            gameSettings.setWithTests(false);
            gameSettings.setMaxAssertionsPerTest(3);
            gameSettings.setMutantValidatorLevel(MODERATE);
            gameSettings.setChatEnabled(true);
            gameSettings.setCaptureIntentions(false);
            gameSettings.setEquivalenceThreshold(0);
            gameSettings.setLevel(HARD);
            gameSettings.setCreatorRole(OBSERVER);
            gameSettings.setStartGame(false);
            gameSettings.setClassroomId(null);

            final int currentDefaultGameDurationMinutes = AdminDAO.getSystemSetting(
                    AdminSystemSettings.SETTING_NAME.GAME_DURATION_MINUTES_DEFAULT).getIntValue();
            gameSettings.setGameDurationMinutes(currentDefaultGameDurationMinutes);

            return gameSettings;
        }

    }
}
