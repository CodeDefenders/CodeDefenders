package org.codedefenders.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;

import org.codedefenders.GameClass;
import org.codedefenders.GameLevel;
import org.codedefenders.GameMode;
import org.codedefenders.GameState;
import org.codedefenders.Mutant;
import org.codedefenders.Role;
import org.codedefenders.TargetExecution;
import org.codedefenders.User;
import org.codedefenders.duel.DuelGame;
import org.codedefenders.events.Event;
import org.codedefenders.events.EventStatus;
import org.codedefenders.events.EventType;
import org.codedefenders.multiplayer.LineCoverage;
import org.codedefenders.multiplayer.MultiplayerGame;
import org.codedefenders.rules.DatabaseRule;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.util.DatabaseConnection;
import org.codedefenders.validation.CodeValidator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @author Jose Rojas
 */
@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DatabaseConnection.class, CodeValidator.class })
public class RunnerTest {

	@Before // BeforeClass has to be static...
	public void createEntities() {
		user1 = new User("FREE_USERNAME", "TEST_PASSWORD", "TESTMAIL@TEST.TEST");
		user2 = new User("FREE_USERNAME2", "TEST_PASSWORD2", "TESTMAIL@TEST.TEST2");
		//
		creator = new User(4, "FREE_USERNAME3", "TEST_PASSWORD3", "TESTMAIL@TEST.TEST3");
		//
		cut1 = new GameClass("MyClass", "", "", "");
		cut2 = new GameClass("", "AliasForClass2", "", "");
		multiplayerGame = new MultiplayerGame(cut1.getId(), creator.getId(), GameLevel.EASY, (float) 1, (float) 1,
				(float) 1, 10, 4, 4, 4, 0, 0, (int) 1e5, (int) 1E30, GameState.CREATED.name(), false);
	}

	// This will re-create the same DB from scratch every time... is this really
	// necessary ?!
	@Rule
	DatabaseRule db = new DatabaseRule("defender", "db/emptydb.sql");

	@Before
	public void mockDBConnections() throws Exception {
		PowerMockito.mockStatic(DatabaseConnection.class);
		PowerMockito.when(DatabaseConnection.getConnection()).thenAnswer(new Answer<Connection>() {
			public Connection answer(InvocationOnMock invocation) throws SQLException {
				// Return a new connection from the rule instead
				return db.getConnection();
			}
		});
	}

	private User creator;
	private User user1;
	private User user2;

	private MultiplayerGame multiplayerGame;

	private GameClass cut1;
	private GameClass cut2;
	private Mutant mutant1;
	private org.codedefenders.Test test;

	@Test
	public void testInsertUser() throws Exception {
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

		assertTrue(user1.insert());
		User userFromDB = DatabaseAccess.getUser(user1.getId());
		assertEquals(user1.getId(), userFromDB.getId());
		assertEquals(user1.getUsername(), userFromDB.getUsername());
		assertEquals(user1.getEmail(), userFromDB.getEmail());
		// FIXME this is never written to the DB
		// assertEquals(user1.isValidated(), userFromDB.isValidated());
		assertTrue(passwordEncoder.matches(user1.getPassword(), userFromDB.getPassword()));
		assertNotEquals("Password should not be stored in plain text", user1.getPassword(), userFromDB.getPassword());
		// FIXME Split this in two tests
		// assertFalse("Inserting a user twice should fail", user1.insert());
	}

	@Test
	public void testInsertClasses() throws Exception {
		assertEquals(0, DatabaseAccess.getAllClasses().size());

		assertTrue("Should have inserted class", cut1.insert());
		assertEquals(1, DatabaseAccess.getAllClasses().size());

		assertTrue("Should have inserted class", cut2.insert());
		assertEquals(2, DatabaseAccess.getAllClasses().size());
		PowerMockito.verifyStatic();
	}

	@Test
	public void testInsertGame() throws Exception {
		assumeTrue(creator.insert());
		assumeTrue(user1.insert());
		assumeTrue(cut1.insert());

		Whitebox.setInternalState(multiplayerGame, "classId", cut1.getId());
		// multiplayerGame.classId = cut1.getId();

		assertTrue(multiplayerGame.insert());

		MultiplayerGame multiplayerGameFromDB = DatabaseAccess.getMultiplayerGame(multiplayerGame.getId());
		assertEquals(multiplayerGameFromDB.getFinishDateTime(), multiplayerGame.getFinishDateTime());
		assertTrue(Arrays.equals(multiplayerGameFromDB.getAttackerIds(), multiplayerGame.getAttackerIds()));
		assertEquals(multiplayerGameFromDB.getPrize(), multiplayerGame.getPrize(), 1e-10);
		assertEquals(multiplayerGameFromDB.getAttackerLimit(), multiplayerGame.getAttackerLimit());
		assertEquals(multiplayerGameFromDB.getMinAttackers(), multiplayerGame.getMinAttackers());
		assertEquals(multiplayerGameFromDB.getMinDefenders(), multiplayerGame.getMinDefenders());
	}

