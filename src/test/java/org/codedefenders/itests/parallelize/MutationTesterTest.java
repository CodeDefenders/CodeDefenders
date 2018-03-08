package org.codedefenders.itests.parallelize;

import org.codedefenders.*;
import org.codedefenders.exceptions.CodeValidatorException;
import org.codedefenders.itests.IntegrationTest;
import org.codedefenders.multiplayer.MultiplayerGame;
import org.codedefenders.rules.DatabaseRule;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.util.DatabaseConnection;
import org.junit.*;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.*;
import javax.naming.spi.InitialContextFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * This test shall evaluate how Mutation Tester handle concurrent updates.
 * Problematic cases: - More than 1 tests kill the same mutant - Mutant state is
 * reset from killed to alive while setting the score
 * 
 * The problem lies in the fact that MutationTester set the state of the mutant
 * and forces it to the database, while the state of the mutants should be
 * handled by the database instead !
 * 
 * @author gambi
 *
 */

@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DatabaseConnection.class }) // , MutationTester.class })
public class MutationTesterTest {

	// PowerMock does not work with @ClassRule !!
	// This really should be only per class, not per test... in each test we can
	// truncate the tables ?
	@Rule
//	public DatabaseRule db = new DatabaseRule("defender", "db/emptydb.sql", "useAffectedRows=true");
	public DatabaseRule db = new DatabaseRule("defender", "db/emptydb.sql");

	//
	private static File codedefendersHome;

	// PROBLEM: @ClassRule cannot be used with PowerMock ...
	@BeforeClass
	public static void setupEnvironment() throws IOException {
		// ERROR > WARN > INFO > DEBUG > TRACE.
		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
		System.setProperty(org.slf4j.impl.SimpleLogger.LOG_FILE_KEY, "System.err");
		//

		codedefendersHome = Files.createTempDirectory("integration-tests").toFile();
		// TODO Will this remove all the files ?
		codedefendersHome.deleteOnExit();
	}

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

	// CUT
	private MutationTester tester;

	// We use the "real ant runner" but we need to provide a mock to Context
	@Mock
	private InitialContextFactory mockedFactory;

	// https://stackoverflow.com/questions/36734275/how-to-mock-initialcontext-constructor-in-unit-testing
	// FIXME this has hardcoded values, not sure how to handle those... maybe
	// read from the config.properties file ?
	public static class MyContextFactory implements InitialContextFactory {
		@Override
		public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
			InitialContext mockedInitialContext = PowerMockito.mock(InitialContext.class);
			NamingEnumeration<NameClassPair> mockedEnumeration = PowerMockito.mock(NamingEnumeration.class);
			// Look at this again ...
			PowerMockito.mockStatic(NamingEnumeration.class);
			//
			PowerMockito.when(mockedEnumeration.hasMore()).thenReturn(true, true, true, true, false);
			PowerMockito.when(mockedEnumeration.next()).thenReturn(
					new NameClassPair("data.dir", String.class.getName()),
					new NameClassPair("parallelize", String.class.getName()),
					new NameClassPair("mutant.coverage", String.class.getName()),
					new NameClassPair("ant.home", String.class.getName())//
			);
			//
			PowerMockito.when(mockedInitialContext.toString()).thenReturn("Mocked Initial Context");
			PowerMockito.when(mockedInitialContext.list("java:/comp/env")).thenReturn(mockedEnumeration);
			//
			Context mockedEnvironmentContext = PowerMockito.mock(Context.class);
			PowerMockito.when(mockedInitialContext.lookup("java:/comp/env")).thenReturn(mockedEnvironmentContext);

			PowerMockito.when(mockedEnvironmentContext.lookup("mutant.coverage")).thenReturn("enabled");
			// FIXMED
			PowerMockito.when(mockedEnvironmentContext.lookup("parallelize")).thenReturn("enabled");
			//
			PowerMockito.when(mockedEnvironmentContext.lookup("data.dir"))
					.thenReturn(codedefendersHome.getAbsolutePath());

