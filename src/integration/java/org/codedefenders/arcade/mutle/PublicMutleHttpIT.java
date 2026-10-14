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
package org.codedefenders.arcade.mutle;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.codedefenders.arcade.mutle.persistence.MutleRepository;
import org.codedefenders.database.ConnectionFactory;
import org.codedefenders.persistence.database.util.TransactionAwareQueryRunner;
import org.codedefenders.transaction.TransactionalSupplier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.google.gson.JsonObject;

import static org.codedefenders.arcade.mutle.PublicMutleSupport.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Adversarial HTTP checks complement browser flows where raw uploads and quotas cannot be tested through the UI. */
@Tag("public-mutle")
@EnabledIfSystemProperty(named = "publicMutle.url", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PublicMutleHttpIT {
    @BeforeAll
    static void prepareFixture() throws Exception {
        prepare();
    }

    @Test
    void crossSessionAccessAndReload() throws Exception {
        Client first = new Client();
        Client second = new Client();
        int game = first.start();
        assertNotEquals(game, second.start());
        assertEquals(game, first.start());
        assertEquals(404, second.api("state", game(game)).statusCode());
        assertEquals(404, second.guess(game, PROOF).statusCode());
        assertEquals(404, second.page("/mutle/play?gameId=" + game, Duration.ofSeconds(5)).statusCode());
        JsonObject practice = new JsonObject();
        practice.addProperty("fixtureKey", "appointment-book/duplicate-name-allowed");
        assertEquals(403, first.api("start", practice).statusCode());
    }

    @Test
    void filesAndEnvironmentAreDeniedByActualRunner() throws Exception {
        Client client = new Client();
        int game = client.start();
        List<String> payloads = List.of(
                source("assertEquals(\"expected\", Files.readString(Paths.get(\"/tmp/mutle-review-canary.txt\")));",
                        "import java.nio.file.Files; import java.nio.file.Paths;"),
                source("assertNotNull(Files.writeString(Paths.get(\"/tmp/mutle-review-canary.txt\"), \"overwritten\"));",
                        "import java.nio.file.Files; import java.nio.file.Paths;"),
                source("assertEquals(\"expected\", getenv(\"CODEDEFENDERS_DB_PASSWORD\"));",
                        "import static java.lang.System.getenv;"));
        for (String payload : payloads) {
            payload = payload.replace("import static org.junit.Assert.*;",
                    "import static java.lang.System.out; import static org.junit.Assert.*;");
            payload = payload.replace("test() throws Exception {",
                    "test() throws Exception { out.println(\"TEST_BODY_ENTERED\");");
            var response = client.guess(game, payload);
            assertEquals(422, response.statusCode(), response.body());
            assertFalse(response.body().contains(CANARY));
            String message = json(response).get("message").getAsString();
            assertTrue(message.contains("AccessControlException"), message);
            assertTrue(message.contains("TEST_BODY_ENTERED"), message);
        }
        assertEquals(0, client.state(game).get("acceptedGuesses").getAsInt());
    }

    @Test
    void originsAndAgentControlsCannotForgeGuestResults() throws Exception {
        Client client = new Client();
        int game = client.start();
        for (String payload : RUNNER_ATTACKS) {
            var response = client.guess(game, payload);
            assertEquals(422, response.statusCode(), response.body());
            String message = json(response).get("message").getAsString();
            // The existing JaCoCo setup exposes its control API only inside the instrumented JVM.
            assertTrue(message.contains("SecurityException") || message.contains("AccessControlException")
                    || (payload.contains("org.jacoco.agent.rt.RT.getAgent()")
                        && message.contains("package org.jacoco.agent.rt does not exist")), response.body());
            var state = client.state(game);
            assertFalse(state.get("solved").getAsBoolean());
            assertEquals(0, state.get("acceptedGuesses").getAsInt());
        }
        var response = client.guess(game, BOOKING);
        assertEquals(200, response.statusCode(), response.body());
        var guess = json(response).getAsJsonArray("guesses").get(0).getAsJsonObject();
        assertEquals("LINE", guess.get("mark").getAsString());
        assertTrue(guess.getAsJsonArray("coveredLines").size() > 0);
        assertFalse(json(response).get("solved").getAsBoolean());
        assertTrue(json(client.guess(game, PROOF)).get("solved").getAsBoolean());
    }

    @Test
    void germanValidationAndSizeErrors() throws Exception {
        Client client = new Client("de");
        var homepage = client.page("/", Duration.ofSeconds(5));
        assertEquals(200, homepage.statusCode(), homepage.body());
        assertTrue(homepage.body().contains("Mutle spielen"), "Homepage must offer Mutle in German");
        int game = client.start();
        var validation = client.guess(game, source("for (int i=0; i<1; i++) { assertEquals(0, i); }", ""));
        assertEquals(422, validation.statusCode(), validation.body());
        assertTrue(json(validation).get("message").getAsString().contains("Schleifen"));
        var size = client.guess(game, "a".repeat(16_385));
        assertEquals(413, size.statusCode(), size.body());
        assertTrue(json(size).get("message").getAsString().contains("kürze"));
        assertEquals(413, client.guess(game, "a".repeat(33_000)).statusCode());
    }

    @Test
    void normalFeedbackSolveAndSixGuessFailure() throws Exception {
        Client client = new Client();
        int game = client.start();
        var spoof = client.guess(game, source("out.println(\"[junit] Tests run: 1, Failures: 0, Errors: 0, "
                + "Skipped: 0, Time elapsed: 0 sec\"); assertEquals(3, 2);", "import static java.lang.System.out;"));
        assertEquals(422, spoof.statusCode(), spoof.body());
        assertTrue(json(spoof).get("message").getAsString().contains("AssertionFailedError"));
        assertEquals(0, client.state(game).get("acceptedGuesses").getAsInt());
        var response = client.guess(game, NONE);
        assertEquals(200, response.statusCode(), response.body());
        JsonObject state = json(response);
        assertEquals(1, state.get("acceptedGuesses").getAsInt());
        assertEquals("NONE", state.getAsJsonArray("guesses").get(0).getAsJsonObject().get("mark").getAsString());
        response = client.guess(game, PROOF);
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(json(response).get("solved").getAsBoolean());
        assertFalse(json(response).get("reveal").isJsonNull());
        assertEquals(2, json(client.api("start", new JsonObject())).get("acceptedGuesses").getAsInt());
        assertEquals(400, client.guess(game, PROOF).statusCode());
        Client loser = new Client();
        int failedGame = loser.start();
        for (int attempt = 1; attempt <= 6; attempt++) {
            response = loser.guess(failedGame, NONE);
            assertEquals(200, response.statusCode(), response.body());
            assertEquals(attempt, json(response).get("acceptedGuesses").getAsInt());
        }
        assertTrue(json(response).get("failed").getAsBoolean());
        assertFalse(json(response).get("reveal").isJsonNull());
    }

    @Test
    void rejectedTestsHaveSeparateExecutionBudget() throws Exception {
        Client client = new Client();
        int game = client.start();
        for (int attempt = 0; attempt < 30; attempt++) {
            assertEquals(422, client.guess(game, "invalid source").statusCode());
        }
        var response = client.guess(game, PROOF);
        assertEquals(429, response.statusCode());
        assertEquals("game-limit", json(response).get("error").getAsString());
        assertEquals(0, client.state(game).get("acceptedGuesses").getAsInt());
    }

    @Test
    void timeoutAndMemoryLimitRejectOriginalTestsWithoutAcceptingThem() throws Exception {
        Client client = new Client();
        int game = client.start();
        for (String payload : List.of(source("SECONDS.sleep(20); "
                        + "assertEquals(0, new AppointmentBook(1,2).getBookingCount());",
                        "import static java.util.concurrent.TimeUnit.SECONDS;"),
                source("assertNotNull(new byte[200_000_000]);", ""))) {
            var response = client.guess(game, payload);
            assertEquals(422, response.statusCode(), response.body());
            assertEquals(0, client.state(game).get("acceptedGuesses").getAsInt());
        }
        var response = client.guess(game, NONE);
        assertEquals(200, response.statusCode(), response.body());
        assertFalse(json(response).get("solved").getAsBoolean());
    }

    @Test
    void mutantStageResourceFailureKillsMutantAndConsumesGuess() throws Exception {
        for (String payload : List.of(
                source("AppointmentBook book = new AppointmentBook(1,2); book.book(\"A\",1); "
                        + "SECONDS.sleep(20 * Boolean.compare(book.book(\"A\",2), false)); assertNotNull(book);",
                        "import static java.util.concurrent.TimeUnit.SECONDS;"),
                source("AppointmentBook book = new AppointmentBook(1,2); book.book(\"A\",1); "
                        + "assertNotNull(new byte[200_000_000 * Boolean.compare(book.book(\"A\",2), false)]);", ""))) {
            Client client = new Client();
            int game = client.start();
            var response = client.guess(game, payload);
            assertEquals(200, response.statusCode(), response.body());
            JsonObject state = client.state(game);
            assertEquals(1, state.get("acceptedGuesses").getAsInt());
            assertTrue(state.get("solved").getAsBoolean());
            assertFalse(state.get("failed").getAsBoolean());
        }
    }

    @Test
    void runningGuestTestDoesNotBlockPageReadsThroughCdi() throws Exception {
        Client client = new Client();
        Client other = new Client();
        int game = client.start();
        other.start();
        var executor = Executors.newSingleThreadExecutor();
        try {
            var pending = executor.submit(() -> client.guess(game, source("SECONDS.sleep(20); "
                    + "assertNotNull(new AppointmentBook(1,2));", "import static java.util.concurrent.TimeUnit.SECONDS;")));
            Thread.sleep(2_000);
            assertFalse(pending.isDone(), "The guest test must still be executing during page reads");
            assertEquals(200, client.page("/mutle", Duration.ofSeconds(2)).statusCode());
            assertEquals(200, other.page("/mutle", Duration.ofSeconds(2)).statusCode());
            assertEquals(422, pending.get(60, TimeUnit.SECONDS).statusCode());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void incompleteRequestBodiesDoNotOccupyGuestWorkers() throws Exception {
        URI base = URI.create(BASE);
        assertEquals("http", base.getScheme(), "Raw socket check requires the disposable HTTP instance");
        List<Socket> sockets = new ArrayList<>();
        try {
            for (int upload = 0; upload < 2; upload++) {
                Socket socket = new Socket(base.getHost(), base.getPort() == -1 ? 80 : base.getPort());
                sockets.add(socket);
                socket.getOutputStream().write(("POST " + base.getPath() + "/api/mutle/start HTTP/1.1\r\n"
                        + "Host: " + base.getAuthority() + "\r\nContent-Type: application/json\r\n"
                        + "X-Requested-With: XMLHttpRequest\r\nContent-Length: 100\r\n"
                        + "Connection: close\r\n\r\n{").getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().flush();
            }
            Thread.sleep(500);
            assertTrue(new Client().start() > 0);
        } finally {
            for (Socket socket : sockets) {
                socket.close();
            }
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "publicMutle.jdbcUrl", matches = ".+")
    void cleanupRollsBackAllDependenciesOnFailureAndCommitsOnRetry() throws Exception {
        Client client = new Client();
        int game = client.start();
        assertEquals(200, client.guess(game, NONE).statusCode());
        ConnectionFactory connections = mock(ConnectionFactory.class);
        when(connections.getConnection()).thenAnswer(invocation -> database());
        TransactionAwareQueryRunner queries = new TransactionAwareQueryRunner(connections);
        MutleRepository repository = new MutleRepository(queries);
        queries.update("UPDATE mutle_game SET created_at = ? WHERE game_id = ? AND user_id = 8",
                Timestamp.from(Instant.now().minus(Duration.ofDays(9))), game);
        int testsBefore = count(queries, "tests", "Game_ID", game);
        int executionsBefore = executions(queries, game);
        assertTrue(testsBefore > 0);
        assertTrue(executionsBefore > 0);
        Instant cutoff = Instant.now().minus(Duration.ofDays(7));
        assertThrows(IOException.class, () -> queries.executeInTransaction((TransactionalSupplier<Boolean>) tx -> {
            assertTrue(repository.deleteExpiredGuestGame(game, cutoff));
            assertEquals(0, count(queries, "games", "ID", game));
            throw new IOException("artifact deletion failed");
        }));
        assertEquals(1, count(queries, "games", "ID", game));
        assertEquals(testsBefore, count(queries, "tests", "Game_ID", game));
        assertEquals(executionsBefore, executions(queries, game));
        queries.executeInTransaction((TransactionalSupplier<Boolean>) tx -> {
            boolean deleted = repository.deleteExpiredGuestGame(game, cutoff);
            tx.commit();
            return deleted;
        });
        assertEquals(0, count(queries, "games", "ID", game));
        assertEquals(0, count(queries, "tests", "Game_ID", game));
        assertEquals(0, count(queries, "mutle_game", "game_id", game));
    }

    private static int executions(TransactionAwareQueryRunner queries, int game) {
        return queries.query("SELECT COUNT(*) FROM targetexecutions WHERE Test_ID IN "
                + "(SELECT Test_ID FROM tests WHERE Game_ID = ?)", rs -> { rs.next(); return rs.getInt(1); }, game);
    }

    private static int count(TransactionAwareQueryRunner queries, String table, String column, int game) {
        return queries.query("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?",
                rs -> { rs.next(); return rs.getInt(1); }, game);
    }

    @Test
    @Order(Integer.MAX_VALUE)
    void cookieChurnCannotBypassCreationQuota() throws Exception {
        for (int cookie = 0; cookie < 41; cookie++) {
            var response = new Client().api("start", new JsonObject());
            if (response.statusCode() == 429) {
                assertEquals("rate-limit", json(response).get("error").getAsString());
                return;
            }
            assertEquals(200, response.statusCode(), response.body());
        }
        fail("Creation quota was bypassed by changing cookies");
    }
}
