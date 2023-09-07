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
package org.codedefenders;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.codedefenders.api.analytics.TestSmellDetectorProducer;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.execution.BackendExecutorService;
import org.codedefenders.execution.ClassCompilerService;
import org.codedefenders.game.GameClass;
import org.codedefenders.instrumentation.MetricsRegistry;
import org.codedefenders.notification.impl.NotificationService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.transaction.TransactionManager;
import org.codedefenders.util.WeldExtension;
import org.codedefenders.util.WeldSetup;
import org.codedefenders.util.concurrent.ExecutorServiceProvider;
import org.jboss.weld.junit4.WeldInitiator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import testsmell.AbstractSmell;
import testsmell.TestFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(WeldExtension.class)
public class GameManagingUtilsTest {

    private static TestSmellsDAO mockedTestSmellDAO;

    @TempDir
    public Path tempFolder;

    @WeldSetup
    public WeldInitiator weld = WeldInitiator
                .from(GameManagingUtils.class,
                        GameManagingUtilsTest.class,
                        TestSmellDetectorProducer.class,
                        NotificationService.class,
                        ExecutorServiceProvider.class,
                        MetricsRegistry.class,
                        Configuration.class)
                .inject(this)
                .activate(RequestScoped.class)
                .activate(ApplicationScoped.class)
                .build();// ofTestPackage();

    /*
     * TODO At the moment I cannot find a better way to initialize TestSmellsDAO
     * before the weld rule call the producer
     */
    @BeforeAll
    public static void setupTestSmellDao() {
        mockedTestSmellDAO = Mockito.mock(TestSmellsDAO.class);
    }

    /*
     * Since the mock is static we need to explicitly reset it between tests
     */
    @BeforeEach
    public void resetTestSmellsDAOMock() {
        Mockito.reset(mockedTestSmellDAO);
    }

    @ApplicationScoped
    @Produces
    TestSmellsDAO produceBar() {
        return mockedTestSmellDAO;
    }

    @ApplicationScoped
    @Produces
    ClassCompilerService produceClassCompiler() {
        return Mockito.mock(ClassCompilerService.class);

    }

    @ApplicationScoped
    @Produces
    BackendExecutorService produceBackend() {
        return Mockito.mock(BackendExecutorService.class);
    }

    @Produces
    @Singleton
    Configuration produceConfiguration() {
        return mock(Configuration.class);
    }

    @Produces
    TransactionManager produceTransactionManager() {
        return mock(TransactionManager.class);
    }


    @Inject
    // Testing configuration ?
    private GameManagingUtils gameManagingUtils;

