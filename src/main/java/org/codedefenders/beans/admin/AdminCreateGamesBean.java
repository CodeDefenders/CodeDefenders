package org.codedefenders.beans.admin;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.MeleeGameDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Role;
import org.codedefenders.model.UserEntity;
import org.codedefenders.model.UserInfo;
import org.codedefenders.model.creategames.GameSettings;
import org.codedefenders.model.creategames.StagedGameList;
import org.codedefenders.model.creategames.StagedGameList.StagedGame;
import org.codedefenders.model.creategames.roleassignment.MeleeRoleAssignment;
import org.codedefenders.model.creategames.roleassignment.OppositeRoleAssignment;
import org.codedefenders.model.creategames.roleassignment.RandomRoleAssignment;
import org.codedefenders.model.creategames.roleassignment.RoleAssignment;
import org.codedefenders.model.creategames.roleassignment.RoleAssignmentMethod;
import org.codedefenders.model.creategames.teamassignment.RandomGameAssignment;
import org.codedefenders.model.creategames.teamassignment.ScoreGameAssignment;
import org.codedefenders.model.creategames.teamassignment.GameAssignment;
import org.codedefenders.model.creategames.teamassignment.TeamAssignmentMethod;
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

import static java.text.MessageFormat.format;
import static org.codedefenders.game.GameType.MELEE;

/**
 * Implements the functionality of the admin create games page.
 * <br/>
 * <br/>
 * It provides the consistencies of {@link StagedGameList} and additionally guarantees that all users
 * assigned to staged games are valid users (i.e. existing non-system users that are active).
 * @see AdminCreateGames
 */
@Named("adminCreateGames")
@SessionScoped
public class AdminCreateGamesBean implements Serializable {
    private final Object synchronizer = new Serializable() {};
    private final MessagesBean messages;
    private final EventDAO eventDAO;
    private final UserRepository userRepo;
    private final CreateGamesService createGamesService;

    @Inject
    public AdminCreateGamesBean(MessagesBean messages,
                                EventDAO eventDAO, UserRepository userRepo,
                                CreateGamesService createGamesService) {
        this.messages = messages;
        this.eventDAO = eventDAO;
        this.userRepo = userRepo;
        this.createGamesService = createGamesService;
    }

    public Object getSynchronizer() {
        return synchronizer;
    }

    /**
     * The staged game list managed by the bean.
     */
    private final StagedGameList stagedGameList = new StagedGameList();

    /**
     * Maps the user IDs of all valid users to {@link UserInfo UserInfos}. This map should be updated with
     * {@link AdminCreateGamesBean#update()} on every request before this bean is used.
     */
    private final Map<Integer, UserInfo> userInfos = new HashMap<>();

    /**
     * Returns the staged games list that is managed by the bean.
     * @return The beans staged games list.
     */
    public StagedGameList getStagedGameList() {
        return stagedGameList;
    }

    /**
     * Retrieves the most recent {@link UserInfo UserInfos} from the DB and removes any users that are no longer active
     * from any staged games. This method should be called on every request before this bean is used.
     */
    public void update() {
        userInfos.clear();
        for (UserInfo userInfo : AdminDAO.getAllUsersInfo()) {
            userInfos.put(userInfo.getUser().getId(), userInfo);
        }
        stagedGameList.retainUsers(userInfos.keySet());
    }

    /**
     * Returns a mapping of the all valid users' IDs to their corresponding {@link UserInfo}.
     * @return A mapping of the all valid users' IDs to their corresponding {@link UserInfo}.
     */
    public Map<Integer, UserInfo> getUserInfos() {
        return Collections.unmodifiableMap(userInfos);
    }

    /* ============================================================================================================== */

    /**
     * Assigns selected users to teams and adds staged games with these teams to the list.
     * @param users The players for the staged games.
     * @param gameSettings The game settings.
     * @param roleAssignmentMethod The method of assigning roles to users. Only relevant for non-melee games.
     * @param teamAssignmentMethod The method of assigning users to teams.
     * @param attackersPerGame The number of attackers per game. Only relevant for non-melee games.
     * @param defendersPerGame The number of defenders per game. Only relevant for non-melee games.
     * @param playersPerGame The number of defenders per game. Only relevant for melee games.
     * @see RoleAssignmentMethod
     * @see TeamAssignmentMethod
     */
    public void stageGamesWithUsers(Set<UserInfo> users, GameSettings gameSettings,
                                    RoleAssignmentMethod roleAssignmentMethod,
                                    TeamAssignmentMethod teamAssignmentMethod,
                                    int attackersPerGame, int defendersPerGame, int playersPerGame) {

        Map<Integer, UserInfo> usersMap = users.stream()
                .collect(Collectors.toMap(
                        info -> info.getUser().getId(),
                        info -> info
                ));

        RoleAssignment role = null;
        switch (roleAssignmentMethod) {
            case RANDOM:
                role = new RandomRoleAssignment();
                break;
            case OPPOSITE:
                role = new OppositeRoleAssignment(
                        userId -> usersMap.get(userId).getLastRole(),
                        new RandomRoleAssignment());
                break;
        }
        if (gameSettings.getGameType() == MELEE) {
            role = new MeleeRoleAssignment();
        }

        GameAssignment game = null;
        switch (teamAssignmentMethod) {
            case RANDOM:
                game = new RandomGameAssignment();
                break;
            case SCORE_DESCENDING:
                game = new ScoreGameAssignment(Comparator.comparingInt(userId -> usersMap.get(userId).getTotalScore()));
                break;
        }

        int numGames = stagedGameList.stageGamesWithUsers(usersMap.keySet(), gameSettings, role, game,
                attackersPerGame, defendersPerGame, playersPerGame);
        messages.add(format("Created {0} staged games.", numGames));
    }

