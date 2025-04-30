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
package org.codedefenders.beans.creategames;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.EventDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Role;
import org.codedefenders.model.creategames.GameSettings;
import org.codedefenders.model.creategames.StagedGameList;
import org.codedefenders.model.creategames.StagedGameList.StagedGame;
import org.codedefenders.model.creategames.gameassignment.GameAssignmentStrategy;
import org.codedefenders.model.creategames.gameassignment.RandomGameAssignmentStrategy;
import org.codedefenders.model.creategames.gameassignment.SortedGameAssignmentStrategy;
import org.codedefenders.model.creategames.roleassignment.MeleeRoleAssignmentStrategy;
import org.codedefenders.model.creategames.roleassignment.OppositeRoleAssignmentStrategy;
import org.codedefenders.model.creategames.roleassignment.RandomRoleAssignmentStrategy;
import org.codedefenders.model.creategames.roleassignment.RoleAssignmentStrategy;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.CreateGamesService;
import org.codedefenders.servlets.creategames.CreateGamesServlet;
import org.codedefenders.util.JSONUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;

import static java.text.MessageFormat.format;
import static org.codedefenders.game.GameType.MELEE;

/**
 * <p>Provides information about staged games and users for the admin create games page,
 * and wraps some operations of {@link StagedGameList} to add messages for the user.
 *
 * <p>It provides the consistencies of {@link StagedGameList} and additionally guarantees that all users
 * assigned to staged games are valid users (e.g. active non-system users for the admin page, or classroom members for
 * the classroom page).
 *
 * @see StagedGameList
 * @see CreateGamesServlet
 */
public abstract class CreateGamesBean implements Serializable {
    private final MessagesBean messages;
    private final EventDAO eventDAO;
    private final UserRepository userRepo;
    private final CreateGamesService createGamesService;
    private final GameClassRepository gameClassRepo;
    private final CodeDefendersAuth login;

    protected final StagedGameList stagedGames;

    @Inject
    public CreateGamesBean(StagedGameList stagedGames,
                           MessagesBean messages,
                           EventDAO eventDAO,
                           UserRepository userRepo,
                           CreateGamesService createGamesService,
                           GameClassRepository gameClassRepo,
                           CodeDefendersAuth login) {
        this.messages = messages;
        this.eventDAO = eventDAO;
        this.userRepo = userRepo;
        this.createGamesService = createGamesService;
        this.stagedGames = stagedGames;
        this.gameClassRepo = gameClassRepo;
        this.login = login;
    }

    /**
     * Returns a monitor that can be used to synchronize operations on this staged game context.
     */
    public Object getSynchronizer() {
        return stagedGames;
    }

    /**
     * Returns the staged games list that is managed by the bean.
     * @return The beans staged games list.
     */
    public StagedGameList getStagedGames() {
        return stagedGames;
    }

    /**
     * Returns a staged game by its id.
     * @return The staged game, or {@code null} if no game with the ID exists in the list.
     */
    public StagedGame getStagedGame(int stagedGameId) {
        return stagedGames.getGame(stagedGameId);
    }

    /**
     * Fetches user information for all possible players and creators in the create-games context.
     */
    public abstract Map<Integer, ? extends UserInfo> getUserInfos();

    /**
     * Fetches the IDs of all active multiplayer games in the context.
     */
    public abstract Set<Integer> getAvailableMultiplayerGames();

    /**
     * Fetches the IDs of all active melee games in the context.
     */
    public abstract Set<Integer> getAvailableMeleeGames();

    /**
     * Fetches the IDs of all active melee games in the context.
     */
    public abstract Set<Integer> getAssignedUsers();

    /**
     * Returns the kind of staged games page (currently ADMIN or CLASSROOM).
     */
    public abstract Kind getKind();

    /**
     * Returns the user information for the given user ID.
     * @return The user information for the given user ID,
     * or {@code null} if no user with the given id is managed by the bean.
     */
    public UserInfo getUserInfo(int userId) {
        return getUserInfos().get(userId);
    }

    /**
     * Constructs the role assignment strategy for the type.
     */
    public RoleAssignmentStrategy getRoleAssignment(RoleAssignmentStrategy.Type method) {
        switch (method) {
            case RANDOM:
                return new RandomRoleAssignmentStrategy();
            case OPPOSITE:
                return new OppositeRoleAssignmentStrategy(
                        userId -> getUserInfo(userId).getLastRole(),
                        new RandomRoleAssignmentStrategy());
            default:
                throw new IllegalStateException("Unknown role assignment method: " + method);
        }
    }

