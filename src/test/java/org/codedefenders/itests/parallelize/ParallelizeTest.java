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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.DatabaseConnection;
import org.codedefenders.database.UserDAO;
import org.codedefenders.execution.MutationTester;
import org.codedefenders.execution.RandomTestScheduler;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.execution.TestScheduler;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.itests.IntegrationTest;
import org.codedefenders.model.User;
import org.codedefenders.rules.DatabaseRule;
import org.codedefenders.servlets.games.GameManager;
import org.codedefenders.util.Constants;
import org.codedefenders.validation.code.CodeValidatorException;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import difflib.DiffUtils;
import difflib.Patch;
import edu.emory.mathcs.backport.java.util.Arrays;

@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DatabaseConnection.class, MutationTester.class}) // , MutationTester.class })
public class ParallelizeTest {

	// PowerMock does not work with @ClassRule !!
	// This really should be only per class, not per test... in each test we can
	// truncate the tables ?
	@Rule
	public DatabaseRule db = new DatabaseRule("defender", "db/emptydb.sql");

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

	private User createAndInsertUserInDBWithDefaultCredentials(String name) {
		User newUser = new User(name, User.encodePassword("password"), "demo@" + name + ".org");
		boolean inserted = newUser.insert();
		assumeTrue(inserted);
		return newUser;
	}

	// This assumes standard layout
	private GameClass updateCUTAndStoreInDB(String name) {
		try {
			// Upload the Class Under test - Maybe better use Classloader
			// Where is this store eventually?
			File cutFolder = new File(Constants.CUTS_DIR, name);
			cutFolder.mkdirs();
			File jFile = new File(cutFolder, name + ".java");
			File cFile = new File(cutFolder, name + ".class");

			Files.copy(Paths.get("src/test/resources/itests/sources/" + name + "/" + name + ".java"),
					new FileOutputStream(jFile));
			Files.copy(Paths.get("src/test/resources/itests/sources/" + name + "/" + name + ".class"),
					new FileOutputStream(cFile));

			///
			GameClass cut = new GameClass("Lift", "Lift", jFile.getAbsolutePath(), cFile.getAbsolutePath());
			//
			System.out.println("ParallelizeTest.updateCUTAndStoreInDB() Storing CUT to " + jFile.getAbsolutePath());
			//
			boolean inserted = cut.insert();
			assumeTrue(inserted);
			return cut;

		} catch (Exception e) {
			assumeNoException(e);
		}
		return null;
	}

	private MultiplayerGame createStandardBattlegroundStartItAndStoreInDB(GameClass cut, User creator) {
		//
		int classId = cut.getId();
		int creatorId = creator.getId();
		GameLevel level = GameLevel.HARD;
		float lineCoverage = (float) 1;
		float mutantCoverage = (float) 1;
		float prize = (float) 1;
		int defenderValue = 1;
		int attackerValue = 1;
		int defenderLimit = 2;
		int attackerLimit = 2;
		int minDefenders = 1;
		int minAttackers = 1;
		long startDateTime = new Timestamp(System.currentTimeMillis() - 10000).getTime();
		//
		long finishDateTime = new Timestamp(System.currentTimeMillis() + 24 * 60 * 60 * 100 * 1000).getTime();

		String status = GameState.ACTIVE.name();
		int maxAssertionsPerTest = 2;
		boolean chatEnabled = false;
		CodeValidatorLevel mutantValidatorLevel = CodeValidatorLevel.MODERATE;
		boolean markUncovered = false;

		MultiplayerGame multiplayerGame = new MultiplayerGame(classId, creatorId, level, lineCoverage, mutantCoverage,
				prize, defenderValue, attackerValue, defenderLimit, attackerLimit, minDefenders, minAttackers,
				startDateTime, finishDateTime, status, maxAssertionsPerTest, chatEnabled, mutantValidatorLevel,
				markUncovered);
		// Store to db
		boolean inserted = multiplayerGame.insert();
		assumeTrue(inserted);

		// Check that the game really started?
		MultiplayerGame activeGame = DatabaseAccess.getMultiplayerGame(multiplayerGame.getId());
		assumeThat(multiplayerGame.getId(), is(activeGame.getId()));

		return activeGame;
	}