	@Test
	public void testGameLists() throws Exception {
		assumeTrue(creator.insert());
		assumeTrue(user1.insert());
		assumeTrue(user2.insert());
		assumeTrue(cut1.insert());

		DuelGame dg1 = new DuelGame(cut1.getId(), user1.getId(), 100, Role.DEFENDER, GameLevel.EASY);
		assumeTrue(dg1.insert());

		MultiplayerGame mg2 = new MultiplayerGame(cut1.getId(), creator.getId(), GameLevel.EASY, (float) 1, (float) 1,
				(float) 1, 10, 4, 4, 4, 0, 0, (int) 1e5, (int) 1E30, GameState.ACTIVE.name(), false);
		assumeTrue(mg2.insert());
		assumeTrue(mg2.addPlayer(user1.getId(), Role.DEFENDER));
		assertTrue(mg2.update());
		
		MultiplayerGame mg3 = new MultiplayerGame(cut1.getId(), creator.getId(), GameLevel.EASY, (float) 1, (float) 1,
				(float) 1, 10, 4, 4, 4, 0, 0, (int) 1e5, (int) 1E30, GameState.ACTIVE.name(), false);
		assumeTrue(mg3.insert());
		
		assumeTrue(mg3.addPlayer(user1.getId(), Role.DEFENDER));
		assumeTrue(mg3.addPlayer(user2.getId(), Role.ATTACKER));
		assumeTrue(mg3.update());

		MultiplayerGame mg4 = new MultiplayerGame(cut1.getId(), creator.getId(), GameLevel.EASY, (float) 1, (float) 1,
				(float) 1, 10, 4, 4, 4, 0, 0, (int) 1e5, (int) 1E30, GameState.FINISHED.name(), false);
		assumeTrue(mg4.insert());
		
		// TODO Why is 0
		assertEquals(1, DatabaseAccess.getOpenGames().size());

		assertEquals(1, DatabaseAccess.getGamesForUser(user1.getId()).size());
		// User 1 participates in 2 MP games
		assertEquals(2, DatabaseAccess.getJoinedMultiplayerGamesForUser(user1.getId()).size());
		// This might return data based on timestamp ... Makes test tricky
		assertEquals(2, DatabaseAccess.getFinishedMultiplayerGamesForUser(user1.getId()).size());

		// FIXME:
		// User 2 participates in 1 MP game, but probably beacuse of timestamp
		// this is closed
//		assertEquals(0, DatabaseAccess.getOpenMultiplayerGamesForUser(user2.getId()).size());
		// TODO Not sure what's this and why it does not return 2...
//		assertEquals(0, DatabaseAccess.getMultiplayerGamesForUser(user1.getId()).size());

	}

	@Test
	public void testInsertPlayer() throws Exception {
		assumeTrue(creator.insert());
		assumeTrue(user1.insert());
		assumeTrue(cut1.insert());

		Whitebox.setInternalState(multiplayerGame, "classId", cut1.getId());
		// multiplayerGame.classId = cut1.getId();

		assertTrue(multiplayerGame.insert());
		assertTrue(multiplayerGame.addPlayer(user1.getId(), Role.DEFENDER));
		int playerID = DatabaseAccess.getPlayerIdForMultiplayerGame(user1.getId(), multiplayerGame.getId());
		assertTrue(playerID > 0);
		assertEquals(DatabaseAccess.getUserFromPlayer(playerID).getId(), user1.getId());
		assertTrue(DatabaseAccess.getPlayersForMultiplayerGame(multiplayerGame.getId(), Role.DEFENDER).length > 0);
		assertEquals(DatabaseAccess.getPlayerPoints(playerID), 0);
		DatabaseAccess.increasePlayerPoints(13, playerID);
		assertEquals(DatabaseAccess.getPlayerPoints(playerID), 13);
	}

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

