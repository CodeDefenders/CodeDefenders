/*
 * Copyright (C) 2021 Code Defenders contributors
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

package org.codedefenders.itests;

import java.sql.Connection;
import java.sql.Timestamp;

import org.codedefenders.database.DatabaseConnection;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Mutant.Equivalence;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * @author Jose Rojas
 */
@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({DatabaseConnection.class, CodeValidator.class})
public class DatabaseTest {
    private static long START_TIME = (int) 1e15;
    private static long END_TIME = (int) 1e30;

    @Before // BeforeClass has to be static...
    public void createEntities() {
        user1 = new UserEntity("FREE_USERNAME", UserEntity.encodePassword("TEST_PASSWORD"), "TESTMAIL@TEST.TEST");
        user2 = new UserEntity("FREE_USERNAME2", UserEntity.encodePassword("TEST_PASSWORD2"), "TESTMAIL@TEST.TEST2");

        creator = new UserEntity(20000, "FREE_USERNAME3", UserEntity.encodePassword("TEST_PASSWORD3"), "TESTMAIL@TEST.TEST3");

        cut1 = GameClass.build()
                .id(22345678)
                .name("MyClass")
                .alias("")
                .javaFile("")
                .classFile("")
                .create();

        cut1 = GameClass.build()
                .id(34865)
                .name("")
                .alias("AliasForClass2")
                .javaFile("")
                .classFile("")
                .create();

        multiplayerGame = new MultiplayerGame
                .Builder(cut1.getId(), creator.getId(), 5)
                .level(GameLevel.EASY)
                .defenderValue(10)
                .attackerValue(4)
                .mutantValidatorLevel(CodeValidatorLevel.MODERATE)
                .chatEnabled(true)
                .build();
    }

    // This will re-create the same DB from scratch every time... is this really
    // necessary ?! THIS IS NOT ACTUALLY THE CASE. I SUSPECT THAT THE RULE CREATES ONLY ONCE THE DB
    // TODO(Alex): Migrate to /src/integration/ and convert to proper Database Test
    //@Rule
    //public DatabaseRule db = new DatabaseRule();

    @Before
    public void mockDBConnections() throws Exception {
        PowerMockito.mockStatic(DatabaseConnection.class);
        PowerMockito.when(DatabaseConnection.getConnection()).thenAnswer((Answer<Connection>) invocation -> {
            // Return a new connection from the rule instead
            return null; //db.getConnection();
        });
    }

    private UserEntity creator;
    private UserEntity user1;
    private UserEntity user2;

    private MultiplayerGame multiplayerGame;

    private GameClass cut1;
    private GameClass cut2;
    private Mutant mutant1;
    private org.codedefenders.game.Test test;

    //FIXME
    @Ignore
    @Test
    public void testInsertClasses() throws Exception {
        assertEquals(0, GameClassDAO.getAllPlayableClasses().size());

        assertTrue("Should have inserted class", cut1.insert());
        assertEquals(1, GameClassDAO.getAllPlayableClasses().size());

        assertTrue("Should have inserted class", cut2.insert());
        assertEquals(2, GameClassDAO.getAllPlayableClasses().size());
        //PowerMockito.verifyStatic();
    }

    //FIXME
    @Ignore
    @Test
    public void testInsertGame() throws Exception {
        assumeTrue(creator.insert());
        assumeTrue(user1.insert());
        assumeTrue(cut1.insert());

        Whitebox.setInternalState(multiplayerGame, "classId", cut1.getId());
        // multiplayerGame.classId = cut1.getId();

        assertTrue(multiplayerGame.insert());

        MultiplayerGame multiplayerGameFromDB = MultiplayerGameDAO.getMultiplayerGame(multiplayerGame.getId());
        assertEquals(multiplayerGameFromDB.getPrize(), multiplayerGame.getPrize(), 1e-10);
        assertEquals(multiplayerGameFromDB.getMaxAssertionsPerTest(), multiplayerGame.getMaxAssertionsPerTest());
        assertEquals(multiplayerGameFromDB.isChatEnabled(), multiplayerGame.isChatEnabled());
        assertEquals(multiplayerGameFromDB.getMutantValidatorLevel(), multiplayerGame.getMutantValidatorLevel());
    }

