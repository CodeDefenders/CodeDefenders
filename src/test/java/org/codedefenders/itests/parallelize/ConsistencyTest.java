/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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
package org.codedefenders.itests.parallelize;

import org.codedefenders.database.DatabaseConnection;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.execution.MutationTester;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.itests.IntegrationTest;
import org.codedefenders.model.User;
import org.codedefenders.rules.DatabaseRule;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.validation.code.CodeValidatorException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DatabaseConnection.class }) // , MutationTester.class })
public class ConsistencyTest {

	// PowerMock does not work with @ClassRule !!
	// This really should be only per class, not per test... in each test we can
	// truncate the tables ?
	@Rule
	public DatabaseRule db = new DatabaseRule("defender", "db/emptydb.sql"); //, "useAffectedRows=true");

	//
	private static File codedefendersHome;

	// PROBLEM: @ClassRule cannot be used with PowerMock ...
	@BeforeClass
	public static void setupEnvironment() throws IOException {
		codedefendersHome = Files.createTempDirectory("integration-tests").toFile();
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
			System.out.println("ParallelizeAntRunnerTest.MyContextFactory.getInitialContext()");
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
		System.out.println("ParallelizeAntRunnerTest.setupClass() " + isCreated);
		isCreated = (new File(Constants.CUTS_DIR)).mkdirs() || (new File(Constants.CUTS_DIR)).exists();
		System.out.println("ParallelizeAntRunnerTest.setupClass() " + isCreated);
		isCreated = (new File(Constants.TESTS_DIR)).mkdirs() || (new File(Constants.TESTS_DIR)).exists();
		System.out.println("ParallelizeAntRunnerTest.setupClass() " + isCreated);
		//
		// Setup the environment
		Files.createSymbolicLink(new File(Constants.DATA_DIR, "build.xml").toPath(),
				Paths.get(new File("src/test/resources/itests/build.xml").getAbsolutePath()));

		Files.createSymbolicLink(new File(Constants.DATA_DIR, "security.policy").toPath(),
				Paths.get(new File("src/test/resources/itests/relaxed.security.policy").getAbsolutePath()));

		Files.createSymbolicLink(new File(Constants.DATA_DIR, "lib").toPath(),
				Paths.get(new File("src/test/resources/itests/lib").getAbsolutePath()));

	}

