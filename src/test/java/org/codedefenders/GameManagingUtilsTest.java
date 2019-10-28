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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.codedefenders.api.analytics.TestSmellDetectorProducer;
import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.execution.BackendExecutorService;
import org.codedefenders.execution.ClassCompilerService;
import org.codedefenders.game.GameClass;
import org.codedefenders.notification.impl.NotificationService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.jboss.weld.junit4.WeldInitiator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import edu.emory.mathcs.backport.java.util.Arrays;
import testsmell.AbstractSmell;
import testsmell.TestFile;

public class GameManagingUtilsTest {

    private static TestSmellsDAO mockedTestSmellDAO;

    /*
     * TODO At the moment I cannot find a better way to initialize TestSmellsDAO
     * before the weld rule call the producer
     */
    @BeforeClass
    public static void setupTestSmellDao() {
        mockedTestSmellDAO = Mockito.mock(TestSmellsDAO.class);
    }

    /*
     * Since the mock is static we need to explicitly reset it between tests
     */
    @Before
    public void resetTestSmellsDAOMock() {
        Mockito.reset(mockedTestSmellDAO);
    }

    // https://github.com/weld/weld-junit/blob/master/junit4/README.md
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public WeldInitiator weld = WeldInitiator
            .from(GameManagingUtils.class,
                    GameManagingUtilsTest.class,
                    TestSmellDetectorProducer.class,
                    NotificationService.class)
            .inject(this)
            .activate(RequestScoped.class)
            .activate(ApplicationScoped.class)
            .build();// ofTestPackage();

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

    @Inject
    // Testing configuration ?
    private GameManagingUtils gameManagingUtils;

    private GameClass createMockedCUT() throws IOException {
        String originalCode = "" //
                + "public class Lift {" + "\n" //
                + "private int topFloor;" + "\n"//
                + "private int currentFloor=0;" + "\n"//
                + "public Lift(int highestFloor) { this.topFloor = highestFloor;}" + "\n" //
                + "public void goUp() {currentFloor++;}" + "\n" //
                + "public void goDown() {currentFloor--;}" + "\n" //
                + "public int getTopFloor() { return topFloor;}" + "\n" //
                + "}";

        File cutJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(cutJavaFile, originalCode, Charset.defaultCharset());

        GameClass mockedGameClass = mock(GameClass.class);

        when(mockedGameClass.getJavaFile()).thenReturn(cutJavaFile.getPath());
        return mockedGameClass;
    }

    private org.codedefenders.game.Test createMockedTest(String testCode) throws IOException {
        File testJavaFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(testJavaFile, testCode, Charset.defaultCharset());

        org.codedefenders.game.Test mockedTest = mock(org.codedefenders.game.Test.class);

        when(mockedTest.getJavaFile()).thenReturn(testJavaFile.getPath());
        return mockedTest;
    }

    @Test
    public void testSmellGood() throws IOException {
        String testCode = "" + "import org.junit.*;" + "\n" + "import static org.junit.Assert.*;" + "\n"
                + "import static org.hamcrest.MatcherAssert.assertThat;" + "\n"
                + "import static org.hamcrest.Matchers.*;" + "\n" + "public class TestLift {" + "\n"
                + "    @Test(timeout = 4000)" + "\n" + "    public void test() throws Throwable {" + "\n"
                + "        Lift l = new Lift(50);" + "\n" + "        assertEquals(50, l.getTopFloor());" + "\n"
                + "    }" + "\n" + "}";

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
        Set<String> expectedSmells = new HashSet<>(Arrays.asList(new String[] {}));
        // Collect smells
        Set<String> actualSmells = new HashSet<>();
        for (AbstractSmell smell : argument.getValue().getTestSmells()) {
            if (smell.getHasSmell()) {
                actualSmells.add(smell.getSmellName());
            }
        }

        Assert.assertEquals(expectedSmells, actualSmells);
    }

