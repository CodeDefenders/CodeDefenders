/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;

import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.codedefenders.DatabaseRule;
import org.codedefenders.database.DatabaseConnection;
import org.codedefenders.execution.IMutationTester;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.UserEntity;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
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

//FIXME
@Ignore
@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({DatabaseConnection.class})
public class ExecutionTest {

    @Inject
    private GameManagingUtils gameManagingUtils;

    @Inject
    private IMutationTester mutationTester;

    @Rule
    public DatabaseRule db = new DatabaseRule();

    // PROBLEM: @ClassRule cannot be used with PowerMock ...
    private static File codedefendersHome;

    @BeforeClass
    public static void setupEnvironment() throws IOException {
        codedefendersHome = Files.createTempDirectory("integration-tests").toFile();
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
            @Override
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
    public void testMutant9559() throws IOException {
        // Create the users
        UserEntity observer = new UserEntity("observer", UserEntity.encodePassword("password"), "demo@observer.com");
        observer.insert();
        UserEntity attacker = new UserEntity("demoattacker", UserEntity.encodePassword("password"), "demo@attacker.com");
        attacker.insert();
        UserEntity defender = new UserEntity("demodefender", UserEntity.encodePassword("password"), "demo@defender.com");
        defender.insert();
        // CUT
        File cutFolder = new File(Constants.CUTS_DIR, "XmlElement");
        cutFolder.mkdirs();
        File javaFile = new File(cutFolder, "XmlElement.java");
        File classFile = new File(cutFolder, "XmlElement.class");
        Files.copy(Paths.get("src/test/resources/itests/sources/XmlElement/XmlElement.java"),
                new FileOutputStream(javaFile));
        Files.copy(Paths.get("src/test/resources/itests/sources/XmlElement/XmlElement.class"),
                new FileOutputStream(classFile));

        GameClass cut = GameClass.build()
                .name("XmlElement")
                .alias("XmlElement")
                .javaFile(javaFile.getAbsolutePath())
                .classFile(classFile.getAbsolutePath())
                .create();
        cut.insert();

        // Observer creates a new MP game
        MultiplayerGame multiplayerGame = new MultiplayerGame
                .Builder(cut.getId(), observer.getId(), 2)
                .state(GameState.ACTIVE)
                .level(GameLevel.EASY)
                .defenderValue(10)
                .attackerValue(4)
                .mutantValidatorLevel(CodeValidatorLevel.STRICT)
                .chatEnabled(true)
                .build();
        multiplayerGame.insert();

        System.out.println("ExecutionTest.testMutant9559() CREATED GAME " + multiplayerGame.getId());
        // Attacker and Defender join the game.
        multiplayerGame.addPlayer(defender.getId(), Role.DEFENDER);
        multiplayerGame.addPlayer(attacker.getId(), Role.ATTACKER);
        //////
        // Submit Mutant 9559
        String mutantText = new String(
                Files.readAllBytes(new File("src/test/resources/itests/mutants/XmlElement/Mutant9559.java").toPath()),
                Charset.defaultCharset());
        // Do the mutant thingy
        Mutant mutant = gameManagingUtils.createMutant(multiplayerGame.getId(), multiplayerGame.getClassId(),
                mutantText, attacker.getId(), Constants.MODE_BATTLEGROUND_DIR);
        //
        mutationTester.runAllTestsOnMutant(multiplayerGame, mutant);

    }
}
