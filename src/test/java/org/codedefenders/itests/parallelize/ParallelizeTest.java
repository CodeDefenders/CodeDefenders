package org.codedefenders.itests.parallelize;

import org.codedefenders.execution.MutationTester;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.DatabaseConnection;
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
import org.codedefenders.validation.CodeValidatorException;
import org.junit.After;
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
@PrepareForTest({DatabaseConnection.class}) // , MutationTester.class })
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

	@After
	public void deleteTemporaryFiles() {
		// TODO ?
	}

	@Test
	public void testRunAllTestsOnMutant() throws IOException, CodeValidatorException {
		User observer = new User("observer", "password", "demo@observer.com");
		observer.insert();
		//
		System.out.println("ParallelizeAntRunnerTest.testRunAllTestsOnMutant() Observer " + observer.getId());
		User attacker = new User("demoattacker", "password", "demo@attacker.com");
		attacker.insert();
		System.out.println("ParallelizeAntRunnerTest.testRunAllTestsOnMutant() Attacker " + attacker.getId());
		User defender = new User("demodefender", "password", "demo@defender.com");
		defender.insert();
		System.out.println("ParallelizeAntRunnerTest.testRunAllTestsOnMutant() Defender " + defender.getId());
		//
		//
		// Upload the Class Under test - Maybe better use Classloader
		// TODO Work only on Linux/Mac
		File cutFolder = new File(Constants.CUTS_DIR, "Lift");
		cutFolder.mkdirs();
		File jFile = new File(cutFolder, "Lift.java");
		File cFile = new File(cutFolder, "Lift.class");

		Files.copy(Paths.get("src/test/resources/itests/sources/Lift/Lift.java"), new FileOutputStream(jFile));
		Files.copy(Paths.get("src/test/resources/itests/sources/Lift/Lift.class"), new FileOutputStream(cFile));

		///
		GameClass cut = new GameClass("Lift", "Lift", jFile.getAbsolutePath(), cFile.getAbsolutePath());
		cut.insert();
		System.out.println("ParallelizeAntRunnerTest.testRunAllTestsOnMutant() Cut " + cut.getId());

		//
		MultiplayerGame multiplayerGame = new MultiplayerGame(cut.getId(), observer.getId(), GameLevel.HARD, (float) 1,
				(float) 1, (float) 1, 10, 4, 4, 4, 0, 0, (int) 1e5, (int) 1E30, GameState.ACTIVE.name(), false, 2, true, null, false);
		// Store to db
		multiplayerGame.insert();

		// Attacker and Defender must join the game. Those calls update also the
		// db
		multiplayerGame.addPlayer(defender.getId(), Role.DEFENDER);
		multiplayerGame.addPlayer(attacker.getId(), Role.ATTACKER);

		//
		System.out.println("ParallelizeAntRunnerTest.testRunAllTestsOnMutant() Game " + multiplayerGame.getId());
		//
		//
		MultiplayerGame activeGame = DatabaseAccess.getMultiplayerGame(multiplayerGame.getId());
		assertEquals("Cannot find the right active game", multiplayerGame.getId(), activeGame.getId());

		// Read and submit some test- This will call AntRunner
		// Are those saved to DB ?!
		for (int i = 1; i <= 4; i++) {
			String testText = new String(
					Files.readAllBytes(
							new File("src/test/resources/itests/tests/PassingTestLift" + i + ".java").toPath()),
					Charset.defaultCharset());
			GameManager.createTest(activeGame.getId(), activeGame.getClassId(), testText, defender.getId(), "mp");
		}
		// List<org.codedefenders.game.Test> tests = activeGame.getTests(true); //
		// executable
		// tests
		// submitted
		// by
		// Assertion roulette ?
		// assertEquals("Missing input test from defender!", 4, tests.size());

		// Schedule a test which kills the mutant - Where ? in the middle ?
		String testText = new String(
				Files.readAllBytes(new File("src/test/resources/itests/tests/KillingTestLift.java").toPath()),
				Charset.defaultCharset());
		GameManager.createTest(activeGame.getId(), activeGame.getClassId(), testText, defender.getId(), "mp");

		// Read and Submit the mutants - No tests so far
		String mutantText = new String(
				Files.readAllBytes(new File("src/test/resources/itests/mutants/Lift/MutantLift1.java").toPath()),
				Charset.defaultCharset());
		Mutant mutant = GameManager.createMutant(activeGame.getId(), activeGame.getClassId(), mutantText,
				attacker.getId(), "mp");

		assertNotNull("Invalid mutant", mutant.getClassFile());

		ArrayList<String> messages = new ArrayList<>();
		// TODO Mock the AntRunner... an interface would make things a lot
		// easier !
		// Finally invoke the method.... For the moment this invokes AntRunner
		MutationTester.runAllTestsOnMutant(activeGame, mutant, messages);

		// assertMutant is killed !
		assertFalse("Mutant not killed", mutant.isAlive());

		// FIXME. With the parallel implementation we have the "issue" that
		// cancelled tasks will run to completion, and store their data on the
		// db

	}
	// public static void runAllTestsOnMutant(AbstractGame game, Mutant mutant,
	// ArrayList<String> messages) {

}
