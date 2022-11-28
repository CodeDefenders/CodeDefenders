package org.codedefenders.beans.admin;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
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
import java.util.stream.Stream;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.admin.StagedGameList.GameSettings;
import org.codedefenders.beans.admin.StagedGameList.StagedGame;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.MeleeGameDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.UserEntity;
import org.codedefenders.model.UserInfo;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.servlets.admin.AdminCreateGames;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.JSONUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import static java.text.MessageFormat.format;
import static org.codedefenders.beans.admin.AdminCreateGamesBean.RoleAssignmentMethod.RANDOM;
import static org.codedefenders.beans.admin.StagedGameList.GameSettings.GameType.MELEE;
import static org.codedefenders.beans.admin.StagedGameList.GameSettings.GameType.MULTIPLAYER;
import static org.codedefenders.util.Constants.DUMMY_ATTACKER_USER_ID;
import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;

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

    private final Object synchronizer = new Object();
    private final CodeDefendersAuth login;
    private final MessagesBean messages;
    private final GameManagingUtils gameManagingUtils;
    private final EventDAO eventDAO;
    private final UserRepository userRepo;

    @Inject
    public AdminCreateGamesBean(CodeDefendersAuth login, MessagesBean messages, GameManagingUtils gameManagingUtils, EventDAO eventDAO, UserRepository userRepo) {
        this.login = login;
        this.messages = messages;
        this.gameManagingUtils = gameManagingUtils;
        this.eventDAO = eventDAO;
        this.userRepo = userRepo;
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
        for (StagedGame stagedGame : stagedGameList.getStagedGames().values()) {
            for (int userId : stagedGame.getPlayers()) {
                if (!userInfos.containsKey(userId)) {
                    stagedGame.removePlayer(userId);
                }
            }
        }
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
        /* Split users into attackers and defenders. */
        Set<UserInfo> attackers = new HashSet<>();
        Set<UserInfo> defenders = new HashSet<>();
        if (gameSettings.getGameType() != MELEE) {
            assignRoles(users, roleAssignmentMethod, attackersPerGame, defendersPerGame, attackers, defenders);
        } else {
            /* Add all users to one role for melee games to avoid player numbers differing by more than 1 between games.
             * For non-melee games this is expected, since, if the players can't be evenly distributed between games,
             * games that are assigned more attackers should also get assigned more defenders as other games. */
            attackers.addAll(users);
        }

        int numGames;
        if (gameSettings.getGameType() != MELEE) {
            numGames = users.size() / (attackersPerGame + defendersPerGame);
            /* Avoid empty games. */
            if (numGames > attackers.size() && numGames > defenders.size())  {
                int numGames1 = attackersPerGame > 0 ? attackers.size() / attackersPerGame : 0;
                int numGames2 = defendersPerGame > 0 ? defenders.size() / defendersPerGame : 0;
                numGames = Math.max(numGames1, numGames2);
            }
        } else {
            numGames = users.size() / playersPerGame;
        }

        /* Always create at least one game. */
        if (numGames == 0) {
            numGames = 1;
        }

        /* Assign attackers and defenders to teams. */
        List<List<UserInfo>> attackerTeams = splitIntoTeams(attackers, numGames, teamAssignmentMethod);
        List<List<UserInfo>> defenderTeams = splitIntoTeams(defenders, numGames, teamAssignmentMethod);

        for (int i = 0; i < numGames; i++) {
            StagedGame stagedGame = stagedGameList.addStagedGame(gameSettings);
            List<UserInfo> attackerTeam = attackerTeams.get(i);
            List<UserInfo> defenderTeam = defenderTeams.get(i);
            for (UserInfo user : attackerTeam) {
                stagedGame.addAttacker(user.getUser().getId());
            }
            for (UserInfo user : defenderTeam) {
                stagedGame.addDefender(user.getUser().getId());
            }
        }

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
            if (insertStagedGame(stagedGame)) {
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
        boolean success;
        switch (role) {
            case PLAYER:
            case ATTACKER:
                success = stagedGame.addAttacker(user.getId());
                break;
            case DEFENDER:
                success = stagedGame.addDefender(user.getId());
                break;
            default:
                messages.add(format("ERROR: Cannot add player to staged game with role {0}. Invalid role.", role));
                return false;
        }

        if (success) {
            messages.add(format("Added user {0} to staged game {1} as {2}.",
                    user.getId(), stagedGame.getFormattedId(), role.getFormattedString()));
        } else {
            messages.add(format("ERROR: Cannot add user {0} to staged game {1}. "
                    + "User is already assigned to a different staged game.",
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

    /**
     * Assigns roles to a collection of users based on a {@link RoleAssignmentMethod}. The users will be added to the
     * given {@code attackers} and {@code defenders} sets accordingly. Users that cannot be assigned with the given
     * {@link RoleAssignmentMethod} are assigned {@link RoleAssignmentMethod#RANDOM randomly}, trying to assign the
     * correct number of attackers and defenders.
     * <br/>
     * <br/>
     * The passed {@code attackers} and {@code defenders} sets can be non-empty. In this case, the number of already
     * assigned attackers and defenders
     * will be taken into account.
     * @param users The users to be assigned roles. Must be disjoint with {@code attackers} and {@code defenders}.
     * @param method The method of assigning the roles.
     * @param attackersPerGame The number of attackers per game.
     * @param defendersPerGame The number of defenders per game.
     * @param attackers The users assigned as attackers. Must be disjoint with {@code users} and {@code defenders}.
     * @param defenders The users assigned as defenders. Must be disjoint with {@code users} and {@code attackers}.
     * @throws IllegalArgumentException If the given {@code users}, {@code attackers} and {@code defenders} sets
     *                                  are not disjoint.
     */
    public void assignRoles(Set<UserInfo> users, RoleAssignmentMethod method,
                            int attackersPerGame, int defendersPerGame,
                            Set<UserInfo> attackers, Set<UserInfo> defenders) {

        if (attackersPerGame < 0 || defendersPerGame < 0 || attackersPerGame + defendersPerGame == 0) {
            throw new IllegalArgumentException(format("Invalid team sizes. "
                    + "Attackers per game: {0}, defenders per game: {1}.",
                    attackersPerGame, defendersPerGame));
        }

        if (Stream.of(users, attackers, defenders).flatMap(Collection::stream).distinct().count()
                != users.size() + attackers.size() + defenders.size()) {
            throw new IllegalArgumentException("User sets must be disjoint.");
        }

        switch (method) {
            case RANDOM:
                /* Calculate the number of attackers to assign, while taking into account how users have previously been
                 * distributed. (This method can be called with non-empty attackers and defenders sets containing
                 * already assigned users.) */
                int numUsers = users.size() + attackers.size() + defenders.size();
                int numAttackers = (int) Math.round(numUsers
                        * ((double) attackersPerGame / (attackersPerGame + defendersPerGame)));
                int remainingNumAttackers = Math.max(0, numAttackers - attackers.size());
                remainingNumAttackers = Math.min(remainingNumAttackers, users.size());

                List<UserInfo> shuffledUsers = new ArrayList<>(users);
                Collections.shuffle(shuffledUsers);
                for (int i = 0; i < remainingNumAttackers; i++) {
                    attackers.add(shuffledUsers.get(i));
                }
                for (int i = remainingNumAttackers; i < shuffledUsers.size(); i++) {
                    defenders.add(shuffledUsers.get(i));
                }

                break;

            case OPPOSITE:
                Set<UserInfo> remainingUsers = new HashSet<>();

                for (UserInfo userInfo : users) {
                    if (userInfo.getLastRole() == Role.ATTACKER) {
                        defenders.add(userInfo);
                    } else if (userInfo.getLastRole() == Role.DEFENDER) {
                        attackers.add(userInfo);
                    } else {
                        remainingUsers.add(userInfo);
                    }
                }

                /* Randomly assign remaining users (that were neither attacker or defender in their last game). */
                assignRoles(remainingUsers, RANDOM, attackersPerGame, defendersPerGame, attackers, defenders);

                break;

            default:
                throw new IllegalArgumentException(format("Unknown role assignment method: {0}.", method));
        }
    }

    /**
     * Splits the given users into teams according to a {@link TeamAssignmentMethod}. If the users cannot be split into
     * teams evenly, the size of some teams is increased by 1 to fit the remaining users into teams.
     * @param users The users to be split into teams.
     * @param numTeams The number of teams to split the users into.
     * @param method The method of splitting the users into teams.
     * @return A list of the assigned teams.
     * @throws IllegalArgumentException If {@code numTeams} is <= 0;
     */
    public List<List<UserInfo>> splitIntoTeams(Set<UserInfo> users, int numTeams, TeamAssignmentMethod method) {
        if (numTeams <= 0) {
            throw new IllegalArgumentException("Need at least one team to be able to assign players to teams.");
        }

        List<UserInfo> usersList = new ArrayList<>(users);

        switch (method) {
            case RANDOM:
                Collections.shuffle(usersList);
                break;
            case SCORE_DESCENDING:
                usersList.sort(Comparator.comparingInt(UserInfo::getTotalScore).reversed());
                break;
            default:
                throw new IllegalArgumentException(format("Unknown team assignment method: {0}.", method));
        }

        int numUsersPerTeam = usersList.size() / numTeams;
        int numRemainingUsers = usersList.size() % numTeams;

        List<List<UserInfo>> teams = new ArrayList<>();

        int index = 0;
        for (int i = 0; i < numTeams; i++) {
            List<UserInfo> subList;
            if (i < numRemainingUsers) {
                subList = usersList.subList(index, index + numUsersPerTeam + 1);
                index += numUsersPerTeam + 1;
            } else {
                subList = usersList.subList(index, index + numUsersPerTeam);
                index += numUsersPerTeam;
            }
            teams.add(new ArrayList<>(subList));
        }

        return teams;
    }

    /**
     * Creates a stages game as a real game and adds its assigned users to it.
     * @param stagedGame The staged game to create.
     * @return {@code true} if the game was successfully created, {@code false} if not.
     */
    public boolean insertStagedGame(StagedGame stagedGame) {
        return insertStagedGame(stagedGame,null);
    }
    public boolean insertStagedGame(StagedGame stagedGame, String returnUrl) {
        GameSettings gameSettings = stagedGame.getGameSettings();

        /* Create the game. */
        AbstractGame game;
        if (gameSettings.getGameType() == MULTIPLAYER) {
            game = new MultiplayerGame.Builder(gameSettings.getCut().getId(),
                    login.getUserId(),
                    gameSettings.getMaxAssertionsPerTest())
                    .cut(gameSettings.getCut())
                    .mutantValidatorLevel(gameSettings.getMutantValidatorLevel())
                    .chatEnabled(gameSettings.isChatEnabled())
                    .capturePlayersIntention(gameSettings.isCaptureIntentions())
                    .automaticMutantEquivalenceThreshold(gameSettings.getEquivalenceThreshold())
                    .level(gameSettings.getLevel())
                    .returnUrl(returnUrl)
                    .build();
        } else if (gameSettings.getGameType() == MELEE) {
            game = new MeleeGame.Builder(gameSettings.getCut().getId(),
                    login.getUserId(),
                    gameSettings.getMaxAssertionsPerTest())
                    .cut(gameSettings.getCut())
                    .mutantValidatorLevel(gameSettings.getMutantValidatorLevel())
                    .chatEnabled(gameSettings.isChatEnabled())
                    .capturePlayersIntention(gameSettings.isCaptureIntentions())
                    .automaticMutantEquivalenceThreshold(gameSettings.getEquivalenceThreshold())
                    .level(gameSettings.getLevel())
                    .returnUrl(returnUrl)
                    .build();
        } else {
            messages.add(format("ERROR: Cannot create staged game {0}. Invalid game type: {1}.",
                    stagedGame.getFormattedId(), gameSettings.getGameType().getName()));
            return false;
        }

        /* Insert the game. */
        game.setEventDAO(eventDAO);
        game.setUserRepository(userRepo);
        if (!game.insert()) {
            messages.add(format("ERROR: Could not create game for staged game {0}.",
                    stagedGame.getFormattedId()));
            return false;
        }

        /* Add system users and predefined mutants/tests. */
        if (gameSettings.getGameType() != MELEE) {
            if (!game.addPlayer(DUMMY_ATTACKER_USER_ID, Role.ATTACKER)
                    || !game.addPlayer(DUMMY_DEFENDER_USER_ID, Role.DEFENDER)) {
                messages.add(format("ERROR: Could not add system players to game {0}.",
                        stagedGame.getFormattedId()));
                return false;
            }
        } else {
            if (!game.addPlayer(DUMMY_ATTACKER_USER_ID, Role.PLAYER)
                    || !game.addPlayer(DUMMY_DEFENDER_USER_ID, Role.PLAYER)) {
                messages.add(format("ERROR: Could not add system players to game {0}.",
                        stagedGame.getFormattedId()));
                return false;
            }
        }

        if (gameSettings.isWithMutants() || gameSettings.isWithTests()) {
            gameManagingUtils.addPredefinedMutantsAndTests(game,
                    gameSettings.isWithMutants(), gameSettings.isWithTests());
        }

        /* Add users to the game. */
        if (gameSettings.getGameType() == MULTIPLAYER) {
            if (gameSettings.getCreatorRole() == Role.ATTACKER || gameSettings.getCreatorRole() == Role.DEFENDER) {
                game.addPlayer(login.getUserId(), gameSettings.getCreatorRole());
            }
            for (int userId : stagedGame.getAttackers()) {
                UserInfo user = userInfos.get(userId);
                if (user != null) {
                    game.addPlayer(user.getUser().getId(), Role.ATTACKER);
                } else {
                    messages.add(format("ERROR: Cannot add user {0} to existing game {1} as {2}. "
                            + "User does not exist.",
                            userId, game.getId(), Role.ATTACKER.getFormattedString()));
                }
            }
            for (int userId : stagedGame.getDefenders()) {
                UserInfo user = userInfos.get(userId);
                if (user != null) {
                    game.addPlayer(user.getUser().getId(), Role.DEFENDER);
                } else {
                    messages.add(format("ERROR: Cannot add user {0} to existing game {1} as {2}. "
                            + "User does not exist.",
                            userId, game.getId(), Role.DEFENDER.getFormattedString()));
                }
            }
        } else if (gameSettings.getGameType() == MELEE) {
            if (gameSettings.getCreatorRole() == Role.PLAYER) {
                game.addPlayer(login.getUserId(), gameSettings.getCreatorRole());
            }
            for (int userId : stagedGame.getPlayers()) {
                UserInfo user = userInfos.get(userId);
                if (user != null) {
                    game.addPlayer(user.getUser().getId(), Role.PLAYER);
                } else {
                    messages.add(format("ERROR: Cannot add user {0} to existing game {1} as {2}. "
                            + "User does not exist.",
                            userId, game.getId(), Role.PLAYER.getFormattedString()));
                }
            }
        }

        /* Start game if configured to. */
        if (gameSettings.isStartGame()) {
            game.setState(GameState.ACTIVE);
            game.update();
        }

        return true;
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

    /* ============================================================================================================== */

    public enum RoleAssignmentMethod {
        /**
         * Users are assigned roles randomly, trying to assign the correct number of attackers and defenders.
         */
        RANDOM,

        /**
         * Users are assigned the role opposite of the last role they played as.
         */
        OPPOSITE
    }

    public enum TeamAssignmentMethod {
        /**
         * Teams are assigned randomly.
         */
        RANDOM,

        /**
         * Teams are assigned based on the total score of users,
         * putting users with similar total scores in the same team.
         */
        SCORE_DESCENDING
    }
}