    /**
     * Creates empty staged games.
     * @param gameSettings The settings for the staged games.
     * @param numGames The number of staged games to create.
     */
    public void stageEmptyGames(GameSettings gameSettings, int numGames) {
        for (int i = 0; i < numGames; i++) {
            stagedGameList.addStagedGame(gameSettings);
        }

        messages.add(format("Created {0} empty staged games.", numGames));
    }

    /**
     * Deletes the given staged games from the list.
     * @param stagedGames The staged games to delete.
     */
    public void deleteStagedGames(List<StagedGame> stagedGames) {
        for (StagedGame stagedGame : stagedGames) {
            stagedGameList.removeStagedGame(stagedGame.getId());
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
                stagedGameList.removeStagedGame(stagedGame.getId());
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
        stagedGame.getGameSettings().setCreatorRole(Role.OBSERVER);
        messages.add(format(
                "Removed you from staged game {0}. Your role is now {1} again.",
                stagedGame.getFormattedId(), Role.OBSERVER)
        );
    }

    /**
     * Switches the role of a user assigned to a staged game.
     *
     * @param stagedGame The staged game.
     * @param user The user.
     * @return {@code true} if the user's role could be switched, {@code false} if not.
     */
    public boolean switchRole(StagedGame stagedGame, UserEntity user) {
        if (stagedGame.getAttackers().contains(user.getId())) {
            stagedGame.removePlayer(user.getId());
            stagedGame.addDefender(user.getId());
        } else if (stagedGame.getDefenders().contains(user.getId())) {
            stagedGame.removePlayer(user.getId());
            stagedGame.addAttacker(user.getId());
        } else {
            messages.add(format("ERROR: Cannot switch role of user {0} in staged game {1}. "
                    + "User is not assigned to the staged game.",
                    user.getId(), stagedGame.getFormattedId()));
            return false;
        }
        messages.add(format("Switched role of user {0} in staged game {1}.",
                user.getId(), stagedGame.getFormattedId()));
        return true;
    }

    /**
     * Switches the creator's role of a staged game.
     * @param stagedGame The staged game.
     * @return {@code true} if the creator's role could be switched, {@code false} if not.
     */
    public boolean switchCreatorRole(StagedGame stagedGame) {
        if (stagedGame.getGameSettings().getCreatorRole() == Role.PLAYER) {
            stagedGame.getGameSettings().setCreatorRole(Role.PLAYER);
        } else if (stagedGame.getGameSettings().getCreatorRole() == Role.ATTACKER) {
            stagedGame.getGameSettings().setCreatorRole(Role.DEFENDER);
        } else if (stagedGame.getGameSettings().getCreatorRole() == Role.DEFENDER) {
            stagedGame.getGameSettings().setCreatorRole(Role.ATTACKER);
        } else {
            messages.add(format("ERROR: Cannot switch your role in staged game {0}. "
                            + "You are not assigned to the staged game.",
                    stagedGame.getFormattedId()));
            return false;
        }
        messages.add(format("Switched your role in staged game {0}.",
                stagedGame.getFormattedId()));
        return true;
    }

    /**
     * Moves a use from one staged game to another.
     * @param stagedGameFrom The staged game to move the user from.
     * @param stagedGameTo The staged game to move the user to.
     * @param user The user.
     * @param role The role the user should be added to the game as.
     * @return {@code true} if the user could be added, {@code false} if not.
     */
    public boolean movePlayerBetweenStagedGames(StagedGame stagedGameFrom, StagedGame stagedGameTo,
                                                UserEntity user, Role role) {
        if (!stagedGameFrom.removePlayer(user.getId())) {
            messages.add(format(
                    "ERROR: Cannot move user {0} from staged game {1}. "
                    + "User is not assigned to the staged game.",
                    user.getId(), stagedGameFrom.getFormattedId())
            );
            return false;
        }

        switch (role) {
            case PLAYER:
            case ATTACKER:
                stagedGameTo.addAttacker(user.getId());
                break;
            case DEFENDER:
                stagedGameTo.addDefender(user.getId());
                break;
            default:
                messages.add(format("ERROR: Cannot move player to staged game with role {0}. Invalid role.", role));
                return false;
        }

        messages.add(format("Moved user {0} from staged game {1} to staged game {2} as {3}.",
                user.getId(), stagedGameFrom.getFormattedId(), stagedGameTo.getFormattedId(),
                role.getFormattedString()));
        return true;
    }

    /**
     * Adds a user to a staged game.
     * @param stagedGame The staged game.
     * @param user The user.
     * @param role The role the user should be added to the game as.
     * @return {@code true} if the user could be added, {@code false} if not.
     */
    public boolean addPlayerToStagedGame(StagedGame stagedGame, UserEntity user, Role role) {
        boolean success = stagedGame.addPlayer(user.getId(), role);
        if (success) {
            messages.add(format("Added user {0} to staged game {1} as {2}.",
                    user.getId(), stagedGame.getFormattedId(), role.getFormattedString()));
        } else {
            messages.add(format("ERROR: Cannot add user {0} to staged game {1}. ",
                    user.getId(), stagedGame.getFormattedId()));
        }
        return success;
    }

    /**
     * Adds a user to an existing game.
     * @param game The game.
     * @param user The user.
     * @param role The role the user should be added to the game as.
     * @return {@code true} if the user could be added, {@code false} if not.
     */
    public boolean addPlayerToExistingGame(AbstractGame game, UserEntity user, Role role) {
        game.setEventDAO(eventDAO);
        game.setUserRepository(userRepo);
        if (!game.addPlayer(user.getId(), role)) {
            messages.add(format("ERROR: Cannot add user {0} to existing game {1} as {2}.",
                    user.getId(), game.getId(), role));
            return false;
        }
        messages.add(format("Added user {0} to existing game {1} as {2}.", user.getId(), game.getId(), role));
        return true;
    }

    /* ============================================================================================================== */

    /**
     * Maps the give user IDs to UserInfos.
     * @param userIds The user IDs.
     * @return An optional with the resulting UserInfos, or {@link Optional#empty()} if a user ID could not be mapped
     *         to a valid user.
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
     * Maps the give user names/emails to UserInfos.
     * @param userNames The user names/emails.
     * @return An optional with the resulting UserInfos, or {@link Optional#empty()} if a user name/email could not be
     *         mapped to a valid user.
     */
    public Optional<Set<UserInfo>> getUserInfosForNamesAndEmails(Collection<String> userNames) {
        /* Construct maps like this, because userInfos.stream().collect(Collectors.toMap(...)) produces a
           NullPointerException for some reason. */
        Map<String, UserInfo> userByName = new HashMap<>();
        for (UserInfo user : userInfos.values()) {
            userByName.put(user.getUser().getUsername(), user);
        }
        Map<String, UserInfo> userByEmail = new HashMap<>();
        for (UserInfo user : userInfos.values()) {
            userByEmail.put(user.getUser().getEmail(), user);
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
                .registerTypeAdapter(UserInfo.class, new UserInfoSerializer())
                .registerTypeAdapter(UserEntity.class, new UserEntitySerializer())
                .create();
        return gson.toJson(getUserInfos());
    }

    public String getStagedGamesAsJSON() {
        Gson gson = new GsonBuilder()
                .serializeNulls()
                .registerTypeAdapterFactory(new JSONUtils.MapTypeAdapterFactory())
                .registerTypeAdapterFactory(new JSONUtils.SetTypeAdapterFactory())
                .registerTypeAdapter(GameClass.class, new GameClassSerializer())
                .create();
        return gson.toJson(getStagedGameList().getStagedGames());
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

    // TODO: Move this elsewhere?
    public static class UserInfoSerializer implements JsonSerializer<UserInfo> {
        @Override
        public JsonElement serialize(UserInfo userInfo, Type type, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.add("user", context.serialize(userInfo.getUser()));
            obj.addProperty("lastLogin", userInfo.getLastLogin() == null ? null
                    : userInfo.getLastLogin().toEpochMilli());
            obj.addProperty("lastRole", userInfo.getLastRole() == null ? null
                    : userInfo.getLastRole().toString());
            obj.addProperty("totalScore", userInfo.getTotalScore());
            return obj;
        }
    }

    // TODO: Move this elsewhere?
    public static class UserEntitySerializer implements JsonSerializer<UserEntity> {
        @Override
        public JsonElement serialize(UserEntity user, Type type, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", user.getId());
            obj.addProperty("username", user.getUsername());
            return obj;
        }
    }

    // TODO: Move this elsewhere?
    public static class GameClassSerializer implements JsonSerializer<GameClass> {
        @Override
        public JsonElement serialize(GameClass gameClass, Type type, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", gameClass.getId());
            obj.addProperty("name", gameClass.getName());
            obj.addProperty("alias", gameClass.getAlias());
            return obj;
        }
    }
}
