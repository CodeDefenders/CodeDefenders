package org.codedefenders.beans.admin;

import java.io.Serializable;
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
import java.util.stream.Stream;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.beans.admin.StagedGameList.GameSettings;
import org.codedefenders.beans.admin.StagedGameList.StagedGame;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.EventDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.User;
import org.codedefenders.model.UserInfo;
import org.codedefenders.servlets.admin.AdminCreateGames;
import org.codedefenders.servlets.games.GameManagingUtils;

import static java.text.MessageFormat.format;
import static org.codedefenders.beans.admin.AdminCreateGamesBean.RoleAssignmentMethod.RANDOM;
import static org.codedefenders.beans.admin.StagedGameList.GameSettings.GameType.MELEE;
import static org.codedefenders.beans.admin.StagedGameList.GameSettings.GameType.MULTIPLAYER;
import static org.codedefenders.util.Constants.DUMMY_ATTACKER_USER_ID;
import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;

/**
 * Implements the functionality of the admin create games page.
 * @see AdminCreateGames
 */
@SessionScoped
public class AdminCreateGamesBean implements Serializable {

    @Inject
    private LoginBean login;

    @Inject
    private MessagesBean messages;

    @Inject
    private GameManagingUtils gameManagingUtils;

    @Inject
    private EventDAO eventDAO;

    private StagedGameList stagedGameList;

    /**
     * Maps the user IDs of all valid users to {@link UserInfo UserInfos}. This map should be updated with
     * {@link AdminCreateGamesBean#updateUserInfos()} on every request before this bean is used.
     */
    private final Map<Integer, UserInfo> userInfos = new HashMap<>();

    /**
     * Produces the staged games list that is managed by the bean.
     * @return The beans staged games list.
     */
    @Produces
    @Named("stagedGameList")
    public StagedGameList getStagedGameList() {
        if (stagedGameList == null) {
            stagedGameList = new StagedGameList();
        }
        return stagedGameList;
    }

    /**
     * Retrieves the most recent {@link UserInfo UserInfos} from the DB.
     * This method should be called on every request before this bean is used.
     */
    public void updateUserInfos() {
        userInfos.clear();
        for (UserInfo userInfo : AdminDAO.getAllUsersInfo()) {
            userInfos.put(userInfo.getUser().getId(), userInfo);
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
     * @param roleAssignmentMethod The method of assigning roles to users.
     * @param teamAssignmentMethod The method of assigning users to teams.
     * @param attackersPerGame The number of attackers per game.
     * @param defendersPerGame The number of defenders per game.
     * @see RoleAssignmentMethod
     * @see TeamAssignmentMethod
     */
    public void stageGames(Set<UserInfo> users, GameSettings gameSettings,
                           RoleAssignmentMethod roleAssignmentMethod, TeamAssignmentMethod teamAssignmentMethod,
                           int attackersPerGame, int defendersPerGame) {
        int numGames = users.size() / (attackersPerGame + defendersPerGame);
        numGames = numGames == 0 ? 1 : numGames;

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

        messages.add(format("Staged {0} games.", numGames));
    }

    /**
     * Deletes the given staged games from the list.
     * @param stagedGames The staged games to delete.
     */
    public void deleteStagedGames(List<StagedGame> stagedGames) {
        for (StagedGame stagedGame : stagedGames) {
            stagedGameList.removeStagedGame(stagedGame.getId());
        }

        messages.add(format("Deleted {0} games.", stagedGames.size()));
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

        messages.add(format("Created {0} games.", stagedGames.size()));
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
                    "Removed user {0} from staged game T{1}.",
                    userId, stagedGame.getId())
            );
            return true;
        } else {
            messages.add(format(
                    "ERROR: Cannot remove user {0} from staged game T{1}. "
                            + "User is not assigned to the the staged game.",
                    userId, stagedGame.getId()));
            return false;
        }
    }

    /**
     * Adds a user to a staged game.
     * @param stagedGame The staged game.
     * @param user The user.
     * @param role The role the user should be added to the game as.
     * @return {@code true} if the user could be added, {@code false} if not.
     */
    public boolean addPlayerToStagedGame(StagedGame stagedGame, User user, Role role) {
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
                messages.add(format("Cannot add player with role {0}. Invalid role.", role));
                return false;
        }

        if (success) {
            messages.add(format("Added user {0} to staged game T{1} as {2}.",
                    user.getId(), stagedGame.getId(), role.getFormattedString()));
        } else {
            messages.add(format("ERROR: Cannot add user {0} to staged game T{1}. "
                            + "User is already assigned to a different staged game.",
                    user.getId(), stagedGame.getId()));
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
    public boolean addPlayerToExistingGame(AbstractGame game, User user, Role role) {
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
     * given {@code attackers} and {@code defenders} sets accordingly. The passed {@code attackers} and
     * {@code defenders} sets can be non-empty. In this case, the number of already assigned attackers and defenders
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
     * Splits the given users into teams according to a {@link TeamAssignmentMethod}.
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
                    .build();
        } else {
            messages.add(format("ERROR: Could not create staged game T{0}. Invalid game type: {1}.",
                    stagedGame.getId(), gameSettings.getGameType().getName()));
            return false;
        }

        /* Insert the game. */
        game.setEventDAO(eventDAO);
        if (!game.insert()) {
            messages.add(format("ERROR: Could not create staged game T{0}. Could not insert into the database.",
                    stagedGame.getId()));
            return false;
        }

        /* Add system users and predefined mutants/tests. */
        if (!game.addPlayer(DUMMY_ATTACKER_USER_ID, Role.ATTACKER)
                || !game.addPlayer(DUMMY_DEFENDER_USER_ID, Role.DEFENDER)) {
            messages.add(format("ERROR: Could not add system players to game T{0}.", stagedGame.getId()));
            return false;
        }
        if (gameSettings.isWithMutants() || gameSettings.isWithTests()) {
            gameManagingUtils.addPredefinedMutantsAndTests(game,
                    gameSettings.isWithMutants(), gameSettings.isWithTests());
        }

        /* Add users to the game. */
        if (gameSettings.getGameType() == MULTIPLAYER) {
            for (int userId : stagedGame.getAttackers()) {
                UserInfo user = userInfos.get(userId);
                if (user != null) {
                    addPlayerToExistingGame(game, user.getUser(), Role.ATTACKER);
                } else {
                    messages.add(format("ERROR: Cannot add user {0} to existing game {1} as {2}. User does not exist.",
                            userId, game.getId(), Role.ATTACKER.getFormattedString()));
                }
            }
            for (int userId : stagedGame.getDefenders()) {
                UserInfo user = userInfos.get(userId);
                if (user != null) {
                    addPlayerToExistingGame(game, user.getUser(), Role.DEFENDER);
                } else {
                    messages.add(format("ERROR: Cannot add user {0} to existing game {1} as {2}. User does not exist.",
                            userId, game.getId(), Role.DEFENDER.getFormattedString()));
                }
            }
        } else if (gameSettings.getGameType() == MELEE) {
            for (int userId : stagedGame.getDefenders()) {
                UserInfo user = userInfos.get(userId);
                if (user != null) {
                    addPlayerToExistingGame(game, user.getUser(), Role.PLAYER);
                } else {
                    messages.add(format("ERROR: Cannot add user {0} to existing game {1} as {2}. User does not exist.",
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

    public enum RoleAssignmentMethod {
        /**
         * Users are assigned roles randomly, trying to assign the correct number of attackers and defenders.
         */
        RANDOM,

        /**
         * Users are assigned the role opposite of the last role they played as.
         * Users, which played neither as attacker or defender are assigned roles randomly.
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
