package org.codedefenders.beans.admin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codedefenders.beans.admin.AdminCreateGamesBean.RoleAssignmentMethod;
import org.codedefenders.beans.admin.AdminCreateGamesBean.TeamAssignmentMethod;
import org.codedefenders.beans.admin.StagedGameList.GameSettings;
import org.codedefenders.beans.admin.StagedGameList.StagedGame;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.DatabaseConnection;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MeleeGameDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.User;
import org.codedefenders.model.UserInfo;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Sets;

import static org.codedefenders.beans.admin.StagedGameList.GameSettings.GameType.MELEE;
import static org.codedefenders.beans.admin.StagedGameList.GameSettings.GameType.MULTIPLAYER;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AdminCreateGamesBean.class,
        DatabaseConnection.class,
        AdminDAO.class,
        GameDAO.class,
        MultiplayerGameDAO.class,
        MeleeGameDAO.class,
        UserDAO.class
})
public class AdminCreateGamesBeanTest {
    private AdminCreateGamesBean adminCreateGamesBean;
    private StagedGameList stagedGameList;

    private HashMap<Integer, UserInfo> userInfos;

    @Before
    public void mockDatabase() {
        // PowerMockito.mockStatic(DatabaseConnection.class);
        // PowerMockito.when(DatabaseConnection.getConnection()).thenReturn(null);
        PowerMockito.mockStatic(AdminDAO.class);
        PowerMockito.mockStatic(GameDAO.class);
        PowerMockito.mockStatic(MultiplayerGameDAO.class);
        PowerMockito.mockStatic(MeleeGameDAO.class);
        PowerMockito.mockStatic(UserDAO.class);
        PowerMockito.mockStatic(AdminCreateGamesBean.class);
    }

    /*
     * Initialize AdminCreateGamesBean and its dependencies manually,
     * because I couldn't get WeldInitiator to work with PowerMockito.
     */
    @Before
    public void initializeBean() throws NoSuchFieldException, IllegalAccessException {
        adminCreateGamesBean = new AdminCreateGamesBean();
        stagedGameList = adminCreateGamesBean.getStagedGameList();

        /* Mock bean dependencies of AdminCreateGamesBean. */
        LoginBean loginBean = PowerMockito.mock(LoginBean.class);
        PowerMockito.when(loginBean.getUserId()).thenReturn(0);
        PowerMockito.when(loginBean.getUser()).thenReturn(new User(0, "creator", "", ""));

        MessagesBean messagesBean = PowerMockito.mock(MessagesBean.class);
        GameManagingUtils gameManagingUtils = PowerMockito.mock(GameManagingUtils.class);
        EventDAO eventDAO = PowerMockito.mock(EventDAO.class);

        /* Inject the mocked beans manually. */
        Field messagesField = AdminCreateGamesBean.class.getDeclaredField("messages");
        messagesField.setAccessible(true);
        messagesField.set(adminCreateGamesBean, messagesBean);

        Field loginField = AdminCreateGamesBean.class.getDeclaredField("login");
        loginField.setAccessible(true);
        loginField.set(adminCreateGamesBean, loginBean);

        Field gameManagingUtilsField = AdminCreateGamesBean.class.getDeclaredField("gameManagingUtils");
        gameManagingUtilsField.setAccessible(true);
        gameManagingUtilsField.set(adminCreateGamesBean, gameManagingUtils);

        Field eventDAOField = AdminCreateGamesBean.class.getDeclaredField("eventDAO");
        eventDAOField.setAccessible(true);
        eventDAOField.set(adminCreateGamesBean, eventDAO);
    }

    @Before
    public void initializeData() {
        userInfos = new HashMap<>();
        userInfos.put(1, new UserInfo(new User(1, "userA", "", "userA@email.com"), null, Role.ATTACKER, 1));
        userInfos.put(2, new UserInfo(new User(2, "userB", "", "userB@email.com"), null, Role.ATTACKER, 2));
        userInfos.put(3, new UserInfo(new User(3, "userC", "", "userC@email.com"), null, Role.ATTACKER, 3));
        userInfos.put(4, new UserInfo(new User(4, "userD", "", "userD@email.com"), null, Role.ATTACKER, 4));
        userInfos.put(5, new UserInfo(new User(5, "userE", "", "userE@email.com"), null, Role.PLAYER, 5));
        userInfos.put(6, new UserInfo(new User(6, "userF", "", "userF@email.com"), null, Role.PLAYER, 6));
        userInfos.put(7, new UserInfo(new User(7, "userG", "", "userG@email.com"), null, Role.DEFENDER, 7));
        userInfos.put(8, new UserInfo(new User(8, "userH", "", "userH@email.com"), null, Role.DEFENDER, 8));
    }