		int pid = DatabaseAccess.getPlayerIdForMultiplayerGame(user1.getId(), multiplayerGame.getId());
		mutant1 = new Mutant(99, multiplayerGame.getId(), "TEST_J_FILE1", "TEST_C_FILE1", true,
				Mutant.Equivalence.ASSUMED_NO, 1, 99, pid);
		Mutant mutant2 = new Mutant(100, multiplayerGame.getId(), "TEST_J_FILE2", "TEST_C_FILE2", false,
				Mutant.Equivalence.ASSUMED_YES, 2, 2, pid);
		assertTrue(mutant1.insert());
		assertTrue(mutant2.insert());
		Mutant[] ml = { mutant1, mutant2 };
		assertTrue(Arrays.equals(DatabaseAccess.getMutantsForPlayer(pid).toArray(), ml));
		assertTrue(Arrays.equals(DatabaseAccess.getMutantsForGame(multiplayerGame.getId()).toArray(), ml));
	}

	@Test
	public void testInsertTest() throws Exception {
		assumeTrue(creator.insert());
		assumeTrue(user1.insert());
		assumeTrue(cut1.insert());

		Whitebox.setInternalState(multiplayerGame, "classId", cut1.getId());
		// multiplayerGame.classId = cut1.getId();

		assertTrue(multiplayerGame.insert());
		assertTrue(multiplayerGame.addPlayer(user1.getId(), Role.DEFENDER));

		int pid = DatabaseAccess.getPlayerIdForMultiplayerGame(user1.getId(), multiplayerGame.getId());
		test = new org.codedefenders.Test(99, multiplayerGame.getId(), "TEST_J_FILE", "TEST_C_FILE", 1, 10, pid);
		test.setPlayerId(pid);

		assertTrue(test.insert());
		org.codedefenders.Test testFromDB = DatabaseAccess.getTestForId(test.getId());
		assertEquals(testFromDB.getJavaFile(), test.getJavaFile());
		assertEquals(testFromDB.getClassFile(), test.getClassFile());
		assertEquals(testFromDB.getGameId(), test.getGameId());
		assertEquals(testFromDB.getPlayerId(), test.getPlayerId());

		LineCoverage lc = new LineCoverage();
		test.setLineCoverage(lc);
		test.setScore(17);
		test.setAiMutantsKilled(23);
		assertTrue(test.update());

		testFromDB = DatabaseAccess.getTestForId(test.getId());
		assertEquals(testFromDB.getScore(), test.getScore());
		assertEquals(testFromDB.getAiMutantsKilled(), test.getAiMutantsKilled());
		assertEquals(testFromDB.getLineCoverage().getLinesCovered(), test.getLineCoverage().getLinesCovered());
		assertEquals(testFromDB.getLineCoverage().getLinesUncovered(), test.getLineCoverage().getLinesUncovered());
	}

	@Test
	public void testEquivalences() throws Exception {

		testInsertMutant();

		assumeTrue(user2.insert());
		assumeTrue(multiplayerGame.addPlayer(user2.getId(), Role.DEFENDER));

		int pid = DatabaseAccess.getPlayerIdForMultiplayerGame(user2.getId(), multiplayerGame.getId());
		assertTrue(DatabaseAccess.insertEquivalence(mutant1, pid));
		assertEquals(DatabaseAccess.getEquivalentDefenderId(mutant1), pid);
	}

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
		int pidDefender = DatabaseAccess.getPlayerIdForMultiplayerGame(user1.getId(), multiplayerGame.getId());
		test = new org.codedefenders.Test(99, multiplayerGame.getId(), "TEST_J_FILE", "TEST_C_FILE", 1, 10,
				pidDefender);
		test.setPlayerId(pidDefender);
		assumeTrue(test.insert());
		LineCoverage lc = new LineCoverage();
		test.setLineCoverage(lc);
		test.setScore(17);
		test.setAiMutantsKilled(23);
		assumeTrue(test.update());

		//
		int pidAttacker = DatabaseAccess.getPlayerIdForMultiplayerGame(user1.getId(), multiplayerGame.getId());
		Mutant mutant1 = new Mutant(999, multiplayerGame.getId(), "TEST_J_FILE1", "TEST_C_FILE1", true,
				Mutant.Equivalence.ASSUMED_NO, 1, 99, pidAttacker);
		assertTrue(mutant1.insert());

		// Leave the ID in the constructor
		TargetExecution te = new TargetExecution(1, test.getId(), mutant1.getId(),
				TargetExecution.Target.TEST_EQUIVALENCE, "SUCCESS", "msg", "1995-03-27 12:08:00");
		assertTrue(te.insert());
		TargetExecution teFromDB = DatabaseAccess.getTargetExecutionForPair(test.getId(), mutant1.getId());
		assertEquals(te.message, teFromDB.message);
		assertEquals(te.status, teFromDB.status);
		assertEquals(te.target, teFromDB.target);
		assertEquals(te.mutantId, teFromDB.mutantId);
		assertEquals(te.testId, teFromDB.testId);
	}

	@Test
	public void testEvents() throws Exception { // TODO figure out why table
												// events does not have foreign
												// keys
		testInsertPlayer();
		int pid = DatabaseAccess.getPlayerIdForMultiplayerGame(user2.getId(), multiplayerGame.getId());
		Timestamp ts = Timestamp.valueOf("1995-03-27 12:08:00");
		Event ev = new Event(1, multiplayerGame.getId(), pid, "message", EventType.ATTACKER_MESSAGE, EventStatus.GAME,
				ts);
		assertTrue(ev.insert());
		assertEquals(DatabaseAccess.getEventsForGame(multiplayerGame.getId()).size(), 2);
		assertEquals(DatabaseAccess.getNewEventsForGame(multiplayerGame.getId(), 0, Role.DEFENDER).size(), 1);
		assertEquals(DatabaseAccess.getNewEventsForGame(multiplayerGame.getId(), 0, Role.ATTACKER).size(), 2);
		assertEquals(DatabaseAccess.getNewEventsForGame(multiplayerGame.getId(), (int) 1E20, Role.ATTACKER).size(), 0);
	}
}
