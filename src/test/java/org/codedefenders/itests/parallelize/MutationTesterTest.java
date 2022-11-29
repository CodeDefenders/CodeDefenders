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
package org.codedefenders.itests.parallelize;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.codedefenders.DatabaseRule;
import org.codedefenders.MutationTesterUtilities;
import org.codedefenders.database.DatabaseConnection;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.execution.IMutationTester;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.itests.IntegrationTest;
import org.codedefenders.model.UserEntity;
import org.codedefenders.util.Constants;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * This test shall evaluate how Mutation Tester handle concurrent updates.
 * Problematic cases: - More than 1 tests kill the same mutant - Mutant state is
 * reset from killed to alive while setting the score
 *
 * <p>The problem lies in the fact that MutationTester set the state of the mutant
 * and forces it to the database, while the state of the mutants should be
 * handled by the database instead !
 *
 * @author gambi
 */

@Ignore // Test is broken, game232 data are no where..
@Category(IntegrationTest.class)
@RunWith(PowerMockRunner.class)
@PrepareForTest({DatabaseConnection.class}) // , MutationTester.class })
public class MutationTesterTest {
    private static Logger logger = LoggerFactory.getLogger(MutationTesterTest.class);

    // PowerMock does not work with @ClassRule !!
    // This really should be only per class, not per test... in each test we can
    // truncate the tables ?
    @Rule
    public DatabaseRule db = new DatabaseRule();

    //
    private static File codedefendersHome;

    // PROBLEM: @ClassRule cannot be used with PowerMock ...
    @BeforeClass
    public static void setupEnvironment() throws IOException {
        // ERROR > WARN > INFO > DEBUG > TRACE.
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO");
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.err");
        //

        codedefendersHome = Files.createTempDirectory("integration-tests").toFile();
        codedefendersHome.deleteOnExit();
    }

    @Before
    public void mockDBConnections() throws Exception {
        PowerMockito.mockStatic(DatabaseConnection.class);
        PowerMockito.when(DatabaseConnection.getConnection()).thenAnswer(invocation -> {
            // Return a new connection from the rule instead
            return db.getConnection();
        });
    }

    // CUT
    private IMutationTester tester;

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
        isCreated = (new File(codedefendersHome + "/mutants")).mkdirs() || (new File(codedefendersHome + "/mutants")).exists();
        // System.out.println("ParallelizeAntRunnerTest.setupClass() " +
        // isCreated);
        isCreated = (new File(codedefendersHome + "/sources")).mkdirs() || (new File(codedefendersHome + "/sources")).exists();
        // System.out.println("ParallelizeAntRunnerTest.setupClass() " +
        // isCreated);
        isCreated = (new File(codedefendersHome + "/tests")).mkdirs() || (new File(codedefendersHome + "/tests")).exists();
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