	private MultiplayerGame setupTestBattlegroundUsing(String cutName) {
		User observer = createAndInsertUserInDBWithDefaultCredentials("observer");
		User defender = createAndInsertUserInDBWithDefaultCredentials("demodefender");
		User attacker = createAndInsertUserInDBWithDefaultCredentials("demoattacker");

		GameClass cut = updateCUTAndStoreInDB(cutName);

		MultiplayerGame activeGame = createStandardBattlegroundStartItAndStoreInDB(cut, observer);
		// Attacker and Defender must join the game.
		// Those calls update also the DB
		boolean playerAdded = activeGame.addPlayer(defender.getId(), Role.DEFENDER);
		assumeTrue(playerAdded);
		System.out.println("ParallelizeTest.setupTestBattlegroundUsing() Added defender " + defender.getId() );
		
		playerAdded = activeGame.addPlayer(attacker.getId(), Role.ATTACKER);
		assumeTrue(playerAdded);
		System.out.println("ParallelizeTest.setupTestBattlegroundUsing() Added attacker " + attacker.getId() );
		//
		return activeGame;
	}

	/*
	 * Create few tests with no mutant, then create a mutant and check that the
	 * tests are executed in the corrected order
	 */
	@Test
	public void testTestExecutionOrder() {
		MultiplayerGame battlegroundGame = setupTestBattlegroundUsing("Lift");
		
		//
		
		int defenderID = UserDAO.getUserForPlayer( battlegroundGame.getDefenderIds()[0]).getId();
		int attackerID =  UserDAO.getUserForPlayer( battlegroundGame.getAttackerIds()[0]).getId();
		ArrayList<String> messages = new ArrayList<>();

		List<org.codedefenders.game.Test> submittedTests = new ArrayList<>();
		try {
			// Submit and execute a Test
			String testText = ""
					+ "/* no package name */" + "\n"
					+"" + "\n"
					+"import static org.junit.Assert.*;" + "\n"
					+"" + "\n"
					+"import org.junit.*;" + "\n"
					+"public class TestLift {" + "\n"
					+"	@Test(timeout = 4000)" + "\n"
					+"	public void test() throws Throwable {" + "\n"
					+"		// test here!" + "\n"
					+"		Lift l = new Lift(10,10);" + "\n"
					+"		assertEquals(10, l.getTopFloor());" + "\n"
					+"" + "\n"
					+"	}" + "\n"
					+"}";

			org.codedefenders.game.Test newTest = GameManager.createTest(battlegroundGame.getId(), battlegroundGame.getClassId(), testText, defenderID, "mp");
			MutationTester.runTestOnAllMultiplayerMutants(battlegroundGame, newTest, messages);
			assumeThat(battlegroundGame.getTests(true).size(), is(1));
			// Append this for oracles and mocks
			submittedTests.add( newTest );
			

			// Submit and execute a Test
			testText = ""
					+ "/* no package name */" + "\n"
					+"" + "\n"
					+"import static org.junit.Assert.*;" + "\n"
					+"" + "\n"
					+"import org.junit.*;" + "\n"
					+"public class TestLift {" + "\n"
					+"	@Test(timeout = 4000)" + "\n"
					+"	public void test() throws Throwable {" + "\n"
					+"		// test here!" + "\n"
					+"		Lift l = new Lift(10,10);" + "\n"
					+"		assertEquals(10, l.getCapacity());"+ "\n"
					+"" + "\n"
					+"	}" + "\n"
					+"}";
			newTest = GameManager.createTest(battlegroundGame.getId(), battlegroundGame.getClassId(), testText, defenderID, "mp");
			MutationTester.runTestOnAllMultiplayerMutants(battlegroundGame, newTest, messages);
			assumeThat(battlegroundGame.getTests(true).size(), is(2));
			// Append this for oracles and mocks
			submittedTests.add( newTest );
			
			// Create the mutant from the patch
			List<String> diff = Arrays.asList( new String[]{
					"--- null",
					"+++ null",
					"@@ -11,7 +11,7 @@",
					" ",
					"     public Lift(int highestFloor, int maxRiders) {",
					"         this(highestFloor);",
					"-        capacity = maxRiders;",
					"+        capacity = 0;",
					"     }",
					" ",
					"     public int getTopFloor() {"
			});
			
			Patch patch = DiffUtils.parseUnifiedDiff(diff);
			// Read the CUT code
			List<String> origincalCode = Arrays.asList( battlegroundGame.getCUT().getAsString().split("\n") );
			// Apply the patch
			
			List<String> mutantCode =(List<String>) DiffUtils.patch( origincalCode, patch);
			String mutantText = String.join("\n", mutantCode);
			
			Mutant mutant = GameManager.createMutant(battlegroundGame.getId(), battlegroundGame.getClassId(),
					mutantText, attackerID, "mp");
			
			// Mock the scheduler to return a random but known test distribution:
			TestScheduler mockedTestScheduler = Mockito.mock( TestScheduler.class );
			List<org.codedefenders.game.Test> randomSchedule = new RandomTestScheduler().scheduleTests( submittedTests );
			Mockito.doReturn(randomSchedule).when(mockedTestScheduler)
					.scheduleTests(
							org.mockito.Matchers.anyList());
			
			// Do the execution
			MutationTester.runAllTestsOnMutant(battlegroundGame, mutant, messages, mockedTestScheduler);

			// Check that the test execution logged in the DB are in the same order
			List<TargetExecution> executedTargets = new ArrayList<TargetExecution>(); 
			for( org.codedefenders.game.Test submittedTest : randomSchedule ){
				executedTargets.add( DatabaseAccess.getTargetExecutionForPair(submittedTest.getId(), mutant.getId()));
			}
			
			
			
			
			// Ideal Solution: use verify static, problem MutationTester is the class under test AND the mocked class !
//			 PowerMockito.verifyStatic(VerificationModeFactory.times(2));
//			 // Since I am not sure the instances will be the same given the DB interaction I need to explicitly
//			 // check the content of the elements
//			 MutationTester.testVsMutant(
//					 Mockito.argThat(
//					 // Match test by ID
//					 new ArgumentMatcher<org.codedefenders.game.Test>() {
//						@Override
//						public boolean matches(Object argument) {
//							if( argument instanceof org.codedefenders.game.Test){
//								System.out.println("matching " + ((org.codedefenders.game.Test) argument).getId() + " with " + randomSchedule.get(0).getId());
//								return ((org.codedefenders.game.Test) argument).getId() == randomSchedule.get(0).getId();
//							} else {
//							return false;
//							}
//						}
//					 }), 
//					 Mockito.argThat(
//					 new ArgumentMatcher<Mutant>() {
//							@Override
//							public boolean matches(Object argument) {
//								if( argument instanceof Mutant){
//									System.out.println("matching " + ((Mutant) argument).getId() + " with " + mutant.getId());
//									return ((Mutant) argument).getId() == mutant.getId();
//								} else {
//								return false;
//								}
//							}
//						 }));
//			 
			 
		} catch (Exception e) {
			assumeNoException(e);
		}

	}