	/**
	 * Setup a game with an attacker and multiple defendes and check that a
	 * mutant can be killed only once and points are reported correctly.
	 * 
	 * @throws IOException
	 * @throws CodeValidatorException
	 * @throws InterruptedException 
	 */
	@Test
	public void testRunAllTestsOnMutant() throws IOException, CodeValidatorException, InterruptedException {
		User observer = new User("observer", User.encodePassword("password"), "demo@observer.com");
		observer.insert();
		//
		System.out.println("ConsistencyTest.testRunAllTestsOnMutant() Observer " + observer.getId());
		User attacker = new User("demoattacker", User.encodePassword("password"), "demo@attacker.com");
		attacker.insert();
		System.out.println("ConsistencyTest.testRunAllTestsOnMutant() Attacker" + attacker.getId());
		//
		// Create 3 defenders
		//
		User[] defenders = new User[3];
		for (int i = 0; i < defenders.length; i++) {
			defenders[i] = new User("demodefender" + i, User.encodePassword("password"), "demo"+i+"@defender.com");
			defenders[i].insert();
			System.out.println("ConsistencyTest.testRunAllTestsOnMutant() Defender " + defenders[i].getId());
		}

		// Upload the Class Under test - Maybe better use Classloader
		File cutFolder = new File(Constants.CUTS_DIR, "Lift");
		cutFolder.mkdirs();
		File jFile = new File(cutFolder, "Lift.java");
		File cFile = new File(cutFolder, "Lift.class");

		Files.copy(Paths.get("src/test/resources/itests/sources/Lift/Lift.java"), new FileOutputStream(jFile));
		Files.copy(Paths.get("src/test/resources/itests/sources/Lift/Lift.class"), new FileOutputStream(cFile));

		///
		GameClass cut = new GameClass("Lift", "Lift", jFile.getAbsolutePath(), cFile.getAbsolutePath());
		cut.insert();
		System.out.println("ConsistencyTest.testRunAllTestsOnMutant() Cut " + cut.getId());

		//
		final long startTime = System.currentTimeMillis() - 1000 * 3600;
		final long finishTime = System.currentTimeMillis() + 1000 * 3600;
		final MultiplayerGame multiplayerGame = new MultiplayerGame
				.Builder(cut.getId(), observer.getId(), startTime, finishTime, 2, 4, 4, 0, 0)
				.defenderValue(10)
				.attackerValue(4)
				.state(GameState.ACTIVE)
				.chatEnabled(true)
				.build();
		// Store to db
		multiplayerGame.insert();

		// Attacker and Defender must join the game. Those calls update also the
		// db
		multiplayerGame.addPlayer(attacker.getId(), Role.ATTACKER);
		//
		for (User defender : defenders) {
			multiplayerGame.addPlayer(defender.getId(), Role.DEFENDER);
		}

		System.out.println("ConsistencyTest.testRunAllTestsOnMutant() Game " + multiplayerGame.getId());

		MultiplayerGame activeGame = MultiplayerGameDAO.getMultiplayerGame(multiplayerGame.getId());
		assertEquals("Cannot find the right active game", multiplayerGame.getId(), activeGame.getId());

		// Attack
		String mutantText = new String(
				Files.readAllBytes(new File("src/test/resources/itests/mutants/Lift/MutantLift1.java").toPath()),
				Charset.defaultCharset());
		Mutant mutant = GameManagingUtils.createMutant(activeGame.getId(), activeGame.getClassId(), mutantText,
				attacker.getId(), Constants.MODE_BATTLEGROUND_DIR);

		// Generate the tests for the clients
		String testText = new String(
				Files.readAllBytes(new File("src/test/resources/itests/tests/KillingTestLift.java").toPath()),
				Charset.defaultCharset());
		//
		List<org.codedefenders.game.Test> tests = new ArrayList<>();
		for (final User defender : defenders) {
			tests.add(GameManagingUtils.createTest(activeGame.getId(), activeGame.getClassId(), testText, defender.getId(),
					Constants.MODE_BATTLEGROUND_DIR));
		}
		System.out.println("ReplayGame232Test.testRunAllTestsOnMutant() tests " + tests);
		// List<org.codedefenders.game.Test> theTests = activeGame.getTests(true);
		System.out.println("ReplayGame232Test.testRunAllTestsOnMutant() tests " + activeGame.getTests(true));

		// Now "submit the tests in parallel"

		ExecutorService executorService = Executors.newFixedThreadPool(defenders.length);

		for (final org.codedefenders.game.Test newTest : tests) {

			executorService.submit(new Runnable() {

				@Override
				public void run() {
					System.out.println("Submit test " + newTest.getId());
					MutationTester.runTestOnAllMultiplayerMutants(activeGame, newTest, new ArrayList<>());
					activeGame.update();
				}
			});
		}
		executorService.shutdown();
		executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

		// Refresh the state of the mutant... since there's no refresh method, we reload the object from the DB
		mutant = activeGame.getMutantByID(mutant.getId());
		assertNotNull(mutant);
		
		// assertMutant is killed !
		assertFalse("Mutant not killed", mutant.isAlive());

		int totalKilled = 0;
		for (final org.codedefenders.game.Test newTest : tests) {
			System.out.println("ReplayGame232Test.testRunAllTestsOnMutant() " + newTest.getMutantsKilled());
			totalKilled = totalKilled + newTest.getMutantsKilled();
		}
		assertEquals("Mutant killed multiple times !", 1, totalKilled);
		// Assert
		// List<org.codedefenders.game.Test> tests = activeGame.getTests(true); //

	}

}