    private Set<UserInfo> userSet(int... ids) {
        Set<UserInfo> users = new HashSet<>();
        for (int i : ids) {
            users.add(userInfos.get(i));
        }
        return users;
    }

    @Test
    public void testGetStagedGamesList() {
        StagedGameList list1 = adminCreateGamesBean.getStagedGameList();
        StagedGameList list2 = adminCreateGamesBean.getStagedGameList();

        assertThat(list1, not(nullValue()));
        assertThat(list2, not(nullValue()));
        assertThat(list1, sameInstance(list2));
    }

    @Test
    public void testGetUserInfos() {
        PowerMockito.when(AdminDAO.getAllUsersInfo()).thenReturn(new ArrayList<>(userInfos.values()));

        adminCreateGamesBean.updateUserInfos();
        assertThat(adminCreateGamesBean.getUserInfos(), equalTo(userInfos));
    }

    @Test
    public void testStageGamesMultiplayer() {
        Set<UserInfo> users = new HashSet<>(this.userInfos.values());
        GameSettings gameSettings = new GameSettings();
        gameSettings.setGameType(MULTIPLAYER);
        RoleAssignmentMethod roleAssignmentMethod = RoleAssignmentMethod.RANDOM;
        TeamAssignmentMethod teamAssignmentMethod = TeamAssignmentMethod.RANDOM;
        int attackersPerGame = 2;
        int defendersPerGame = 2;

        adminCreateGamesBean.stageGames(users, gameSettings, roleAssignmentMethod, teamAssignmentMethod,
                attackersPerGame, defendersPerGame);

        assertThat(stagedGameList.getStagedGames().values(), hasSize(2));

        for (StagedGame stagedGame : stagedGameList.getStagedGames().values()) {
            assertThat(stagedGame.getAttackers(), hasSize(2));
            assertThat(stagedGame.getDefenders(), hasSize(2));
        }
    }

    @Test
    public void testStageGamesMelee() {
        Set<UserInfo> users = new HashSet<>(this.userInfos.values());
        GameSettings gameSettings = new GameSettings();
        gameSettings.setGameType(MELEE);
        RoleAssignmentMethod roleAssignmentMethod = RoleAssignmentMethod.RANDOM;
        TeamAssignmentMethod teamAssignmentMethod = TeamAssignmentMethod.RANDOM;
        int attackersPerGame = 2;
        int defendersPerGame = 2;

        adminCreateGamesBean.stageGames(users, gameSettings, roleAssignmentMethod, teamAssignmentMethod,
                attackersPerGame, defendersPerGame);

        assertThat(stagedGameList.getStagedGames().values(), hasSize(2));

        for (StagedGame stagedGame : stagedGameList.getStagedGames().values()) {
            assertThat(stagedGame.getPlayers(), hasSize(4));
        }
    }

    @Test
    public void testDeleteStagedGames() {
        StagedGame stagedGame1 = stagedGameList.addStagedGame(new GameSettings());
        StagedGame stagedGame2 = stagedGameList.addStagedGame(new GameSettings());
        StagedGame stagedGame3 = stagedGameList.addStagedGame(new GameSettings());
        StagedGame stagedGame4 = stagedGameList.addStagedGame(new GameSettings());

        List<StagedGame> stagedGames = new ArrayList<>();
        stagedGames.add(stagedGame2);
        stagedGames.add(stagedGame3);

        adminCreateGamesBean.deleteStagedGames(stagedGames);
        assertThat(stagedGameList.getStagedGames().values(), containsInAnyOrder(stagedGame1, stagedGame4));
    }