    private GameClass createMockedCUT() throws IOException {
        String originalCode = """
                public class Lift {
                    private int topFloor;
                    private int currentFloor = 0;

                    public Lift(int highestFloor) {
                        this.topFloor = highestFloor;
                    }

                    public void goUp() {
                        currentFloor++;
                    }

                    public void goDown() {
                        currentFloor--;
                    }

                    public int getTopFloor() {
                        return topFloor;
                    }
                }""".stripIndent();
        File cutJavaFile = tempFolder.resolve("Lift.java").toFile();
        FileUtils.writeStringToFile(cutJavaFile, originalCode, StandardCharsets.UTF_8);

        GameClass mockedGameClass = mock(GameClass.class);

        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());
        return mockedGameClass;
    }

    private org.codedefenders.game.Test createMockedTest(String testCode) throws IOException {
        File testJavaFile = tempFolder.resolve("Test.java").toFile();
        FileUtils.writeStringToFile(testJavaFile, testCode, StandardCharsets.UTF_8);

        org.codedefenders.game.Test mockedTest = mock(org.codedefenders.game.Test.class);

        when(mockedTest.getJavaFile()).thenReturn(testJavaFile.getPath());
        return mockedTest;
    }

    @Test
    public void testSmellGood() throws IOException {
        String testCode = """
                import org.junit.*;
                import static org.junit.Assert.*;
                import static org.hamcrest.MatcherAssert.assertThat;
                import static org.hamcrest.Matchers.*;

                public class TestLift {
                    @Test(timeout = 4000)
                    public void test() throws Throwable {
                        Lift l = new Lift(50);
                        assertEquals(50, l.getTopFloor());
                    }
                }""".stripIndent();

        org.codedefenders.game.Test newTest = createMockedTest(testCode);
        GameClass cut = createMockedCUT();

        // configure the mock
        gameManagingUtils.detectTestSmells(newTest, cut);

        // Verify that the store method was called once and capture the
        // parameter passed to the invocation
        ArgumentCaptor<TestFile> argument = ArgumentCaptor.forClass(TestFile.class);
        Mockito.verify(mockedTestSmellDAO).storeSmell(Mockito.any(), argument.capture());

        // TODO Probably some smart argument matcher might be needed
        // TODO Matching by string is britlle, maybe match by "class/type"?
        Set<String> expectedSmells = new HashSet<>(Arrays.asList(new String[]{}));
        // Collect smells
        Set<String> actualSmells = new HashSet<>();
        for (AbstractSmell smell : argument.getValue().getTestSmells()) {
            if (smell.hasSmell()) {
                actualSmells.add(smell.getSmellName());
            }
        }

        assertEquals(expectedSmells, actualSmells);
    }

    @Test
    public void testEagerSmellDoesNotTriggerIfProductionsMethodsAreLessThanThreshold() throws IOException {
        // This has 2 production calls instead of 4 {@link
        // TestSmellDetectorProducer.EAGER_TEST_THRESHOLD}
        String testCode = """
                import org.junit.*;
                import static org.junit.Assert.*;
                import static org.hamcrest.MatcherAssert.assertThat;
                import static org.hamcrest.Matchers.*;

                public class TestLift {
                    @Test(timeout = 4000)
                    public void test() throws Throwable {
                        Lift l = new Lift(50);

                        // 1. Production Method call
                        l.goUp();
                        // 2. Production Method call
                        l.goUp();

                        // Calls inside the assertions (or inside other calls?) are not counted
                        assertEquals(50, l.getTopFloor());
                    }
                }""".stripIndent();

        org.codedefenders.game.Test newTest = createMockedTest(testCode);
        GameClass cut = createMockedCUT();

        // configure the mock
        gameManagingUtils.detectTestSmells(newTest, cut);

        // Verify that the store method was called
        ArgumentCaptor<TestFile> argument = ArgumentCaptor.forClass(TestFile.class);
        Mockito.verify(mockedTestSmellDAO).storeSmell(Mockito.any(), argument.capture());
        // We expect no smells
        Set<String> noSmellsExpected = new HashSet<>(Arrays.asList(new String[]{}));
        // Collect smells
        Set<String> actualSmells = new HashSet<>();
        for (AbstractSmell smell : argument.getValue().getTestSmells()) {
            if (smell.hasSmell()) {
                actualSmells.add(smell.getSmellName());
            }
        }

        assertEquals(noSmellsExpected, actualSmells);
    }

    @Test
    public void testEagerSmellTriggerIfProductionsMethodsAreEqualThanThreshold() throws IOException {
        // This has 3 production calls instead of 4 {@link
        // TestSmellDetectorProducer.EAGER_TEST_THRESHOLD}
        String testCode = """
                import org.junit.*;
                import static org.junit.Assert.*;
                import static org.hamcrest.MatcherAssert.assertThat;
                import static org.hamcrest.Matchers.*;

                public class TestLift {
                    @Test(timeout = 4000)
                    public void test() throws Throwable {
                        Lift l = new Lift(50);

                        // 1. Production Method call
                        l.goUp();
                        // 2. Production Method call
                        l.goDown();
                        // 3. Production Method call
                        l.getTopFloor();
                        // 4. Production Method call
                        l.goUp();

                        // Calls inside the assertions (or inside other calls?) are not counted
                        assertEquals(50, l.getTopFloor());
                    }
                }""".stripIndent();

        org.codedefenders.game.Test newTest = createMockedTest(testCode);
        GameClass cut = createMockedCUT();

        // configure the mock
        gameManagingUtils.detectTestSmells(newTest, cut);

        // Verify that the store method was called
        ArgumentCaptor<TestFile> argument = ArgumentCaptor.forClass(TestFile.class);
        Mockito.verify(mockedTestSmellDAO).storeSmell(Mockito.any(), argument.capture());
        // TODO Probably some smart argument matcher might be needed
        // TODO Matching by string is britlle, maybe match by "class/type"?
        Set<String> expectedSmells = Set.of("Eager Test");
        // Collect smells
        Set<String> actualSmells = new HashSet<>();
        for (AbstractSmell smell : argument.getValue().getTestSmells()) {
            if (smell.hasSmell()) {
                actualSmells.add(smell.getSmellName());
            }
        }

        assertEquals(expectedSmells, actualSmells);
    }

}