    //FIXME
    @Ignore
    @Test
    public void testGameLists() {
        assumeTrue(creator.insert());
        assumeTrue(user1.insert());
        assumeTrue(user2.insert());
        assumeTrue(cut1.insert());

        MultiplayerGame mg2 = new MultiplayerGame
                .Builder(cut1.getId(), creator.getId(), 2)
                .state(GameState.ACTIVE)
                .level(GameLevel.EASY)
                .defenderValue(10)
                .attackerValue(4)
                .mutantValidatorLevel(CodeValidatorLevel.MODERATE)
                .chatEnabled(true)
                .build();
        assumeTrue(mg2.insert());
        assumeTrue(mg2.addPlayer(user1.getId(), Role.DEFENDER));
        assertTrue(mg2.update());

        MultiplayerGame mg3 = new MultiplayerGame
                .Builder(cut1.getId(), creator.getId(), 2)
                .state(GameState.ACTIVE)
                .level(GameLevel.EASY)
                .defenderValue(10)
                .attackerValue(4)
                .mutantValidatorLevel(CodeValidatorLevel.MODERATE)
                .chatEnabled(true)
                .build();
        assumeTrue(mg3.insert());

        assumeTrue(mg3.addPlayer(user1.getId(), Role.DEFENDER));
        assumeTrue(mg3.addPlayer(user2.getId(), Role.ATTACKER));
        assumeTrue(mg3.update());

        MultiplayerGame mg4 = new MultiplayerGame
                .Builder(cut1.getId(), creator.getId(), 2)
                .level(GameLevel.EASY)
                .defenderValue(10)
                .attackerValue(4)
                .mutantValidatorLevel(CodeValidatorLevel.MODERATE)
                .chatEnabled(true)
                .build();
        assertEquals(1, MultiplayerGameDAO.getActiveMultiplayerGamesWithInfoForUser(user2.getId()).size());
        assertEquals(2, MultiplayerGameDAO.getActiveMultiplayerGamesWithInfoForUser(user1.getId()).size());
        assertEquals(2, MultiplayerGameDAO.getJoinedMultiplayerGamesForUser(user1.getId()).size());
        assertEquals(0, MultiplayerGameDAO.getFinishedMultiplayerGamesForUser(user1.getId()).size());

        assumeTrue(mg4.insert());
        assumeTrue(mg4.addPlayer(user1.getId(), Role.DEFENDER));
        mg4.setState(GameState.FINISHED);
        assertTrue(mg4.update());

        assertEquals(1, MultiplayerGameDAO.getFinishedMultiplayerGamesForUser(user1.getId()).size());
    }

    //FIXME
    @Ignore
    @Test
    public void testInsertPlayer() throws Exception {
        UserRepository userRepo = null; //new UserRepository(db.getQueryRunner(), mock(MetricsRegistry.class));

        assumeTrue(creator.insert());
        assumeTrue(user1.insert());
        assumeTrue(cut1.insert());

        Whitebox.setInternalState(multiplayerGame, "classId", cut1.getId());
        // multiplayerGame.classId = cut1.getId();

        assertTrue(multiplayerGame.insert());
        assertTrue(multiplayerGame.addPlayer(user1.getId(), Role.DEFENDER));
        int playerId = PlayerDAO.getPlayerIdForUserAndGame(user1.getId(), multiplayerGame.getId());
        assertTrue(playerId > 0);
        assertEquals(userRepo.getUserIdForPlayerId(playerId).get().intValue(), user1.getId());
        assertTrue(GameDAO.getPlayersForGame(multiplayerGame.getId(), Role.DEFENDER).size() > 0);
        assertEquals(PlayerDAO.getPlayerPoints(playerId), 0);
        PlayerDAO.increasePlayerPoints(13, playerId);
        assertEquals(PlayerDAO.getPlayerPoints(playerId), 13);
    }