    @Test
    public void testCreateStagedGames() throws Exception {
        GameSettings gameSettings = GameSettings.getDefault();
        GameClass cut = PowerMockito.mock(GameClass.class);
        PowerMockito.when(cut.getId()).thenReturn(0);
        gameSettings.setCut(cut);

        StagedGame stagedGame1 = stagedGameList.addStagedGame(gameSettings);
        StagedGame stagedGame2 = stagedGameList.addStagedGame(gameSettings);
        StagedGame stagedGame3 = stagedGameList.addStagedGame(gameSettings);
        StagedGame stagedGame4 = stagedGameList.addStagedGame(gameSettings);

        List<StagedGame> stagedGames = new ArrayList<>();
        stagedGames.add(stagedGame2);
        stagedGames.add(stagedGame3);

        PowerMockito.when(MultiplayerGameDAO.storeMultiplayerGame(Matchers.any(MultiplayerGame.class))).thenReturn(0);
        PowerMockito.when(GameDAO.addPlayerToGame(Matchers.anyInt(), Matchers.anyInt(), Matchers.any(Role.class))).thenReturn(true);
        PowerMockito.when(UserDAO.getUserById(Matchers.anyInt())).thenReturn(new User(""));
        adminCreateGamesBean = PowerMockito.spy(adminCreateGamesBean);

        adminCreateGamesBean.createStagedGames(stagedGames);

        assertThat(stagedGameList.getStagedGames().values(), containsInAnyOrder(stagedGame1, stagedGame4));
        PowerMockito.verifyPrivate(adminCreateGamesBean).invoke("insertStagedGame", stagedGame2);
        PowerMockito.verifyPrivate(adminCreateGamesBean).invoke("insertStagedGame", stagedGame3);
    }

    @Test
    public void testRemovePlayerFromStagedGame() {
        StagedGame stagedGame1 = stagedGameList.addStagedGame(new GameSettings());
        StagedGame stagedGame2 = stagedGameList.addStagedGame(new GameSettings());

        stagedGame1.addAttacker(1);
        stagedGame1.addDefender(2);
        stagedGame2.addAttacker(3);
        stagedGame2.addDefender(4);

        assertThat(adminCreateGamesBean.removePlayerFromStagedGame(stagedGame1, 0), is(false));
        assertThat(adminCreateGamesBean.removePlayerFromStagedGame(stagedGame1, 3), is(false));
        assertThat(adminCreateGamesBean.removePlayerFromStagedGame(stagedGame1, 1), is(true));
        assertThat(stagedGame1.getAttackers(), empty());
    }

    @Test
    public void testAddPlayerToStagedGame() {
        StagedGame stagedGame = stagedGameList.addStagedGame(new GameSettings());

        assertThat(adminCreateGamesBean.addPlayerToStagedGame(stagedGame, userInfos.get(1).getUser(), Role.OBSERVER), is(false));
        assertThat(adminCreateGamesBean.addPlayerToStagedGame(stagedGame, userInfos.get(1).getUser(), Role.NONE), is(false));

        assertThat(adminCreateGamesBean.addPlayerToStagedGame(stagedGame, userInfos.get(1).getUser(), Role.ATTACKER), is(true));
        assertThat(stagedGame.getAttackers(), containsInAnyOrder(1));
        assertThat(adminCreateGamesBean.addPlayerToStagedGame(stagedGame, userInfos.get(1).getUser(), Role.DEFENDER), is(false));
        assertThat(adminCreateGamesBean.addPlayerToStagedGame(stagedGame, userInfos.get(1).getUser(), Role.PLAYER), is(false));

        assertThat(adminCreateGamesBean.addPlayerToStagedGame(stagedGame, userInfos.get(2).getUser(), Role.DEFENDER), is(true));
        assertThat(stagedGame.getDefenders(), containsInAnyOrder(2));
        assertThat(adminCreateGamesBean.addPlayerToStagedGame(stagedGame, userInfos.get(2).getUser(), Role.ATTACKER), is(false));
        assertThat(adminCreateGamesBean.addPlayerToStagedGame(stagedGame, userInfos.get(2).getUser(), Role.PLAYER), is(false));

        assertThat(adminCreateGamesBean.addPlayerToStagedGame(stagedGame, userInfos.get(3).getUser(), Role.PLAYER), is(true));
        assertThat(stagedGame.getPlayers(), containsInAnyOrder(1, 2, 3));
        assertThat(adminCreateGamesBean.addPlayerToStagedGame(stagedGame, userInfos.get(3).getUser(), Role.ATTACKER), is(false));
        assertThat(adminCreateGamesBean.addPlayerToStagedGame(stagedGame, userInfos.get(3).getUser(), Role.DEFENDER), is(false));
    }