    /**
     * Constructs the game assignment strategy for the type.
     */
    public GameAssignmentStrategy getGameAssignment(GameAssignmentStrategy.Type method) {
        switch (method) {
            case RANDOM:
                return new RandomGameAssignmentStrategy();
            case SCORE_DESCENDING:
                return new SortedGameAssignmentStrategy(
                        Comparator.comparingInt((Integer userId) -> getUserInfo(userId).getTotalScore()).reversed());
            default:
                throw new IllegalStateException("Unknown game assignment method: " + method);
        }
    }

    /* ============================================================================================================== */

    /**
     * Assigns selected users to teams and adds staged games with these teams to the list.
     * @param userIds The players for the staged games.
     * @param gameSettings The game settings.
     * @param roleAssignmentType The method of assigning roles to users. Only relevant for non-melee games.
     * @param gameAssignmentType The method of assigning users to teams.
     * @param attackersPerGame The number of attackers per game, or the number of players for melee games.
     * @param defendersPerGame The number of defenders per game. Only relevant for non-melee games.
     * @see RoleAssignmentStrategy.Type
     * @see GameAssignmentStrategy.Type
     */
    public void stageGamesWithUsers(Set<Integer> userIds, GameSettings gameSettings,
                                    RoleAssignmentStrategy.Type roleAssignmentType,
                                    GameAssignmentStrategy.Type gameAssignmentType,
                                    int attackersPerGame, int defendersPerGame) {
        // Remove creator
        userIds.remove(login.getUserId());

        RoleAssignmentStrategy roleAssignmentStrategy = gameSettings.getGameType() == MELEE
                ? new MeleeRoleAssignmentStrategy()
                : getRoleAssignment(roleAssignmentType);
        GameAssignmentStrategy gameAssignmentStrategy = getGameAssignment(gameAssignmentType);

        List<StagedGame> newGames = stagedGames.stageGamesWithUsers(userIds, gameSettings,
                roleAssignmentStrategy, gameAssignmentStrategy,
                attackersPerGame, defendersPerGame);

        messages.add(format("Created {0} staged games.", newGames.size()));
    }

    /**
     * Creates empty staged games.
     * @param gameSettings The settings for the staged games.
     * @param numGames The number of staged games to create.
     */
    public void stageEmptyGames(GameSettings gameSettings, int numGames) {
        for (int i = 0; i < numGames; i++) {
            stagedGames.addStagedGame(gameSettings);
        }

        messages.add(format("Created {0} empty staged games.", numGames));
    }

    /**
     * Deletes the given staged games from the list.
     * @param stagedGames The staged games to delete.
     */
    public void deleteStagedGames(List<StagedGame> stagedGames) {
        for (StagedGame stagedGame : stagedGames) {
            this.stagedGames.removeStagedGame(stagedGame.getId());
        }

        messages.add(format("Deleted {0} staged games.", stagedGames.size()));
    }

    /**
     * Creates the given staged games as real games.
     * @param stagedGames The staged games to create.
     */
    public void createStagedGames(List<StagedGame> stagedGames) {
        for (StagedGame stagedGame : stagedGames) {
            if (createGamesService.createGame(stagedGame)) {
                this.stagedGames.removeStagedGame(stagedGame.getId());
            }
        }

        messages.add(format("Created games for {0} staged games.", stagedGames.size()));
    }

    /**
     * Removes a user from a staged game. This method takes a user ID instead of a User object in case an inactive user
     * has to be removed.
     *
     * @param stagedGame The staged game.
     * @param userId The ID of the user to remove.
     * @return {@code true} if the user was assigned to the staged game, {@code false} if not.
     */
    public boolean removePlayerFromStagedGame(StagedGame stagedGame, int userId) {
        if (stagedGame.removePlayer(userId)) {
            messages.add(format(
                    "Removed user {0} from staged game {1}.",
                    userId, stagedGame.getFormattedId())
            );
            return true;
        } else {
            messages.add(format(
                    "ERROR: Cannot remove user {0} from staged game {1}. "
                    + "User is not assigned to the staged game.",
                    userId, stagedGame.getFormattedId()));
            return false;
        }
    }