    @Test
    public void testEagerSmellDoesNotTriggerIfProductionsMethodsAreLessThanThreshold() throws IOException {
        // This has 2 production calls instead of 4 {@link
        // TestSmellDetectorProducer.EAGER_TEST_THRESHOLD}
        String testCode = "" + "import org.junit.*;" + "\n" + "import static org.junit.Assert.*;" + "\n"
                + "import static org.hamcrest.MatcherAssert.assertThat;" + "\n"
                + "import static org.hamcrest.Matchers.*;" + "\n" + "public class TestLift {" + "\n"
                + "    @Test(timeout = 4000)" + "\n" + "    public void test() throws Throwable {" + "\n"
                + "        Lift l = new Lift(50);" + "\n"
                // 1 Production Method call
                + "        l.goUp();" + "\n" //
                // 2 Production Method call
                + "        l.goUp();" + "\n" //
                // Calls inside the assertions (or inside other calls?) are not counted
                + "        assertEquals(50, l.getTopFloor());" + "\n" + "    }" + "\n" + "}";

        org.codedefenders.game.Test newTest = createMockedTest(testCode);
        GameClass cut = createMockedCUT();

        // configure the mock
        gameManagingUtils.detectTestSmells(newTest, cut);

        // Verify that the store method was called
        ArgumentCaptor<TestFile> argument = ArgumentCaptor.forClass(TestFile.class);
        Mockito.verify(mockedTestSmellDAO).storeSmell(Mockito.any(), argument.capture());
        // We expect no smells
        Set<String> noSmellsExpected = new HashSet<>(Arrays.asList(new String[] {}));
        // Collect smells
        Set<String> actualSmells = new HashSet<>();
        for (AbstractSmell smell : argument.getValue().getTestSmells()) {
            if (smell.getHasSmell()) {
                actualSmells.add(smell.getSmellName());
            }
        }

        Assert.assertEquals(noSmellsExpected, actualSmells);
    }

    @Test
    public void testEagerSmellTriggerIfProductionsMethodsAreEqualThanThreshold() throws IOException {
        // This has 3 production calls instead of 4 {@link
        // TestSmellDetectorProducer.EAGER_TEST_THRESHOLD}
        String testCode = "" + "import org.junit.*;" + "\n" + "import static org.junit.Assert.*;" + "\n"
                + "import static org.hamcrest.MatcherAssert.assertThat;" + "\n"
                + "import static org.hamcrest.Matchers.*;" + "\n" + "public class TestLift {" + "\n"
                + "    @Test(timeout = 4000)" + "\n" + "    public void test() throws Throwable {" + "\n"
                + "        Lift l = new Lift(50);" + "\n"
                // 1 Production Method call
                + "        l.goUp();" + "\n" //
                // 2 Production Method call
                + "        l.goDown();" + "\n" //
                // 3 Production Method call
                + "        l.getTopFloor();" + "\n" //
                // 4 Production Method call
                + "        l.goUp();" + "\n" //
                + "        assertEquals(50, l.getTopFloor());" + "\n" + "    }" + "\n" + "}";

        org.codedefenders.game.Test newTest = createMockedTest(testCode);
        GameClass cut = createMockedCUT();

        // configure the mock
        gameManagingUtils.detectTestSmells(newTest, cut);

        // Verify that the store method was called
        ArgumentCaptor<TestFile> argument = ArgumentCaptor.forClass(TestFile.class);
        Mockito.verify(mockedTestSmellDAO).storeSmell(Mockito.any(), argument.capture());
        // TODO Probably some smart argument matcher might be needed
        // TODO Matching by string is britlle, maybe match by "class/type"?
        Set<String> expectedSmells = new HashSet<>(Arrays.asList(new String[] { "Eager Test" }));
        // Collect smells
        Set<String> actualSmells = new HashSet<>();
        for (AbstractSmell smell : argument.getValue().getTestSmells()) {
            if (smell.getHasSmell()) {
                actualSmells.add(smell.getSmellName());
            }
        }

        Assert.assertEquals(expectedSmells, actualSmells);
    }

}