    @Test
    public void testAddPlayerToExistingGame() {
        AbstractGame game = PowerMockito.mock(AbstractGame.class);

        Mockito.when(game.addPlayer(Matchers.eq(1), Matchers.any(Role.class))).thenReturn(false);
        Mockito.when(game.addPlayer(Matchers.eq(2), Matchers.eq(Role.DEFENDER))).thenReturn(true);
        Mockito.when(game.addPlayer(Matchers.eq(3), Matchers.eq(Role.PLAYER))).thenReturn(true);

        assertThat(adminCreateGamesBean.addPlayerToExistingGame(game, userInfos.get(1).getUser(), Role.ATTACKER), is(false));
        assertThat(adminCreateGamesBean.addPlayerToExistingGame(game, userInfos.get(2).getUser(), Role.DEFENDER), is(true));
        assertThat(adminCreateGamesBean.addPlayerToExistingGame(game, userInfos.get(3).getUser(), Role.PLAYER), is(true));

        Mockito.verify(game).addPlayer(1, Role.ATTACKER);
        Mockito.verify(game).addPlayer(2, Role.DEFENDER);
        Mockito.verify(game).addPlayer(3, Role.PLAYER);
    }

    @Test
    public void testGetUserInfosForIds() {
        PowerMockito.when(AdminDAO.getAllUsersInfo()).thenReturn(new ArrayList<>(userInfos.values()));
        adminCreateGamesBean.updateUserInfos();

        Set<Integer> validSet = Stream.of(1, 2, 3).collect(Collectors.toSet());
        Set<Integer> inValidSet = Stream.of(0, 1, 2).collect(Collectors.toSet());

        Optional<Set<UserInfo>> validResult = adminCreateGamesBean.getUserInfosForIds(validSet);
        Optional<Set<UserInfo>> inValidResult = adminCreateGamesBean.getUserInfosForIds(inValidSet);

        assertThat(validResult.isPresent(), is(true));
        assertThat(validResult.get(), containsInAnyOrder(userInfos.get(1), userInfos.get(2), userInfos.get(3)));
        assertThat(inValidResult.isPresent(), is(false));
    }

    @Test
    public void testGetUserInfosForNamesAndEmails() {
        PowerMockito.when(AdminDAO.getAllUsersInfo()).thenReturn(new ArrayList<>(userInfos.values()));
        adminCreateGamesBean.updateUserInfos();

        Set<String> validSet1 = Stream.of("userA", "userB", "userC@email.com").collect(Collectors.toSet());
        Set<String> validSet2 = Stream.of("userA", "userA@email.com", "userB").collect(Collectors.toSet());
        Set<String> inValidSet = Stream.of("userA", "X", "").collect(Collectors.toSet());

        Optional<Set<UserInfo>> validResult1 = adminCreateGamesBean.getUserInfosForNamesAndEmails(validSet1);
        Optional<Set<UserInfo>> validResult2 = adminCreateGamesBean.getUserInfosForNamesAndEmails(validSet2);
        Optional<Set<UserInfo>> inValidResult = adminCreateGamesBean.getUserInfosForNamesAndEmails(inValidSet);

        assertThat(validResult1.isPresent(), is(true));
        assertThat(validResult1.get(), containsInAnyOrder(userInfos.get(1), userInfos.get(2), userInfos.get(3)));
        assertThat(validResult2.isPresent(), is(true));
        assertThat(validResult2.get(), containsInAnyOrder(userInfos.get(1), userInfos.get(2)));
        assertThat(inValidResult.isPresent(), is(false));
    }