    //FIXME
    @Ignore
    @Test
    public void testInsertMutant() throws Exception {
        PowerMockito.mockStatic(CodeValidator.class);
        PowerMockito.when(CodeValidator.getMD5FromFile("TEST_J_FILE1")).thenReturn("MD5_1");
        PowerMockito.when(CodeValidator.getMD5FromFile("TEST_J_FILE2")).thenReturn("MD5_2");

        assumeTrue(creator.insert());
        assumeTrue(user1.insert());
        assumeTrue(cut2.insert());

        Whitebox.setInternalState(multiplayerGame, "classId", cut2.getId());
        // multiplayerGame.classId = cut2.getId();

        assertTrue(multiplayerGame.insert());
        assertTrue(multiplayerGame.addPlayer(user1.getId(), Role.ATTACKER));

        int gid = multiplayerGame.getId();
        int pid = PlayerDAO.getPlayerIdForUserAndGame(user1.getId(), gid);
        int cutId = cut2.getId();
        mutant1 = new Mutant(99, cutId, multiplayerGame.getId(), "TEST_J_FILE1", "TEST_C_FILE1", true,
                Mutant.Equivalence.ASSUMED_NO, 1, 99, pid);
        Mutant mutant2 = new Mutant(100, cutId, gid, "TEST_J_FILE2", "TEST_C_FILE2", false,
                Mutant.Equivalence.ASSUMED_YES, 2, 2, pid);
        assertTrue(mutant1.insert());
        assertTrue(mutant2.insert());
        Mutant[] ml = {mutant1, mutant2};
        assertArrayEquals(MutantDAO.getValidMutantsForPlayer(pid).toArray(), ml);
        assertArrayEquals(MutantDAO.getValidMutantsForGame(gid).toArray(), ml);
    }

    //FIXME
    @Ignore
    @Test
    public void testDoubleUpdateMutant() throws Exception {
        PowerMockito.mockStatic(CodeValidator.class);
        PowerMockito.when(CodeValidator.getMD5FromFile("TEST_J_FILE1")).thenReturn("MD5_1");
        PowerMockito.when(CodeValidator.getMD5FromFile("TEST_J_FILE2")).thenReturn("MD5_2");

        assumeTrue(creator.insert());
        assumeTrue(user1.insert());
        assumeTrue(cut2.insert());

        Whitebox.setInternalState(multiplayerGame, "classId", cut2.getId());
        // multiplayerGame.classId = cut2.getId();

        assertTrue(multiplayerGame.insert());
        assertTrue(multiplayerGame.addPlayer(user1.getId(), Role.ATTACKER));

        int pid = PlayerDAO.getPlayerIdForUserAndGame(user1.getId(), multiplayerGame.getId());
        int cutId = cut2.getId();
        Mutant mutant1 = new Mutant(99, cutId, multiplayerGame.getId(), "TEST_J_FILE1", "TEST_C_FILE1", true,
                Mutant.Equivalence.ASSUMED_NO, 1, 99, pid);

        assertTrue(mutant1.insert());

        assertTrue(mutant1.kill(Equivalence.ASSUMED_NO));
        //
        assertFalse(mutant1.kill(Equivalence.ASSUMED_NO));
        assertFalse(mutant1.kill(Equivalence.ASSUMED_NO));
    }