			PowerMockito.when(mockedEnvironmentContext.lookup("ant.home")).thenReturn("/usr/local");
			//
			return mockedInitialContext;
		}
	}

	@Before
	public void setupClass() throws IOException {
		// Initialize this as mock class
		MockitoAnnotations.initMocks(this);
		// Be sure to setup the "java.naming.factory.initial" to the inner
		// MyContextFactory class
		System.setProperty("java.naming.factory.initial", this.getClass().getCanonicalName() + "$MyContextFactory");
		//
		// Recreate codedefenders' folders
		boolean isCreated = false;
		isCreated = (new File(Constants.MUTANTS_DIR)).mkdirs() || (new File(Constants.MUTANTS_DIR)).exists();
		// System.out.println("ParallelizeAntRunnerTest.setupClass() " +
		// isCreated);
		isCreated = (new File(Constants.CUTS_DIR)).mkdirs() || (new File(Constants.CUTS_DIR)).exists();
		// System.out.println("ParallelizeAntRunnerTest.setupClass() " +
		// isCreated);
		isCreated = (new File(Constants.TESTS_DIR)).mkdirs() || (new File(Constants.TESTS_DIR)).exists();
		// System.out.println("ParallelizeAntRunnerTest.setupClass() " +
		// isCreated);
		//
		// Setup the environment
		Files.createSymbolicLink(new File(Constants.DATA_DIR, "build.xml").toPath(),
				Paths.get(new File("src/test/resources/itests/build.xml").getAbsolutePath()));

		Files.createSymbolicLink(new File(Constants.DATA_DIR, "security.policy").toPath(),
				Paths.get(new File("src/test/resources/itests/relaxed.security.policy").getAbsolutePath()));

		Files.createSymbolicLink(new File(Constants.DATA_DIR, "lib").toPath(),
				Paths.get(new File("src/test/resources/itests/lib").getAbsolutePath()));

	}

	@After
	public void deleteTemporaryFiles() {
		// TODO ?
	}

	private static Runnable defend(MultiplayerGame activeGame, String testFile, User defender,
			ArrayList<String> messages) {
		return new Runnable() {

			@Override
			public void run() {
				try {
					// Compile and test original
					String testText;
					testText = new String(Files.readAllBytes(new File(testFile).toPath()), Charset.defaultCharset());
					org.codedefenders.Test newTest = GameManager.createTest(activeGame.getId(), activeGame.getClassId(),
							testText, defender.getId(), "mp");

					System.out.println(new Date() + " MutationTesterTest.defend() " + defender.getId() + " with "
							+ newTest.getId());
					MutationTester.runTestOnAllMultiplayerMutants(activeGame, newTest, messages);
					activeGame.update();
					System.out.println(new Date() + " MutationTesterTest.defend() " + defender.getId() + ": "
							+ messages.get(messages.size() - 1));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CodeValidatorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
	}

	private static Runnable attack(MultiplayerGame activeGame, String mutantFile, User attacker,
			ArrayList<String> messages) throws IOException {
		return new Runnable() {

			@Override
			public void run() {
				try {
					String mutantText = new String(Files.readAllBytes(new File(mutantFile).toPath()),
							Charset.defaultCharset());
					Mutant mutant = GameManager.createMutant(activeGame.getId(), activeGame.getClassId(), mutantText,
							attacker.getId(), "mp");
					System.out.println(new Date() + " MutationTesterTest.attack() " + attacker.getId() + " with "
							+ mutant.getId());
					MutationTester.runAllTestsOnMutant(activeGame, mutant, messages);
					activeGame.update();
					System.out.println(new Date() + " MutationTesterTest.attack() " + attacker.getId() + ": "
							+ messages.get(messages.size() - 1));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		};
	}

	@Test
	public void testRunAllTestsOnMutant() throws IOException, CodeValidatorException, InterruptedException {

		// Create the users, game, and such
		User observer = new User("observer", "password", "demo@observer.com");
		observer.insert();

		User[] attackers = new User[3];
		attackers[0] = new User("demoattacker", "password", "demo@attacker.com");
		attackers[0].insert();
		attackers[1] = new User("demoattacker1", "password", "demo1@attacker.com");
		attackers[1].insert();
		attackers[2] = new User("demoattacker2", "password", "demo2@attacker.com");
		attackers[2].insert();

		//
		User[] defenders = new User[3];
		defenders[0] = new User("demodefender", "password", "demo@defender.com");
		defenders[0].insert();
		defenders[1] = new User("demodefender1", "password", "demo1@defender.com");
		defenders[1].insert();
		defenders[2] = new User("demodefender2", "password", "demo2@defender.com");
		defenders[2].insert();

		// Taken from Game 232

		// // Upload the Class Under test - Maybe better use Classloader
		File cutFolder = new File(Constants.CUTS_DIR, "IntHashMap");
		cutFolder.mkdirs();
		File jFile = new File(cutFolder, "IntHashMap.java");
		File cFile = new File(cutFolder, "IntHashMap.class");
		File c1File = new File(cutFolder, "IntHashMap$Entry.class");
		//
		Files.copy(Paths.get("src/test/resources/replay/game232/IntHashMap.java"), new FileOutputStream(jFile));
		Files.copy(Paths.get("src/test/resources/replay/game232/IntHashMap.clazz"), new FileOutputStream(cFile));
		Files.copy(Paths.get("src/test/resources/replay/game232/IntHashMap$Entry.clazz"), new FileOutputStream(c1File));

		GameClass cut = new GameClass("IntHashMap", "IntHashMap", jFile.getAbsolutePath(), cFile.getAbsolutePath());
		cut.insert();
		// System.out.println("ParallelizeAntRunnerTest.testRunAllTestsOnMutant()
		// Cut " + cut.getId());
		//
		MultiplayerGame multiplayerGame = new MultiplayerGame(cut.getId(), observer.getId(), GameLevel.HARD, (float) 1,
				(float) 1, (float) 1, 10, 4, 4, 4, 0, 0, (int) 1e5, (int) 1E30, GameState.ACTIVE.name(), false, 2, true, null, false);
		multiplayerGame.insert();
		//
		// // Attacker and Defender must join the game. Those calls update also
		// the
		// // db
		multiplayerGame.addPlayer(defenders[0].getId(), Role.DEFENDER);
		multiplayerGame.addPlayer(defenders[1].getId(), Role.DEFENDER);
		multiplayerGame.addPlayer(defenders[2].getId(), Role.DEFENDER);
		//
		multiplayerGame.addPlayer(attackers[0].getId(), Role.ATTACKER);
		multiplayerGame.addPlayer(attackers[1].getId(), Role.ATTACKER);
		multiplayerGame.addPlayer(attackers[2].getId(), Role.ATTACKER);

		// System.out.println(" Game " + multiplayerGame.getId());

		MultiplayerGame activeGame = DatabaseAccess.getMultiplayerGame(multiplayerGame.getId());
		assertEquals("Cannot find the right active game", multiplayerGame.getId(), activeGame.getId());

		// Use the trace for Game232 - Synchronosous for the moment

		ScheduledExecutorService scheduledExecutors = Executors.newScheduledThreadPool(6);

		// 1/101) Defend defenders[0]using
		// src/test/resources/replay/game232/test.7851
		final ArrayList<String> messages = new ArrayList<>();

		long initialDelay = 1000;
		int speedup = 10;

		scheduledExecutors.schedule(
				defend(activeGame, "src/test/resources/replay/game232/test.7851", defenders[0], messages),
				(0 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 2/101) Defend defenders[1]using
		// src/test/resources/replay/game232/test.7853
		scheduledExecutors.schedule(
				defend(activeGame, "src/test/resources/replay/game232/test.7853", defenders[1], messages),
				(45000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 3/101) Attack attackers[0]using
		// src/test/resources/replay/game232/mutant.12924
		scheduledExecutors.schedule(
				attack(activeGame, "src/test/resources/replay/game232/mutant.12924", attackers[0], messages),
				(46000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 4/101) Attack attackers[1]using
		// src/test/resources/replay/game232/mutant.12925
		scheduledExecutors.schedule(
				attack(activeGame, "src/test/resources/replay/game232/mutant.12925", attackers[1], messages),
				(50000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 5/101) Defend defenders[0]using
		// src/test/resources/replay/game232/test.7854
		scheduledExecutors.schedule(
				defend(activeGame, "src/test/resources/replay/game232/test.7854", defenders[0], messages),
				(58000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 6/101) Attack attackers[2] using
		// src/test/resources/replay/game232/mutant.12933
		scheduledExecutors.schedule(
				attack(activeGame, "src/test/resources/replay/game232/mutant.12933", attackers[2], messages),
				(66000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 7/101) Defend defenders[1]using
		// src/test/resources/replay/game232/test.7859
		scheduledExecutors.schedule(
				defend(activeGame, "src/test/resources/replay/game232/test.7859", defenders[1], messages),
				(82000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 8/101) Attack attackers[1]using
		// src/test/resources/replay/game232/mutant.12958
		scheduledExecutors.schedule(
				attack(activeGame, "src/test/resources/replay/game232/mutant.12958", attackers[1], messages),
				(119000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 9/101) Defend defenders[0]using
		// src/test/resources/replay/game232/test.7866
		scheduledExecutors.schedule(
				defend(activeGame, "src/test/resources/replay/game232/test.7866", defenders[0], messages),
				(157000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 10/101) Defend defenders[2]using
		// src/test/resources/replay/game232/test.7869
		scheduledExecutors.schedule(
				defend(activeGame, "src/test/resources/replay/game232/test.7869", defenders[2], messages),
				(192000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 11/19) Attack attackers[1] using
		// src/test/resources/replay/game232/mutant.12987
		scheduledExecutors.schedule(
				attack(activeGame, "src/test/resources/replay/game232/mutant.12987", attackers[1], messages),
				(198000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 12/19) Attack attackers[0] using
		// src/test/resources/replay/game232/mutant.12989
		scheduledExecutors.schedule(
				attack(activeGame, "src/test/resources/replay/game232/mutant.12989", attackers[0], messages),
				(202000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 13/19) Attack attackers[1] using
		// src/test/resources/replay/game232/mutant.12995
		scheduledExecutors.schedule(
				attack(activeGame, "src/test/resources/replay/game232/mutant.12995", attackers[1], messages),
				(214000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 14/19) Attack attackers[0] using
		// src/test/resources/replay/game232/mutant.12999
		scheduledExecutors.schedule(
				attack(activeGame, "src/test/resources/replay/game232/mutant.12999", attackers[0], messages),
				(231000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 15/19) Defend defenders[0] using
		// src/test/resources/replay/game232/test.7872
		scheduledExecutors.schedule(
				defend(activeGame, "src/test/resources/replay/game232/test.7872", defenders[0], messages),
				(231000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 16/19) Attack attackers[1] using
		// src/test/resources/replay/game232/mutant.13007
		scheduledExecutors.schedule(
				attack(activeGame, "src/test/resources/replay/game232/mutant.13007", attackers[1], messages),
				(246000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 17/19) Attack attackers[1] using
		// src/test/resources/replay/game232/mutant.13014
		scheduledExecutors.schedule(
				attack(activeGame, "src/test/resources/replay/game232/mutant.13014", attackers[1], messages),
				(266000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 18/19) Attack attackers[0] using
		// src/test/resources/replay/game232/mutant.13022
		scheduledExecutors.schedule(
				attack(activeGame, "src/test/resources/replay/game232/mutant.13022", attackers[0], messages),
				(289000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		// 19/19) Attack attackers[1] using
		// src/test/resources/replay/game232/mutant.13025
		scheduledExecutors.schedule(
				attack(activeGame, "src/test/resources/replay/game232/mutant.13025", attackers[1], messages),
				(296000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

		scheduledExecutors.shutdown();
		scheduledExecutors.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		///
		System.out.println(
				"MutationTesterTest.testRunAllTestsOnMutant() Mutant score " + activeGame.getMutantScores().get(-1));
		System.out.println(
				"MutationTesterTest.testRunAllTestsOnMutant() Test score " + activeGame.getTestScores().get(-1));

	}

}