    private void testAssignRoles(RoleAssignmentMethod roleAssignmentMethod,
                                 Set<UserInfo> users, Set<UserInfo> attackers, Set<UserInfo> defenders,
                                 int attackersPerGame, int defendersPerGame,
                                 int expectedNumAttackers, int expectedNumDefenders,
                                 Set<UserInfo> expectedAttackers, Set<UserInfo> expectedDefenders) {
        int numUsers = users.size() + attackers.size() + defenders.size();
        Set<UserInfo> usersBefore = new HashSet<>(users);
        Set<UserInfo> attackersBefore = new HashSet<>(attackers);
        Set<UserInfo> defendersBefore = new HashSet<>(defenders);

        adminCreateGamesBean.assignRoles(users, roleAssignmentMethod, attackersPerGame, defendersPerGame,
                attackers, defenders);

        assertThat(attackers, hasSize(expectedNumAttackers));
        assertThat(defenders, hasSize(expectedNumDefenders));
        assertThat(Sets.union(Sets.union(attackers, defenders), users), hasSize(numUsers));

        assertThat(users, equalTo(usersBefore));
        assertThat(attackers.containsAll(attackersBefore), is(true));
        assertThat(defenders.containsAll(defendersBefore), is(true));

        assertThat(attackers.containsAll(expectedAttackers), is(true));
        assertThat(defenders.containsAll(expectedDefenders), is(true));
    }

    private void testAssignRoles_Random(Set<UserInfo> users, Set<UserInfo> attackers, Set<UserInfo> defenders,
                                        int attackersPerGame, int defendersPerGame,
                                        int expectedNumAttackers, int expectedNumDefenders) {
        testAssignRoles(RoleAssignmentMethod.RANDOM, users, attackers, defenders, attackersPerGame, defendersPerGame,
                expectedNumAttackers, expectedNumDefenders, new HashSet<>(), new HashSet<>());
    }

    private void testAssignRoles_Opposite(Set<UserInfo> users, Set<UserInfo> attackers, Set<UserInfo> defenders,
                                        int attackersPerGame, int defendersPerGame,
                                        int expectedNumAttackers, int expectedNumDefenders,
                                        Set<UserInfo> expectedAttackers, Set<UserInfo> expectedDefenders) {
        testAssignRoles(RoleAssignmentMethod.OPPOSITE, users, attackers, defenders, attackersPerGame,defendersPerGame,
            expectedNumAttackers, expectedNumDefenders, expectedAttackers, expectedDefenders);
    }

    @Test
    public void testAssignRoles_Random_WithoutAlreadyAssignedUsers() {
        /* Even teams, no remaining users. */
        testAssignRoles_Random(
                userSet(1, 2, 3, 4, 5, 6, 7, 8),
                userSet(),
                userSet(),
                1, 1,
                4, 4);

        /* Even teams, no remaining users. */
        testAssignRoles_Random(
                userSet(1, 2, 3, 4, 5, 6, 7, 8),
                userSet(),
                userSet(),
                2, 2,
                4, 4);

        /* Even teams, remaining users. */
        testAssignRoles_Random(
                userSet(1, 2, 3, 4, 5, 6, 7, 8),
                userSet(),
                userSet(),
                3, 3,
                4, 4);

        /* Even teams, too large team sizes. */
        testAssignRoles_Random(
                userSet(1, 2, 3, 4, 5, 6, 7, 8),
                userSet(),
                userSet(),
                10, 10,
                4, 4);

        /* Uneven teams, no remaining users. */
        testAssignRoles_Random(
                userSet(1, 2, 3, 4, 5, 6, 7, 8),
                userSet(),
                userSet(),
                1, 3,
                2, 6);

        /* Uneven teams, too large team sizes. */
        testAssignRoles_Random(
                userSet(1, 2, 3, 4, 5, 6, 7, 8),
                userSet(),
                userSet(),
                3, 9,
                2, 6);

        /* Uneven teams, remaining users. */
        testAssignRoles_Random(
                userSet(1, 2, 3, 4, 5, 6, 7, 8),
                userSet(),
                userSet(),
                1, 2,
                3, 5);

        /* Uneven teams, remaining users. */
        testAssignRoles_Random(
                userSet(1, 2, 3, 4, 5, 6, 7, 8),
                userSet(),
                userSet(),
                2, 1,
                5, 3);

        /* One team size 0. */
        testAssignRoles_Random(
                userSet(1, 2, 3, 4, 5, 6, 7, 8),
                userSet(),
                userSet(),
                0, 1,
                0, 8);

        /* One team size 0. */
        testAssignRoles_Random(
                userSet(1, 2, 3, 4, 5, 6, 7, 8),
                userSet(),
                userSet(),
                1, 0,
                8, 0);

        /* One team size 0, too large other team. */
        testAssignRoles_Random(
                userSet(1, 2, 3, 4, 5, 6, 7, 8),
                userSet(),
                userSet(),
                10, 0,
                8, 0);

        /* Uneven number of users, even teams. */
        testAssignRoles_Random(
                userSet(1, 2, 3, 4, 5, 6, 7),
                userSet(),
                userSet(),
                1, 1,
                4, 3);

        /* Uneven number of users, uneven teams, remaining users. */
        testAssignRoles_Random(
                userSet(1, 2, 3, 4, 5, 6, 7),
                userSet(),
                userSet(),
                1, 2,
                2, 5);

        /* Uneven number of users, uneven teams, no remaining users. */
        testAssignRoles_Random(
                userSet(1, 2, 3, 4, 5, 6, 7),
                userSet(),
                userSet(),
                3, 4,
                3, 4);
    }