    //FIXME
    @Ignore
    @Test
    public void testCannotUpdateKilledMutant() throws Exception {
        PowerMockito.mockStatic(CodeValidator.class);
        PowerMockito.when(CodeValidator.getMD5FromFile("TEST_J_FILE1")).thenReturn("MD5_1");
        PowerMockito.when(CodeValidator.getMD5FromFile("TEST_J_FILE2")).thenReturn("MD5_2");

        assumeTrue(creator.insert());
        assumeTrue(user1.insert());
        assumeTrue(cut2.insert());


        // Creator must be there already
        multiplayerGame = new MultiplayerGame
                .Builder(cut1.getId(), creator.getId(), 5)
                .level(GameLevel.EASY)
                .defenderValue(10)
                .attackerValue(4)
                .mutantValidatorLevel(CodeValidatorLevel.MODERATE)
                .chatEnabled(true)
                .build();
        // Why this ?
        Whitebox.setInternalState(multiplayerGame, "classId", cut2.getId());
        // multiplayerGame.classId = cut2.getId();

        assertTrue(multiplayerGame.insert());
        assertTrue(multiplayerGame.addPlayer(user1.getId(), Role.ATTACKER));

        int pid = PlayerDAO.getPlayerIdForUserAndGame(user1.getId(), multiplayerGame.getId());
        int cutId = cut2.getId();
        Mutant mutant1 = new Mutant(99, cutId, multiplayerGame.getId(), "TEST_J_FILE1", "TEST_C_FILE1", true,
                Mutant.Equivalence.ASSUMED_NO, 1, 99, pid);

        assertTrue(mutant1.insert());
        // Kill the mutant
        assertTrue(mutant1.kill(Equivalence.ASSUMED_NO));
        int score = mutant1.getScore();
        // Prevent score update
        mutant1.setScore(10);
        assertFalse(mutant1.update());
        //

        Mutant storedMutant = MutantDAO.getMutantById(mutant1.getId());
        assertEquals("Score does not match", score, storedMutant.getScore());
        //
        assertEquals(mutant1, storedMutant);
    }

    //FIXME
    @Ignore
    @Test
    public void testInsertTest() throws Exception {
        assumeTrue(creator.insert());
        assumeTrue(user1.insert());
        assumeTrue(cut1.insert());

        Whitebox.setInternalState(multiplayerGame, "classId", cut1.getId());
        // multiplayerGame.classId = cut1.getId();

        assertTrue(multiplayerGame.insert());
        assertTrue(multiplayerGame.addPlayer(user1.getId(), Role.DEFENDER));

        int pid = PlayerDAO.getPlayerIdForUserAndGame(user1.getId(), multiplayerGame.getId());
        test = new org.codedefenders.game.Test(99, cut1.getId(), multiplayerGame.getId(), "TEST_J_FILE", "TEST_C_FILE", 1, 10, pid);
        test.setPlayerId(pid);

        assertTrue(test.insert());
        org.codedefenders.game.Test testFromDB = TestDAO.getTestById(test.getId());
        assertEquals(testFromDB.getJavaFile(), test.getJavaFile());
        assertEquals(testFromDB.getClassFile(), test.getClassFile());
        assertEquals(testFromDB.getGameId(), test.getGameId());
        assertEquals(testFromDB.getPlayerId(), test.getPlayerId());

        LineCoverage lc = new LineCoverage();
        test.setLineCoverage(lc);
        test.setScore(17);
        assertTrue(test.update());

        testFromDB = TestDAO.getTestById(test.getId());
        assertEquals(testFromDB.getScore(), test.getScore());
        assertEquals(testFromDB.getLineCoverage().getLinesCovered(), test.getLineCoverage().getLinesCovered());
        assertEquals(testFromDB.getLineCoverage().getLinesUncovered(), test.getLineCoverage().getLinesUncovered());
    }

    @Ignore
    @Test
    public void testEquivalences() throws Exception {

        testInsertMutant();

        assumeTrue(user2.insert());
        assumeTrue(multiplayerGame.addPlayer(user2.getId(), Role.DEFENDER));

        int pid = PlayerDAO.getPlayerIdForUserAndGame(user2.getId(), multiplayerGame.getId());
        assertTrue(MutantDAO.insertEquivalence(mutant1, pid));
        assertEquals(MutantDAO.getEquivalentDefenderId(mutant1), pid);
    }

