package org.codedefenders.itests;

import org.codedefenders.*;
import org.codedefenders.exceptions.CodeValidatorException;
import org.codedefenders.multiplayer.MultiplayerGame;
import org.codedefenders.rules.DatabaseRule;
import org.codedefenders.util.DatabaseConnection;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
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
import java.util.Hashtable;

@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({DatabaseConnection.class})
public class ExecutionTest {

    @Rule
    public DatabaseRule db = new DatabaseRule("defender", "db/emptydb.sql");

    // PROBLEM: @ClassRule cannot be used with PowerMock ...
    private static File codedefendersHome;

    @BeforeClass
    public static void setupEnvironment() throws IOException {
        codedefendersHome = Files.createTempDirectory("integration-tests").toFile();
        // TODO Will this remove all the files ?
        codedefendersHome.deleteOnExit();
    }

    // This factory enable to configure codedefenders properties
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
            PowerMockito.when(mockedEnvironmentContext.lookup("parallelize")).thenReturn("disabled");
            //
            PowerMockito.when(mockedEnvironmentContext.lookup("data.dir"))
                    .thenReturn(codedefendersHome.getAbsolutePath());

            PowerMockito.when(mockedEnvironmentContext.lookup("ant.home")).thenReturn("/usr/local");
            //
            return mockedInitialContext;
        }
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

    // TODO Maybe a rule here ?!
    @Before
    public void setupCodeDefendersEnvironment() throws IOException {
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

    /*
     * FIXME: Better to link tests to issues.
     *
     * mutant 9559, game 182, XmlElement class
     *
     * @@ -17,7 +17,7 @@ private List<XmlElement> subElements; - private
     * XmlElement parent; + private XmlElement parent = new
     * XmlElement("parent");
     *
     * should be trivial to kill with *any* test but is not killable the mutant
     * causes an infinite recursion in the constructor, so this should be a
     * trivial mutant
     */
    @Test
    public void testMutant9559() throws IOException, CodeValidatorException {
        // MOVE THIS CODE TO BEFORE OF FACTORY METHOD
        ArrayList<String> messages = new ArrayList<String>();
        // Create the users
        User observer = new User("observer", "password", "demo@observer.com");
        observer.insert();
        User attacker = new User("demoattacker", "password", "demo@attacker.com");
        attacker.insert();
        User defender = new User("demodefender", "password", "demo@defender.com");
        defender.insert();
        // CUT
        File cutFolder = new File(Constants.CUTS_DIR, "XmlElement");
        cutFolder.mkdirs();
        File jFile = new File(cutFolder, "XmlElement.java");
        File cFile = new File(cutFolder, "XmlElement.class");
        Files.copy(Paths.get("src/test/resources/itests/sources/XmlElement/XmlElement.java"), new FileOutputStream(jFile));
        Files.copy(Paths.get("src/test/resources/itests/sources/XmlElement/XmlElement.class"), new FileOutputStream(cFile));
        GameClass cut = new GameClass("XmlElement", "XmlElement", jFile.getAbsolutePath(), cFile.getAbsolutePath());
        cut.insert();
        // Observer creates a new MP game
        //
        MultiplayerGame multiplayerGame = new MultiplayerGame(cut.getId(), observer.getId(), GameLevel.HARD, (float) 1,
                (float) 1, (float) 1, 10, 4, 4, 4, 0, 0, (int) 1e5, (int) 1E30, GameState.ACTIVE.name(), false, 2, true, null, false);
        multiplayerGame.insert();
        // Attacker and Defender join the game.
        multiplayerGame.addPlayer(defender.getId(), Role.DEFENDER);
        multiplayerGame.addPlayer(attacker.getId(), Role.ATTACKER);
        //////
        // Submit Mutant 9559
        String mutantText = new String(
                Files.readAllBytes(new File("src/test/resources/itests/mutants/XmlElement/Mutant9559.java").toPath()),
                Charset.defaultCharset());
        // Do the mutant thingy
        Mutant mutant = GameManager.createMutant(multiplayerGame.getId(), multiplayerGame.getClassId(), mutantText,
                attacker.getId(), "mp");
        //
        MutationTester.runAllTestsOnMutant(multiplayerGame, mutant, messages);


    }
}