	@Test
	public void testRunAllTestsOnMutant() throws IOException, CodeValidatorException {

		MultiplayerGame battlegroundGame = setupTestBattlegroundUsing("Lift");
		int defenderID = battlegroundGame.getDefenderIds()[0];
		int attackerID = battlegroundGame.getAttackerIds()[0];

		// Read and submit some test- This will call AntRunner
		// Are those saved to DB ?!
		for (int i = 1; i <= 4; i++) {
			String testText = new String(
					Files.readAllBytes(
							new File("src/test/resources/itests/tests/PassingTestLift" + i + ".java").toPath()),
					Charset.defaultCharset());
			GameManager.createTest(battlegroundGame.getId(), battlegroundGame.getClassId(), testText, defenderID, "mp");
		}

		// Schedule a test which kills the mutant - Where ? in the middle ?
		String testText = new String(
				Files.readAllBytes(new File("src/test/resources/itests/tests/KillingTestLift.java").toPath()),
				Charset.defaultCharset());
		GameManager.createTest(battlegroundGame.getId(), battlegroundGame.getClassId(), testText, defenderID, "mp");

		// Read and Submit the mutants - No tests so far
		String mutantText = new String(
				Files.readAllBytes(new File("src/test/resources/itests/mutants/Lift/MutantLift1.java").toPath()),
				Charset.defaultCharset());
		Mutant mutant = GameManager.createMutant(battlegroundGame.getId(), battlegroundGame.getClassId(), mutantText,
				attackerID, "mp");

		assertNotNull("Invalid mutant", mutant.getClassFile());

		ArrayList<String> messages = new ArrayList<>();
		// TODO Mock the AntRunner... an interface would make things a lot
		// easier !
		// Finally invoke the method.... For the moment this invokes AntRunner
		MutationTester.runAllTestsOnMutant(battlegroundGame, mutant, messages);

		// assertMutant is killed !
		assertFalse("Mutant not killed", mutant.isAlive());

		// FIXME. With the parallel implementation we have the "issue" that
		// cancelled tasks will run to completion, and store their data on the
		// db

	}
	// public static void runAllTestsOnMutant(AbstractGame game, Mutant mutant,
	// ArrayList<String> messages) {

}