    //FIXME
    @Ignore
    @Test
    public void testTargetExecutions() throws Exception {
        PowerMockito.mockStatic(CodeValidator.class);
        PowerMockito.when(CodeValidator.getMD5FromFile("TEST_J_FILE1")).thenReturn("MD5_1");

        assumeTrue(creator.insert());
        assumeTrue(user1.insert());
        assumeTrue(user2.insert());
        assumeTrue(cut1.insert());
        Whitebox.setInternalState(multiplayerGame, "classId", cut1.getId());
        // multiplayerGame.classId = cut1.getId();
        assumeTrue(multiplayerGame.insert());
        assumeTrue(multiplayerGame.addPlayer(user1.getId(), Role.DEFENDER));
        assertTrue(multiplayerGame.addPlayer(user2.getId(), Role.ATTACKER));

        //
        int pidDefender = PlayerDAO.getPlayerIdForUserAndGame(user1.getId(), multiplayerGame.getId());
        test = new org.codedefenders.game.Test(99, cut1.getId(), multiplayerGame.getId(), "TEST_J_FILE", "TEST_C_FILE", 1, 10,
                pidDefender);
        test.setPlayerId(pidDefender);
        assumeTrue(test.insert());
        LineCoverage lc = new LineCoverage();
        test.setLineCoverage(lc);
        test.setScore(17);
        assumeTrue(test.update());

        //
        int pidAttacker = PlayerDAO.getPlayerIdForUserAndGame(user1.getId(), multiplayerGame.getId());
        int cutId = cut1.getId();

        Mutant mutant1 = new Mutant(999, cutId, multiplayerGame.getId(), "TEST_J_FILE1", "TEST_C_FILE1", true,
                Mutant.Equivalence.ASSUMED_NO, 1, 99, pidAttacker);
        assertTrue(mutant1.insert());

        // Leave the ID in the constructor
        TargetExecution te = new TargetExecution(1, test.getId(), mutant1.getId(),
                TargetExecution.Target.TEST_EQUIVALENCE, TargetExecution.Status.SUCCESS, "msg", Timestamp.valueOf("1995-03-27 12:08:00"));
        assertTrue(te.insert());
        TargetExecution teFromDB = TargetExecutionDAO.getTargetExecutionForPair(test.getId(), mutant1.getId());
        assertEquals(te.message, teFromDB.message);
        assertEquals(te.status, teFromDB.status);
        assertEquals(te.target, teFromDB.target);
        assertEquals(te.mutantId, teFromDB.mutantId);
        assertEquals(te.testId, teFromDB.testId);
    }

    // TODO Fix this by injecting the dependency in test. See
    //  https://github.com/weld/weld-junit/blob/master/junit4/README.md
    @Ignore
    @Test
    public void testEvents() throws Exception { // TODO figure out why table events does not have foreign keys

        testInsertPlayer();
        int pid = PlayerDAO.getPlayerIdForUserAndGame(user2.getId(), multiplayerGame.getId());
        Timestamp ts = Timestamp.valueOf("1995-03-27 12:08:00");
        // TODO: Events don't take PlayerIDs but UserIDs!!
        Event ev = new Event(1, multiplayerGame.getId(), pid, "message", EventType.ATTACKER_MESSAGE, EventStatus.GAME,
                ts);

        // TODO Fix me with CDI
        EventDAO eventDAO = null;
        assertTrue(eventDAO.insert(ev));

        assertEquals(eventDAO.getEventsForGame(multiplayerGame.getId()).size(), 2);
        assertEquals(eventDAO.getNewEventsForGame(multiplayerGame.getId(), 0, Role.DEFENDER).size(), 1);
        assertEquals(eventDAO.getNewEventsForGame(multiplayerGame.getId(), 0, Role.ATTACKER).size(), 2);
        assertEquals(eventDAO.getNewEventsForGame(multiplayerGame.getId(), (int) 1E20, Role.ATTACKER).size(), 0);
    }
}