    /**
     * Removes the creator from a staged game, i.e. sets their Role to Observer.
     *
     * @param stagedGame The staged game.
     */
    public void removeCreatorFromStagedGame(StagedGame stagedGame) {
        GameSettings gameSettings = GameSettings.from(stagedGame.getGameSettings())
                .setCreatorRole(Role.OBSERVER)
                .build();
        stagedGame.setGameSettings(gameSettings);
        messages.add(format(
                "Removed you from staged game {0}. Your role is now {1} again.",
                stagedGame.getFormattedId(), Role.OBSERVER)
        );
    }

    /**
     * Switches the role of a user assigned to a staged game.
     *
     * @param stagedGame The staged game.
     * @return {@code true} if the user's role could be switched, {@code false} if not.
     */
    public boolean switchRole(StagedGame stagedGame, int userId) {
        if (stagedGame.switchRole(userId)) {
            messages.add(format("Switched role of user {0} in staged game {1}.",
                    userId, stagedGame.getFormattedId()));
            return true;
        } else {
            messages.add(format("ERROR: Cannot switch role of user {0} in staged game {1}.",
                    userId, stagedGame.getFormattedId()));
            return false;
        }
    }

    /**
     * Switches the creator's role of a staged game.
     * @param stagedGame The staged game.
     * @return {@code true} if the creator's role could be switched, {@code false} if not.
     */
    public boolean switchCreatorRole(StagedGame stagedGame) {
        if (stagedGame.switchCreatorRole()) {
            messages.add(format("Switched your role in staged game {0}.",
                    stagedGame.getFormattedId()));
            return true;
        } else {
            messages.add(format("ERROR: Cannot switch your role in staged game {0}.",
                    stagedGame.getFormattedId()));
            return false;
        }
    }

    /**
     * Moves a use from one staged game to another.
     * @param stagedGameFrom The staged game to move the user from.
     * @param stagedGameTo The staged game to move the user to.
     * @param role The role the user should be added to the game as.
     * @return {@code true} if the user could be added, {@code false} if not.
     */
    public boolean movePlayerBetweenStagedGames(StagedGame stagedGameFrom, StagedGame stagedGameTo,
                                                int userId, Role role) {
        if (!stagedGameFrom.removePlayer(userId)) {
            messages.add(format(
                    "ERROR: Cannot move user {0} from staged game {1}. "
                    + "User is not assigned to the staged game.",
                    userId, stagedGameFrom.getFormattedId())
            );
            return false;
        }

        switch (role) {
            case PLAYER:
            case ATTACKER:
                stagedGameTo.addAttacker(userId);
                break;
            case DEFENDER:
                stagedGameTo.addDefender(userId);
                break;
            default:
                messages.add(format("ERROR: Cannot move player to staged game with role {0}. Invalid role.", role));
                return false;
        }

        messages.add(format("Moved user {0} from staged game {1} to staged game {2} as {3}.",
                userId, stagedGameFrom.getFormattedId(), stagedGameTo.getFormattedId(),
                role.getFormattedString()));
        return true;
    }

    /**
     * Adds a user to a staged game.
     * @param stagedGame The staged game.
     * @param role The role the user should be added to the game as.
     * @return {@code true} if the user could be added, {@code false} if not.
     */
    public boolean addPlayerToStagedGame(StagedGame stagedGame, int userId, Role role) {
        boolean success = stagedGame.addPlayer(userId, role);
        if (success) {
            messages.add(format("Added user {0} to staged game {1} as {2}.",
                    userId, stagedGame.getFormattedId(), role.getFormattedString()));
        } else {
            messages.add(format("ERROR: Cannot add user {0} to staged game {1}. ",
                    userId, stagedGame.getFormattedId()));
        }
        return success;
    }

    /**
     * Adds a user to an existing game.
     * @param game The game.
     * @param role The role the user should be added to the game as.
     * @return {@code true} if the user could be added, {@code false} if not.
     */
    public boolean addPlayerToExistingGame(AbstractGame game, int userId, Role role) {
        if (!game.addPlayer(userId, role)) {
            messages.add(format("ERROR: Cannot add user {0} to existing game {1} as {2}.",
                    userId, game.getId(), role));
            return false;
        }
        messages.add(format("Added user {0} to existing game {1} as {2}.", userId, game.getId(), role));
        return true;
    }

    /* ============================================================================================================== */

    /**
     * Maps the give user IDs to UserInfos.
     * @param userIds The user IDs.
     * @return The resulting UserInfos, or {@link Optional#empty()} if a user ID could not be mapped to a valid user.
     */
    public Optional<Set<UserInfo>> getUserInfosForIds(Collection<Integer> userIds) {
        boolean success = true;
        Set<UserInfo> users = new HashSet<>();

        for (int userId : userIds) {
            UserInfo user = getUserInfo(userId);
            if (user != null) {
                users.add(user);
                continue;
            }
            messages.add(format("ERROR: No valid user with the ID {0} exists.", userId));
            success = false;
        }

        return success ? Optional.of(users) : Optional.empty();
    }