    @Test
    public void testAssignRoles_Random_WithAlreadyAssignedUsers() {
        /* Even teams, expected team distribution possible. */
        testAssignRoles_Random(
                userSet(1, 2, 3, 4),
                userSet(5, 6),
                userSet(7, 8),
                1, 1,
                4, 4);

        /* Uneven teams, expected team distribution possible. */
        testAssignRoles_Random(
                userSet(1, 2, 3, 4),
                userSet(5, 6),
                userSet(7, 8),
                1, 2,
                3, 5);

        /* Even teams, expected team distribution not possible. */
        testAssignRoles_Random(
                userSet(1, 2, 3),
                userSet(4, 5, 6, 7, 8),
                userSet(),
                1, 1,
                5, 3);

        /* Even teams, expected team distribution not possible. */
        testAssignRoles_Random(
                userSet(1, 2),
                userSet(),
                userSet(3, 4, 5, 6, 7, 8),
                1, 1,
                2, 6);

        /* Uneven teams, expected team distribution not possible. */
        testAssignRoles_Random(
                userSet(1, 2, 3),
                userSet(4, 5, 6),
                userSet(7, 8),
                1, 3,
                3, 5);

        /* Uneven number of users, uneven teams, expected team distribution not possible. */
        testAssignRoles_Random(
                userSet(1, 2),
                userSet(4, 5, 6),
                userSet(7, 8),
                1, 3,
                3, 4);

        /* All users already assigned. */
        testAssignRoles_Random(
                userSet(),
                userSet(1, 2, 3, 4),
                userSet(5, 6, 7, 8),
                1, 3,
                4, 4);
    }

    @Test
    public void testAssignRoles_Opposite() {
        /*
         * UserId | Prev. Role
         * -------+-----------
         * 1      | ATTACKER
         * 2      | ATTACKER
         * 3      | ATTACKER
         * 4      | ATTACKER
         * 5      | PLAYER
         * 6      | PLAYER
         * 7      | DEFENDER
         * 8      | DEFENDER
         */

        /* Even teams, no remaining users, all roles. */
        testAssignRoles_Opposite(
                userSet(1, 2, 3, 4, 5, 6, 7, 8),
                userSet(),
                userSet(),
                1, 1,
                4, 4,
                userSet(5, 6, 7, 8),
                userSet(1, 2, 3, 4));

        /* Uneven teams, no remaining users, all roles. */
        testAssignRoles_Opposite(
                userSet(1, 2, 3, 4, 5, 6, 7, 8),
                userSet(),
                userSet(),
                3, 1,
                4, 4,
                userSet(5, 6, 7, 8),
                userSet(1, 2, 3, 4));

        /* Uneven teams, remaining users, all roles. */
        testAssignRoles_Opposite(
                userSet(1, 2, 3, 4, 5, 6, 7, 8),
                userSet(),
                userSet(),
                1, 2,
                3, 5,
                userSet(7, 8),
                userSet(1, 2, 3, 4));

        /* Even teams, no remaining users, no previous roles (PLAYER). */
        testAssignRoles_Opposite(
                userSet(5, 6),
                userSet(),
                userSet(),
                1, 1,
                1, 1,
                userSet(),
                userSet());

        /* Even teams, no remaining users, only attackers. */
        testAssignRoles_Opposite(
                userSet(1, 2, 3, 4),
                userSet(),
                userSet(),
                1, 1,
                0, 4,
                userSet(),
                userSet(1, 2, 3, 4));

        /* Even teams, no remaining users, only attackers, already assigned attackers. */
        testAssignRoles_Opposite(
                userSet(3, 4),
                userSet(1, 2),
                userSet(),
                1, 1,
                2, 2,
                userSet(1, 2),
                userSet(3, 4));

        /* All users already assigned. */
        testAssignRoles_Opposite(
                userSet(),
                userSet(1, 2, 3, 4),
                userSet(5, 6, 7, 8),
                0, 1,
                4, 4,
                userSet(),
                userSet());
    }

