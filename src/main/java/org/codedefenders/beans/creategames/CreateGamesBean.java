package org.codedefenders.beans.creategames;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.MeleeGameDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Role;
import org.codedefenders.model.UserEntity;
import org.codedefenders.model.creategames.GameSettings;
import org.codedefenders.model.creategames.StagedGameList;
import org.codedefenders.model.creategames.StagedGameList.StagedGame;
import org.codedefenders.model.creategames.gameassignment.GameAssignment;
import org.codedefenders.model.creategames.gameassignment.GameAssignmentMethod;
import org.codedefenders.model.creategames.gameassignment.RandomGameAssignment;
import org.codedefenders.model.creategames.gameassignment.ScoreGameAssignment;
import org.codedefenders.model.creategames.roleassignment.MeleeRoleAssignment;
import org.codedefenders.model.creategames.roleassignment.OppositeRoleAssignment;
import org.codedefenders.model.creategames.roleassignment.RandomRoleAssignment;
import org.codedefenders.model.creategames.roleassignment.RoleAssignment;
import org.codedefenders.model.creategames.roleassignment.RoleAssignmentMethod;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.CreateGamesService;
import org.codedefenders.servlets.admin.AdminCreateGames;
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
 * assigned to staged games are valid users (i.e. existing non-system users that are active).
 *
 * @see StagedGameList
 * @see AdminCreateGames
 */
public abstract class CreateGamesBean<T extends CreateGamesBean.UserInfo> implements Serializable {
    private final MessagesBean messages;
    private final EventDAO eventDAO;
    private final UserRepository userRepo;
    private final CreateGamesService createGamesService;

    protected final StagedGameList stagedGames;
    protected final Map<Integer, T> userInfos;

    @Inject
    public CreateGamesBean(StagedGameList stagedGames,
                           MessagesBean messages,
                           EventDAO eventDAO,
                           UserRepository userRepo,
                           CreateGamesService createGamesService) {
        this.messages = messages;
        this.eventDAO = eventDAO;
        this.userRepo = userRepo;
        this.createGamesService = createGamesService;
        this.stagedGames = stagedGames;
        userInfos = new HashMap<>();
    }

    /**
     * Returns a monitor that can be used to synchronize operations on a specific game list.
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
     * Fetches user information for all possible players and creators in the create-games context.
     */
    protected abstract Set<T> fetchUserInfos();

    /**
     * Fetches the IDs of all active multiplayer games in the context.
     */
    protected abstract Set<Integer> fetchAvailableMultiplayerGames();

    /**
     * Fetches the IDs of all active melee games in the context.
     */
    protected abstract Set<Integer> fetchAvailableMeleeGames();

    /**
     * Updates the context to keep users and staged games consistent with the actual application state.
     */
    public void update() {
        userInfos.clear();
        for (T userInfo : fetchUserInfos()) {
            userInfos.put(userInfo.getId(), userInfo);
        }
        stagedGames.retainUsers(userInfos.keySet());
    }

    /**
     * Returns user information for all possible players and creators in the create-games context.
     */
    public Map<Integer, T> getUserInfos() {
        return Collections.unmodifiableMap(userInfos);
    }

    /**
     * Returns user information for the given user ID.
     */
    public T getUserInfo(int userId) {
        return userInfos.get(userId);
    }

    /**
     * Constructs the requested role assignment strategy.
     */
    public RoleAssignment getRoleAssignment(RoleAssignmentMethod method) {
        switch (method) {
            case RANDOM:
                return new RandomRoleAssignment();
            case OPPOSITE:
                return new OppositeRoleAssignment(
                        userId -> getUserInfo(userId).getLastRole(),
                        new RandomRoleAssignment());
            default:
                throw new IllegalStateException("Unknown role assignment method: " + method);
        }
    }

    /**
     * Constructs the requested game assignment strategy.
     */
    public GameAssignment getGameAssignment(GameAssignmentMethod method) {
        switch (method) {
            case RANDOM:
                return new RandomGameAssignment();
            case SCORE_DESCENDING:
                return new ScoreGameAssignment(
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
     * @param roleAssignmentMethod The method of assigning roles to users. Only relevant for non-melee games.
     * @param gameAssignmentMethod The method of assigning users to teams.
     * @param attackersPerGame The number of attackers per game, or the number of players for melee games.
     * @param defendersPerGame The number of defenders per game. Only relevant for non-melee games.
     * @see RoleAssignmentMethod
     * @see GameAssignmentMethod
     */
    public void stageGamesWithUsers(Set<Integer> userIds, GameSettings gameSettings,
                                    RoleAssignmentMethod roleAssignmentMethod,
                                    GameAssignmentMethod gameAssignmentMethod,
                                    int attackersPerGame, int defendersPerGame) {
        RoleAssignment roleAssignment = gameSettings.getGameType() == MELEE
                ? new MeleeRoleAssignment()
                : getRoleAssignment(roleAssignmentMethod);
        GameAssignment gameAssignment = getGameAssignment(gameAssignmentMethod);

        List<StagedGame> newGames = stagedGames.stageGamesWithUsers(userIds, gameSettings,
                roleAssignment, gameAssignment,
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
        GameSettings gameSettings = GameSettings.builder()
                .withSettings(stagedGame.getGameSettings())
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
        game.setEventDAO(eventDAO);
        game.setUserRepository(userRepo);
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
            UserInfo user = userInfos.get(userId);
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
        /* Construct maps like this, because userInfos.stream().collect(Collectors.toMap(...)) produces a
           NullPointerException for some reason. */
        Map<String, UserInfo> userByName = new HashMap<>();
        for (UserInfo userInfo : userInfos.values()) {
            userByName.put(userInfo.getName(), userInfo);
        }
        Map<String, UserInfo> userByEmail = new HashMap<>();
        for (UserInfo userInfo : userInfos.values()) {
            userByEmail.put(userInfo.getEmail().toLowerCase(), userInfo);
        }

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
        return gson.toJson(getStagedGames().getStagedGames());
    }

    public String getActiveMultiplayerGameIdsJSON() {
        List<Integer> gameIds = MultiplayerGameDAO.getAvailableMultiplayerGames().stream()
                .map(AbstractGame::getId)
                .collect(Collectors.toList());
        Gson gson = new GsonBuilder().create();
        return gson.toJson(gameIds);
    }

    public String getActiveMeleeGameIdsJSON() {
        List<Integer> gameIds = MeleeGameDAO.getAvailableMeleeGames().stream()
                .map(AbstractGame::getId)
                .collect(Collectors.toList());
        Gson gson = new GsonBuilder().create();
        return gson.toJson(gameIds);
    }

    public String getUnassignedUserIdsJSON() {
        List<Integer> userIds = userRepo.getUnassignedUsers().stream()
                .map(UserEntity::getId)
                .collect(Collectors.toList());
        Gson gson = new GsonBuilder().create();
        return gson.toJson(userIds);
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
        Map<Integer, GameClass> classes = stagedGames.getStagedGames().values().stream()
                .map(StagedGame::getGameSettings)
                .map(GameSettings::getClassId)
                .distinct()
                .map(GameClassDAO::getClassForId)
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
}