    /**
     * Maps the give usernames/emails to UserInfos.
     * @param userNames The usernames/emails.
     * @return The resulting UserInfos, or {@link Optional#empty()} if a username/email could not be
     *         mapped to a valid user.
     */
    public Optional<Set<UserInfo>> getUserInfosForNamesAndEmails(Collection<String> userNames) {
        Map<String, UserInfo> userByName = getUserInfos().values().stream().collect(Collectors.toMap(
                UserInfo::getName,
                Function.identity()
        ));
        Map<String, UserInfo> userByEmail = getUserInfos().values().stream().collect(Collectors.toMap(
                user -> user.getEmail().toLowerCase(),
                Function.identity()
        ));

        boolean success = true;
        Set<UserInfo> users = new HashSet<>();

        for (String userNameOrEmail : userNames) {
            UserInfo user = userByName.get(userNameOrEmail);
            if (user != null) {
                users.add(user);
                continue;
            }
            user = userByEmail.get(userNameOrEmail.toLowerCase());
            if (user != null) {
                users.add(user);
                continue;
            }
            messages.add(format("ERROR: No valid user with name/email {0} exists.", userNameOrEmail));
            success = false;
        }

        return success ? Optional.of(users) : Optional.empty();
    }

    /* ============================================================================================================== */

    public String getUserInfosAsJSON() {
        Gson gson = new GsonBuilder()
                .serializeNulls()
                .registerTypeAdapterFactory(new JSONUtils.MapTypeAdapterFactory())
                .registerTypeAdapter(Instant.class, new JSONUtils.InstantSerializer())
                .create();
        return gson.toJson(getUserInfos());
    }

    public String getStagedGamesAsJSON() {
        Gson gson = new GsonBuilder()
                .serializeNulls()
                .registerTypeAdapterFactory(new JSONUtils.MapTypeAdapterFactory())
                .registerTypeAdapterFactory(new JSONUtils.SetTypeAdapterFactory())
                .create();
        return gson.toJson(getStagedGames().getMap());
    }

    public String getAvailableMultiplayerGameIdsJSON() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new JSONUtils.SetTypeAdapterFactory())
                .create();
        return gson.toJson(getAvailableMultiplayerGames());
    }

    public String getAvailableMeleeGameIdsJSON() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new JSONUtils.SetTypeAdapterFactory())
                .create();
        return gson.toJson(getAvailableMeleeGames());
    }

    public String getAssignedUserIdsJSON() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(getAssignedUsers());
    }

    public String getUsedClassesJSON() {
        class GameClassSerializer implements JsonSerializer<GameClass> {
            @Override
            public JsonElement serialize(GameClass gameClass, Type type, JsonSerializationContext context) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", gameClass.getId());
                obj.addProperty("name", gameClass.getName());
                obj.addProperty("alias", gameClass.getAlias());
                return obj;
            }
        }
        Map<Integer, GameClass> classes = stagedGames.getMap().values().stream()
                .map(StagedGame::getGameSettings)
                .map(GameSettings::getClassId)
                .distinct()
                .map(gameClassRepo::getClassForId)
                .map(Optional::orElseThrow)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        GameClass::getId,
                        cut -> cut
                ));
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(GameClass.class, new GameClassSerializer())
                .registerTypeAdapterFactory(new JSONUtils.MapTypeAdapterFactory())
                .create();
        return gson.toJson(classes);
    }

    /* ============================================================================================================== */

    public static class UserInfo {
        @Expose
        private final int id;

        @Expose
        private final String name;

        private final String email;

        @Expose
        private final Instant lastLogin;

        @Expose
        private final Role lastRole;

        @Expose
        private final int totalScore;

        public UserInfo(int userId, String username, String email, Instant lastLogin, Role lastRole, int totalScore) {
            this.id = userId;
            this.name = username;
            this.email = email;
            this.lastLogin = lastLogin;
            this.lastRole = lastRole;
            this.totalScore = totalScore;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public Instant getLastLogin() {
            return lastLogin;
        }

        public Role getLastRole() {
            return lastRole;
        }

        public int getTotalScore() {
            return totalScore;
        }
    }

    public enum Kind {
        ADMIN,
        CLASSROOM
    }
}
