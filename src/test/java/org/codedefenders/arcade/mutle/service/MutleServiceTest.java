/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
package org.codedefenders.arcade.mutle.service;

import java.time.Instant;
import java.util.List;

import org.codedefenders.arcade.mutle.fixture.MutleEntry;
import org.codedefenders.arcade.mutle.model.MutleGame;
import org.codedefenders.execution.TargetExecution;
import org.junit.jupiter.api.Test;

import static org.codedefenders.execution.TargetExecution.Status.ERROR;
import static org.codedefenders.execution.TargetExecution.Status.FAIL;
import static org.codedefenders.execution.TargetExecution.Status.SUCCESS;
import static org.codedefenders.execution.TargetExecution.Target.COMPILE_TEST;
import static org.codedefenders.execution.TargetExecution.Target.TEST_ORIGINAL;
import static org.codedefenders.util.Constants.TEST_DID_NOT_COMPILE_MESSAGE;
import static org.codedefenders.util.Constants.TEST_DID_NOT_PASS_ON_CUT_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MutleServiceTest {
    private static final String RAW_EXECUTION_OUTPUT = "java.lang.AssertionError\n\tat TestAppointmentBook.test";
    private static final String COMPILER_OUTPUT = """
            [javac] TestAppointmentBook.java:15: error: ';' expected
            [javac]         assertFalse(ab.book("foo", 2))
            [javac]                                       ^
            [javac] 1 error
            """;

    @Test
    void hidesCompilerOutputFromRejectedSubmission() {
        assertEquals(TEST_DID_NOT_COMPILE_MESSAGE,
                MutleService.rejectionMessage(execution(COMPILE_TEST, FAIL), execution(TEST_ORIGINAL, SUCCESS)));
    }

    @Test
    void reportsCompilerDiagnosticWithoutBuildOutput() {
        TargetExecution compilation = new TargetExecution(1, 0, COMPILE_TEST, FAIL, COMPILER_OUTPUT);

        assertEquals("Your test did not compile. Line 15: ';' expected",
                MutleService.rejectionMessage(compilation, null));
    }

    @Test
    void recognizesCompilationFailureBeforeMissingOriginalExecution() {
        assertEquals(TEST_DID_NOT_COMPILE_MESSAGE, MutleService.rejectionMessage(execution(COMPILE_TEST, FAIL), null));
    }

    @Test
    void reportsOriginalClassFailureWithoutBuildOutput() {
        assertEquals(TEST_DID_NOT_PASS_ON_CUT_MESSAGE
                        + "\n\nDetails:\njava.lang.AssertionError\nat TestAppointmentBook.test",
                MutleService.rejectionMessage(execution(COMPILE_TEST, SUCCESS), execution(TEST_ORIGINAL, ERROR)));
    }

    @Test
    void extractsJUnitErrorFromAntOutput() {
        String output = """
                Buildfile: /tmp/build.xml
                [echo] Test path: /tmp/a:/tmp/b
                [junit] Testsuite: TestParcelLocker
                [junit] Testcase: test took 0.004 sec
                [junit] Caused an ERROR
                [junit] parcel id must not be blank
                [junit] java.lang.IllegalArgumentException: parcel id must not be blank
                [junit] at ParcelLocker.deposit(ParcelLocker.java:12)
                [junit] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0
                BUILD FAILED
                """;

        assertEquals("parcel id must not be blank\n"
                        + "java.lang.IllegalArgumentException: parcel id must not be blank\n"
                        + "at ParcelLocker.deposit(ParcelLocker.java:12)",
                MutleService.testFailureDetails(output));
    }

    @Test
    void handlesMissingExecutionResult() {
        assertEquals("The test could not be accepted.", MutleService.rejectionMessage(null, null));
    }

    @Test
    void historyExcludesPracticeAndCurrentGames() {
        List<MutleGame> games = List.of(
                game(1, -101),
                game(2, 12),
                game(3, 11));

        List<MutleGame> history = MutleService.historyGames(games, entries(), 12);

        assertEquals(List.of(3), history.stream().map(MutleGame::gameId).toList());
    }

    @Test
    void historyExcludesGamesForRemovedFixtures() {
        List<MutleGame> games = List.of(
                game(1, "removed/mutant", 10),
                game(2, "fixture/mutant", 11));

        List<MutleGame> history = MutleService.historyGames(games, entries(), 12);

        assertEquals(List.of(2), history.stream().map(MutleGame::gameId).toList());
    }

    @Test
    void detectsGameForRemovedFixture() {
        assertFalse(MutleService.fixtureAvailable(game(1, "removed/mutant", 12), entries()));
    }

    private static List<MutleEntry> entries() {
        return List.of(new MutleEntry(1, "fixture", "mutant"));
    }

    private static MutleGame game(int gameId, long rotationPeriod) {
        return game(gameId, "fixture/mutant", rotationPeriod);
    }

    private static MutleGame game(int gameId, String fixtureKey, long rotationPeriod) {
        return new MutleGame(gameId, fixtureKey, rotationPeriod, 7, null, null, Instant.EPOCH);
    }

    private static TargetExecution execution(TargetExecution.Target target, TargetExecution.Status status) {
        return new TargetExecution(1, 0, target, status, RAW_EXECUTION_OUTPUT);
    }
}
