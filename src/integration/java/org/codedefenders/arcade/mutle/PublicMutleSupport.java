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

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Shared fixtures and independent visitor sessions for the live Mutle integration checks. */
final class PublicMutleSupport {
    static final String BASE = System.getProperty("publicMutle.url", "").replaceAll("/$", "");
    static final String CANARY = "mutle-review-canary-20261011";
    static final String NONE = source("assertEquals(0, new AppointmentBook(1, 2).getBookingCount());", "");
    static final String PROOF = source("AppointmentBook book = new AppointmentBook(1, 2); "
            + "book.book(\"A\", 1); assertFalse(book.book(\"A\", 2));", "");
    private static final String BOOKING_STATEMENTS = "AppointmentBook book = new AppointmentBook(1, 2); "
            + "assertTrue(book.book(\"A\", 1));";
    static final String BOOKING = source(BOOKING_STATEMENTS, "");
    static final List<String> RUNNER_ATTACKS = List.of(
            source("assertTrue(AppointmentBook.class.getProtectionDomain().getCodeSource()"
                    + ".getLocation().toString().endsWith(\"/cut/\"));", ""),
            source(BOOKING_STATEMENTS + " org.jacoco.agent.rt.RT.getAgent().reset();", ""),
            source("Class.forName(\"org.jacoco.agent.rt.RT\").getMethod(\"getAgent\").invoke(null);", ""));
    private static final Gson GSON = new Gson();

    private PublicMutleSupport() {
    }

    static String source(String statement, String imports) {
        return "import static org.junit.Assert.*; import org.junit.Test; " + imports
                + " public class TestAppointmentBook { @Test public void test() throws Exception { "
                + statement + " } }";
    }

    static Connection database() throws Exception {
        return DriverManager.getConnection(System.getProperty("publicMutle.jdbcUrl"),
                System.getProperty("publicMutle.dbUsername", "root"),
                System.getProperty("publicMutle.dbPassword", ""));
    }

    /** Only the disposable CI instance is seeded; an existing preview is never reset or republished. */
    static void prepare() throws Exception {
        if (!Boolean.getBoolean("publicMutle.seed")) {
            return;
        }
        try (var connection = database(); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT IGNORE INTO mutle_publication (series, fixture_key, release_at) "
                    + "VALUES (1, 'appointment-book/duplicate-name-allowed', '2020-01-01 00:00:00')");
            statement.executeUpdate("UPDATE settings SET STRING_VALUE = 'en,de' WHERE name = 'SUPPORTED_LANGUAGES'");
            try (var user = connection.prepareStatement("INSERT IGNORE INTO users (Username, Password, Email) "
                    + "VALUES (?, ?, ?)")) {
                user.setString(1, "public-mutle-it");
                user.setString(2, new BCryptPasswordEncoder()
                        .encode("public-mutle-it"));
                user.setString(3, "public-mutle-it@codedefenders.invalid");
                user.executeUpdate();
            }
        }
    }

    static final class Client {
        private final String language;
        private final HttpClient http = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(5)).build();

        Client() {
            this("en");
        }

        Client(String language) {
            this.language = language;
        }

        HttpResponse<String> request(String path, JsonObject body, Duration timeout) throws Exception {
            var request = HttpRequest.newBuilder(URI.create(BASE + path)).timeout(timeout)
                    .header("Accept-Language", language);
            if (body != null) {
                request.header("Content-Type", "application/json").header("X-Requested-With", "XMLHttpRequest")
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)));
            }
            return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> page(String path, Duration timeout) throws Exception {
            return request(path, null, timeout);
        }

        HttpResponse<String> api(String action, JsonObject body) throws Exception {
            return request("/api/mutle/" + action, body, Duration.ofSeconds(60));
        }

        int start() throws Exception {
            var response = api("start", new JsonObject());
            assertEquals(200, response.statusCode(), response.body());
            JsonObject state = json(response);
            assertEquals("Appointment Book", state.get("title").getAsString(), "Publish the Appointment Book fixture");
            return state.get("gameId").getAsInt();
        }

        HttpResponse<String> guess(int game, String source) throws Exception {
            JsonObject body = game(game);
            body.addProperty("source", source);
            return api("guess", body);
        }

        JsonObject state(int game) throws Exception {
            var response = api("state", game(game));
            assertEquals(200, response.statusCode(), response.body());
            return json(response);
        }
    }

    static JsonObject game(int id) {
        JsonObject body = new JsonObject();
        body.addProperty("gameId", id);
        return body;
    }

    static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
