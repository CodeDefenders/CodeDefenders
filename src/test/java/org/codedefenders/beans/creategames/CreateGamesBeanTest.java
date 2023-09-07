package org.codedefenders.beans.creategames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.EventDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameType;
import org.codedefenders.game.Role;
import org.codedefenders.model.creategames.GameSettings;
import org.codedefenders.model.creategames.StagedGameList;
import org.codedefenders.model.creategames.gameassignment.GameAssignmentStrategy;
import org.codedefenders.model.creategames.roleassignment.RoleAssignmentStrategy;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.CreateGamesService;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.codedefenders.game.GameType.MELEE;
import static org.codedefenders.game.GameType.MULTIPLAYER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class CreateGamesBeanTest {
    private final GameSettings defaultSettings = new GameSettings(GameType.MULTIPLAYER, -1, false, false, 3,
            CodeValidatorLevel.MODERATE, true, false, 0, GameLevel.HARD, Role.OBSERVER, 60, false, null);

    private AdminCreateGamesBean createGamesBean;
    private StagedGameList stagedGameList;

    private HashMap<Integer, CreateGamesBean.UserInfo> userInfos;

    private UserRepository userRepo;
    private CreateGamesService createGamesService;


    @BeforeEach
    public void init() {
        /* Create data. */
        userInfos = new HashMap<>();
        userInfos.put(1, new CreateGamesBean.UserInfo(1, "userA", "userA@email.com", null, Role.ATTACKER, 1));
        userInfos.put(2, new CreateGamesBean.UserInfo(2, "userB", "userB@email.com", null, Role.ATTACKER, 2));
        userInfos.put(3, new CreateGamesBean.UserInfo(3, "userC", "userC@email.com", null, Role.ATTACKER, 3));
        userInfos.put(4, new CreateGamesBean.UserInfo(4, "userD", "userD@email.com", null, Role.ATTACKER, 4));
        userInfos.put(5, new CreateGamesBean.UserInfo(5, "userE", "userE@email.com", null, Role.PLAYER, 5));
        userInfos.put(6, new CreateGamesBean.UserInfo(6, "userF", "userF@email.com", null, Role.PLAYER, 6));
        userInfos.put(7, new CreateGamesBean.UserInfo(7, "userG", "userG@email.com", null, Role.DEFENDER, 7));
        userInfos.put(8, new CreateGamesBean.UserInfo(8, "userH", "userH@email.com", null, Role.DEFENDER, 8));

        /* Mock bean dependencies of AdminCreateGamesBean. */
        MessagesBean messagesBean = Mockito.mock(MessagesBean.class);
        EventDAO eventDAO = Mockito.mock(EventDAO.class);
        userRepo = Mockito.mock(UserRepository.class);
        createGamesService = Mockito.mock(CreateGamesService.class);

        /* Initialize bean. */
        stagedGameList = new StagedGameList();
        createGamesBean = new AdminCreateGamesBean(
                stagedGameList,
                messagesBean,
                eventDAO,
                userRepo,
                createGamesService
        ) {
            @Override
            public Map<Integer, CreateGamesBean.UserInfo> fetchUserInfos() {
               return userInfos;
            }

            @Override
            public Set<Integer> fetchAvailableMultiplayerGames() {
                return new HashSet<>();
            }

            @Override
            public Set<Integer> fetchAvailableMeleeGames() {
                return new HashSet<>();
            }

            @Override
            public Set<Integer> fetchAssignedUsers() {
                return new HashSet<>();
            }
        };
    }

    @Test
    public void testGetStagedGamesList() {
        StagedGameList list1 = createGamesBean.getStagedGames();
        StagedGameList list2 = createGamesBean.getStagedGames();

        assertThat(list1, not(nullValue()));
        assertThat(list2, not(nullValue()));
        assertThat(list1, sameInstance(list2));
    }

    @Test
    public void testGetUserInfos() {
        assertThat(createGamesBean.getUserInfos(), equalTo(userInfos));
    }

    @Test
    public void testStageGamesMultiplayer() {
        Set<Integer> userIds = new HashSet<>(this.userInfos.keySet());
        GameSettings gameSettings = GameSettings.from(defaultSettings)
                .setGameType(MULTIPLAYER)
                .build();
        RoleAssignmentStrategy.Type roleAssignmentMethod = RoleAssignmentStrategy.Type.RANDOM;
        GameAssignmentStrategy.Type gameAssignmentMethod = GameAssignmentStrategy.Type.RANDOM;
        int attackersPerGame = 2;
        int defendersPerGame = 2;

        createGamesBean.stageGamesWithUsers(userIds, gameSettings, roleAssignmentMethod, gameAssignmentMethod,
                attackersPerGame, defendersPerGame);

        assertThat(stagedGameList.getMap().values(), hasSize(2));

        for (StagedGameList.StagedGame stagedGame : stagedGameList.getMap().values()) {
            assertThat(stagedGame.getAttackers(), hasSize(2));
            assertThat(stagedGame.getDefenders(), hasSize(2));
        }
    }

    @Test
    public void testStageGamesMelee() {
        Set<Integer> userIds = new HashSet<>(this.userInfos.keySet());
        GameSettings gameSettings = GameSettings.from(defaultSettings)
                .setGameType(MELEE)
                .build();
        RoleAssignmentStrategy.Type roleAssignmentMethod = RoleAssignmentStrategy.Type.RANDOM;
        GameAssignmentStrategy.Type gameAssignmentMethod = GameAssignmentStrategy.Type.RANDOM;
        int playersPerGame = 4;

        createGamesBean.stageGamesWithUsers(userIds, gameSettings, roleAssignmentMethod, gameAssignmentMethod,
                playersPerGame, 0);

        assertThat(stagedGameList.getMap().values(), hasSize(2));

        for (StagedGameList.StagedGame stagedGame : stagedGameList.getMap().values()) {
            assertThat(stagedGame.getPlayers(), hasSize(4));
        }
    }

    @Test
    public void testStageGamesNotEnoughPlayersForOneGame() {
        Set<Integer> userIds = new HashSet<>() {{
            add(1);
            add(2);
        }};
        GameSettings gameSettings = GameSettings.from(defaultSettings)
                .setGameType(MULTIPLAYER)
                .build();
        RoleAssignmentStrategy.Type roleAssignmentMethod = RoleAssignmentStrategy.Type.RANDOM;
        GameAssignmentStrategy.Type gameAssignmentMethod = GameAssignmentStrategy.Type.RANDOM;
        int attackersPerGame = 2;
        int defendersPerGame = 2;

        createGamesBean.stageGamesWithUsers(userIds, gameSettings, roleAssignmentMethod, gameAssignmentMethod,
                attackersPerGame, defendersPerGame);

        assertThat(stagedGameList.getMap().values(), hasSize(1));

        for (StagedGameList.StagedGame stagedGame : stagedGameList.getMap().values()) {
            assertThat(stagedGame.getAttackers(), hasSize(1));
            assertThat(stagedGame.getDefenders(), hasSize(1));
        }
    }

    @Test
    public void testDeleteStagedGames() {
        StagedGameList.StagedGame stagedGame1 = stagedGameList.addStagedGame(defaultSettings);
        StagedGameList.StagedGame stagedGame2 = stagedGameList.addStagedGame(defaultSettings);
        StagedGameList.StagedGame stagedGame3 = stagedGameList.addStagedGame(defaultSettings);
        StagedGameList.StagedGame stagedGame4 = stagedGameList.addStagedGame(defaultSettings);

        List<StagedGameList.StagedGame> stagedGames = new ArrayList<>();
        stagedGames.add(stagedGame2);
        stagedGames.add(stagedGame3);

        createGamesBean.deleteStagedGames(stagedGames);
        assertThat(stagedGameList.getMap().values(), containsInAnyOrder(stagedGame1, stagedGame4));
    }

    @Test
    public void testCreateStagedGames() throws Exception {
        Mockito.when(createGamesService.createGame(any())).thenReturn(true);

        GameSettings gameSettings = GameSettings.from(defaultSettings)
                .setClassId(0)
                .build();

        StagedGameList.StagedGame stagedGame1 = stagedGameList.addStagedGame(gameSettings);
        StagedGameList.StagedGame stagedGame2 = stagedGameList.addStagedGame(gameSettings);
        StagedGameList.StagedGame stagedGame3 = stagedGameList.addStagedGame(gameSettings);
        StagedGameList.StagedGame stagedGame4 = stagedGameList.addStagedGame(gameSettings);

        List<StagedGameList.StagedGame> stagedGames = new ArrayList<>();
        stagedGames.add(stagedGame2);
        stagedGames.add(stagedGame3);

        createGamesBean.createStagedGames(stagedGames);

        assertThat(stagedGameList.getMap().values(), containsInAnyOrder(stagedGame1, stagedGame4));
        Mockito.verify(createGamesService).createGame(stagedGame2);
        Mockito.verify(createGamesService).createGame(stagedGame3);
    }

    @Test
    public void testRemovePlayerFromStagedGame() {
        StagedGameList.StagedGame stagedGame1 = stagedGameList.addStagedGame(defaultSettings);
        StagedGameList.StagedGame stagedGame2 = stagedGameList.addStagedGame(defaultSettings);

        stagedGame1.addAttacker(1);
        stagedGame1.addDefender(2);
        stagedGame2.addAttacker(3);
        stagedGame2.addDefender(4);

        assertThat(createGamesBean.removePlayerFromStagedGame(stagedGame1, 0), is(false));
        assertThat(createGamesBean.removePlayerFromStagedGame(stagedGame1, 3), is(false));
        assertThat(createGamesBean.removePlayerFromStagedGame(stagedGame1, 1), is(true));
        assertThat(stagedGame1.getAttackers(), empty());
    }

    @Test
    public void testAddPlayerToStagedGame() {
        StagedGameList.StagedGame stagedGame = stagedGameList.addStagedGame(defaultSettings);

        assertThat(createGamesBean.addPlayerToStagedGame(stagedGame, 1, Role.OBSERVER), is(false));
        assertThat(createGamesBean.addPlayerToStagedGame(stagedGame, 1, Role.NONE), is(false));

        assertThat(createGamesBean.addPlayerToStagedGame(stagedGame, 1, Role.ATTACKER), is(true));
        assertThat(stagedGame.getAttackers(), containsInAnyOrder(1));
        assertThat(createGamesBean.addPlayerToStagedGame(stagedGame, 1, Role.DEFENDER), is(false));
        assertThat(createGamesBean.addPlayerToStagedGame(stagedGame, 1, Role.PLAYER), is(false));

        assertThat(createGamesBean.addPlayerToStagedGame(stagedGame, 2, Role.DEFENDER), is(true));
        assertThat(stagedGame.getDefenders(), containsInAnyOrder(2));
        assertThat(createGamesBean.addPlayerToStagedGame(stagedGame, 2, Role.ATTACKER), is(false));
        assertThat(createGamesBean.addPlayerToStagedGame(stagedGame, 2, Role.PLAYER), is(false));

        assertThat(createGamesBean.addPlayerToStagedGame(stagedGame, 3, Role.PLAYER), is(true));
        assertThat(stagedGame.getPlayers(), containsInAnyOrder(1, 2, 3));
        assertThat(createGamesBean.addPlayerToStagedGame(stagedGame, 3, Role.ATTACKER), is(false));
        assertThat(createGamesBean.addPlayerToStagedGame(stagedGame, 3, Role.DEFENDER), is(false));
    }

    @Test
    public void testAddPlayerToExistingGame() {
        AbstractGame game = Mockito.mock(AbstractGame.class);

        Mockito.when(game.addPlayer(eq(1), any(Role.class))).thenReturn(false);
        Mockito.when(game.addPlayer(eq(2), eq(Role.DEFENDER))).thenReturn(true);
        Mockito.when(game.addPlayer(eq(3), eq(Role.PLAYER))).thenReturn(true);

        assertThat(createGamesBean.addPlayerToExistingGame(game, 1, Role.ATTACKER), is(false));
        assertThat(createGamesBean.addPlayerToExistingGame(game, 2, Role.DEFENDER), is(true));
        assertThat(createGamesBean.addPlayerToExistingGame(game, 3, Role.PLAYER), is(true));

        Mockito.verify(game).addPlayer(1, Role.ATTACKER);
        Mockito.verify(game).addPlayer(2, Role.DEFENDER);
        Mockito.verify(game).addPlayer(3, Role.PLAYER);
    }

    @Test
    public void testGetUserInfosForIds() {
        Set<Integer> validSet = Stream.of(1, 2, 3).collect(Collectors.toSet());
        Set<Integer> inValidSet = Stream.of(0, 1, 2).collect(Collectors.toSet());

        Optional<Set<CreateGamesBean.UserInfo>> validResult = createGamesBean.getUserInfosForIds(validSet);
        Optional<Set<CreateGamesBean.UserInfo>> inValidResult = createGamesBean.getUserInfosForIds(inValidSet);

        assertThat(validResult.isPresent(), is(true));
        assertThat(validResult.get(), containsInAnyOrder(userInfos.get(1), userInfos.get(2), userInfos.get(3)));
        assertThat(inValidResult.isPresent(), is(false));
    }

    @Test
    public void testGetUserInfosForNamesAndEmails() {
        Set<String> validSet1 = Stream.of("userA", "userB", "userC@email.com").collect(Collectors.toSet());
        Set<String> validSet2 = Stream.of("userA", "userA@email.com", "userB").collect(Collectors.toSet());
        Set<String> inValidSet = Stream.of("userA", "X", "").collect(Collectors.toSet());

        Optional<Set<CreateGamesBean.UserInfo>> validResult1 = createGamesBean.getUserInfosForNamesAndEmails(validSet1);
        Optional<Set<CreateGamesBean.UserInfo>> validResult2 = createGamesBean.getUserInfosForNamesAndEmails(validSet2);
        Optional<Set<CreateGamesBean.UserInfo>> inValidResult = createGamesBean.getUserInfosForNamesAndEmails(inValidSet);

        assertThat(validResult1.isPresent(), is(true));
        assertThat(validResult1.get(), containsInAnyOrder(userInfos.get(1), userInfos.get(2), userInfos.get(3)));
        assertThat(validResult2.isPresent(), is(true));
        assertThat(validResult2.get(), containsInAnyOrder(userInfos.get(1), userInfos.get(2)));
        assertThat(inValidResult.isPresent(), is(false));
    }
}