    @Test
    public void testSplitIntoTeams_Random() {
        /* Random, no remaining users. */
        List<List<UserInfo>> teams = adminCreateGamesBean.splitIntoTeams(userSet(1, 2, 3, 4, 5, 6, 7, 8), 2,
                TeamAssignmentMethod.RANDOM);
        assertThat(teams, hasSize(2));
        assertThat(teams.get(0), hasSize(4));
        assertThat(teams.get(1), hasSize(4));
        assertThat(teams.stream().flatMap(List::stream).collect(Collectors.toList()), hasSize(8));

        /* Random, with remaining users. */
        teams = adminCreateGamesBean.splitIntoTeams(userSet(1, 2, 3, 4, 5, 6, 7, 8), 3,
                TeamAssignmentMethod.RANDOM);
        assertThat(teams, hasSize(3));
        assertThat(teams.get(0), hasSize(3));
        assertThat(teams.get(1), hasSize(3));
        assertThat(teams.get(2), hasSize(2));
        assertThat(teams.stream().flatMap(List::stream).collect(Collectors.toList()), hasSize(8));
    }

    @Test
    public void testSplitIntoTeams_Score() {
        /*
         * UserId | Tot. Score
         * -------+-----------
         * 1      | 1
         * 2      | 2
         * 3      | 3
         * 4      | 4
         * 5      | 5
         * 6      | 6
         * 7      | 7
         * 8      | 8
         */

        /* Random, no remaining users. */
        List<List<UserInfo>> teams = adminCreateGamesBean.splitIntoTeams(userSet(1, 2, 3, 4, 5, 6, 7, 8), 2,
                TeamAssignmentMethod.SCORE_DESCENDING);
        assertThat(teams, hasSize(2));
        assertThat(teams.get(0), hasSize(4));
        assertThat(teams.get(1), hasSize(4));
        assertThat(teams.stream().flatMap(List::stream).collect(Collectors.toList()), hasSize(8));
        assertThat(teams.get(0), containsInAnyOrder(userSet(8, 7, 6, 5).toArray()));
        assertThat(teams.get(1), containsInAnyOrder(userSet(4, 3, 2, 1).toArray()));

        /* Random, with remaining users. */
        teams = adminCreateGamesBean.splitIntoTeams(userSet(1, 2, 3, 4, 5, 6, 7, 8), 3,
                TeamAssignmentMethod.SCORE_DESCENDING);
        assertThat(teams, hasSize(3));
        assertThat(teams.get(0), hasSize(3));
        assertThat(teams.get(1), hasSize(3));
        assertThat(teams.get(2), hasSize(2));
        assertThat(teams.stream().flatMap(List::stream).collect(Collectors.toList()), hasSize(8));
        assertThat(teams.get(0), containsInAnyOrder(userSet(8, 7, 6).toArray()));
        assertThat(teams.get(1), containsInAnyOrder(userSet(5, 4, 3).toArray()));
        assertThat(teams.get(2), containsInAnyOrder(userSet(2, 1).toArray()));
    }
}