    @Test
    public void testRunAllTestsOnMutant() throws IOException, InterruptedException {

        // Create the users, game, and such
        UserEntity observer = new UserEntity("observer", UserEntity.encodePassword("password"), "demo@observer.com");
        observer.insert();

        UserEntity[] attackers = new UserEntity[3];
        attackers[0] = new UserEntity("demoattacker", UserEntity.encodePassword("password"), "demo@attacker.com");
        attackers[0].insert();
        attackers[1] = new UserEntity("demoattacker1", UserEntity.encodePassword("password"), "demo1@attacker.com");
        attackers[1].insert();
        attackers[2] = new UserEntity("demoattacker2", UserEntity.encodePassword("password"), "demo2@attacker.com");
        attackers[2].insert();

        //
        UserEntity[] defenders = new UserEntity[3];
        defenders[0] = new UserEntity("demodefender", UserEntity.encodePassword("password"), "demo@defender.com");
        defenders[0].insert();
        defenders[1] = new UserEntity("demodefender1", UserEntity.encodePassword("password"), "demo1@defender.com");
        defenders[1].insert();
        defenders[2] = new UserEntity("demodefender2", UserEntity.encodePassword("password"), "demo2@defender.com");
        defenders[2].insert();

        // Taken from Game 232

        // // Upload the Class Under test - Maybe better use Classloader
        File cutFolder = new File(codedefendersHome + "/sources", "IntHashMap");
        cutFolder.mkdirs();
        File jFile = new File(cutFolder, "IntHashMap.java");
        File cFile = new File(cutFolder, "IntHashMap.class");
        File c1File = new File(cutFolder, "IntHashMap$Entry.class");
        //
        Files.copy(Paths.get("src/test/resources/replay/game232/IntHashMap.java"), new FileOutputStream(jFile));
        Files.copy(Paths.get("src/test/resources/replay/game232/IntHashMap.clazz"), new FileOutputStream(cFile));
        Files.copy(Paths.get("src/test/resources/replay/game232/IntHashMap$Entry.clazz"), new FileOutputStream(c1File));

        GameClass cut = GameClass.build()
                .name("IntHashMap")
                .alias("IntHashMap")
                .javaFile(jFile.getAbsolutePath())
                .classFile(cFile.getAbsolutePath())
                .create();
        cut.insert();
        // System.out.println("ParallelizeAntRunnerTest.testRunAllTestsOnMutant()
        // Cut " + cut.getId());
        MultiplayerGame multiplayerGame = new MultiplayerGame
                .Builder(cut.getId(), observer.getId(), 2)
                .state(GameState.ACTIVE)
                .level(GameLevel.HARD)
                .defenderValue(10)
                .attackerValue(4)
                .build();
        multiplayerGame.insert();
        // Attacker and Defender must join the game. Those calls update also the db
        multiplayerGame.addPlayer(defenders[0].getId(), Role.DEFENDER);
        multiplayerGame.addPlayer(defenders[1].getId(), Role.DEFENDER);
        multiplayerGame.addPlayer(defenders[2].getId(), Role.DEFENDER);

        multiplayerGame.addPlayer(attackers[0].getId(), Role.ATTACKER);
        multiplayerGame.addPlayer(attackers[1].getId(), Role.ATTACKER);
        multiplayerGame.addPlayer(attackers[2].getId(), Role.ATTACKER);

        // System.out.println(" Game " + multiplayerGame.getId());

        MultiplayerGame activeGame = MultiplayerGameDAO.getMultiplayerGame(multiplayerGame.getId());
        assertEquals("Cannot find the right active game", multiplayerGame.getId(), activeGame.getId());

        // Use the trace for Game232 - Synchronosous for the moment

        ScheduledExecutorService scheduledExecutors = Executors.newScheduledThreadPool(6);

        // 1/10) Defend defenders[0]using
        // src/test/resources/replay/game232/test.7851
        final ArrayList<String> messages = new ArrayList<>();

        long initialDelay = 1000;
        int speedup = 10;

        scheduledExecutors.schedule(
                MutationTesterUtilities.defend(activeGame, "src/test/resources/replay/game232/test.7851", defenders[0], logger),
                (0 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 2/10) Defend defenders[1]using
        // src/test/resources/replay/game232/test.7853
        scheduledExecutors.schedule(
                MutationTesterUtilities.defend(activeGame, "src/test/resources/replay/game232/test.7853", defenders[1], logger),
                (45000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 3/10) Attack attackers[0]using
        // src/test/resources/replay/game232/mutant.12924
        scheduledExecutors.schedule(
                MutationTesterUtilities.attack(
                        activeGame,
                        "src/test/resources/replay/game232/mutant.12924",
                        attackers[0], logger),
                (46000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 4/10) Attack attackers[1]using
        // src/test/resources/replay/game232/mutant.12925
        scheduledExecutors.schedule(
                MutationTesterUtilities.attack(activeGame, "src/test/resources/replay/game232/mutant.12925", attackers[1], logger),
                (50000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 5/10) Defend defenders[0]using
        // src/test/resources/replay/game232/test.7854
        scheduledExecutors.schedule(
                MutationTesterUtilities.defend(activeGame, "src/test/resources/replay/game232/test.7854", defenders[0], logger),
                (58000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 6/10) Attack attackers[2] using
        // src/test/resources/replay/game232/mutant.12933
        scheduledExecutors.schedule(
                MutationTesterUtilities.attack(activeGame, "src/test/resources/replay/game232/mutant.12933", attackers[2], logger),
                (66000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 7/10) Defend defenders[1]using
        // src/test/resources/replay/game232/test.7859
        scheduledExecutors.schedule(
                MutationTesterUtilities.defend(activeGame, "src/test/resources/replay/game232/test.7859", defenders[1], logger),
                (82000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 8/10) Attack attackers[1]using
        // src/test/resources/replay/game232/mutant.12958
        scheduledExecutors.schedule(
                MutationTesterUtilities.attack(activeGame, "src/test/resources/replay/game232/mutant.12958", attackers[1], logger),
                (119000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 9/10) Defend defenders[0]using
        // src/test/resources/replay/game232/test.7866
        scheduledExecutors.schedule(
                MutationTesterUtilities.defend(activeGame, "src/test/resources/replay/game232/test.7866", defenders[0], logger),
                (157000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 10/10) Defend defenders[2]using
        // src/test/resources/replay/game232/test.7869
        scheduledExecutors.schedule(
                MutationTesterUtilities.defend(activeGame, "src/test/resources/replay/game232/test.7869", defenders[2], logger),
                (192000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 11/19) Attack attackers[1] using
        // src/test/resources/replay/game232/mutant.12987
        scheduledExecutors.schedule(
                MutationTesterUtilities.attack(activeGame, "src/test/resources/replay/game232/mutant.12987", attackers[1], logger),
                (198000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 12/19) Attack attackers[0] using
        // src/test/resources/replay/game232/mutant.12989
        scheduledExecutors.schedule(
                MutationTesterUtilities.attack(activeGame, "src/test/resources/replay/game232/mutant.12989", attackers[0], logger),
                (202000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 13/19) Attack attackers[1] using
        // src/test/resources/replay/game232/mutant.12995
        scheduledExecutors.schedule(
                MutationTesterUtilities.attack(activeGame, "src/test/resources/replay/game232/mutant.12995", attackers[1], logger),
                (214000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 14/19) Attack attackers[0] using
        // src/test/resources/replay/game232/mutant.12999
        scheduledExecutors.schedule(
                MutationTesterUtilities.attack(activeGame, "src/test/resources/replay/game232/mutant.12999", attackers[0], logger),
                (231000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 15/19) Defend defenders[0] using
        // src/test/resources/replay/game232/test.7872
        scheduledExecutors.schedule(
                MutationTesterUtilities.defend(activeGame, "src/test/resources/replay/game232/test.7872", defenders[0], logger),
                (231000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 16/19) Attack attackers[1] using
        // src/test/resources/replay/game232/mutant.13007
        scheduledExecutors.schedule(
                MutationTesterUtilities.attack(activeGame, "src/test/resources/replay/game232/mutant.13007", attackers[1], logger),
                (246000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 17/19) Attack attackers[1] using
        // src/test/resources/replay/game232/mutant.13014
        scheduledExecutors.schedule(
                MutationTesterUtilities.attack(activeGame, "src/test/resources/replay/game232/mutant.13014", attackers[1], logger),
                (266000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 18/19) Attack attackers[0] using
        // src/test/resources/replay/game232/mutant.13022
        scheduledExecutors.schedule(
                MutationTesterUtilities.attack(activeGame, "src/test/resources/replay/game232/mutant.13022", attackers[0], logger),
                (289000 + initialDelay) / speedup, TimeUnit.MILLISECONDS);

        // 19/19) Attack attackers[1] using
        // src/test/resources/replay/game232/mutant.13025
        scheduledExecutors.schedule(
                MutationTesterUtilities.attack(activeGame, "src/test/resources/replay/game232/mutant.13025", attackers[1], logger),
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
