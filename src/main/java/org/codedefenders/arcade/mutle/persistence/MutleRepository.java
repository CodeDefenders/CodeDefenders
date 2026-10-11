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
package org.codedefenders.arcade.mutle.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.arcade.mutle.model.MutleFixtureBundle;
import org.codedefenders.arcade.mutle.model.MutleFixtureInstallation;
import org.codedefenders.arcade.mutle.model.MutleGame;
import org.codedefenders.arcade.mutle.model.MutlePublication;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.intellij.lang.annotations.Language;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.oneFromRS;
import static org.codedefenders.util.Constants.MUTLE_GUEST_USER_ID;

/** Database access for installed Mutle fixtures and player games. */
@ApplicationScoped
public class MutleRepository {
    private static final Gson GSON = new Gson();
    private static final java.lang.reflect.Type RESOURCE_MAP = new TypeToken<Map<String, String>>() { }.getType();
    private final QueryRunner queryRunner;

    @Inject
    public MutleRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    public Optional<MutleFixtureInstallation> findFixture(String fixtureKey) {
        @Language("SQL") String query = """
                SELECT *
                FROM mutle_fixture
                WHERE fixture_key = ?;
        """;
        return queryRunner.query(query, oneFromRS(MutleRepository::fixtureFromRS), fixtureKey);
    }

    public List<MutleFixtureInstallation> findFixtures() {
        @Language("SQL") String query = """
                SELECT *
                FROM mutle_fixture
                ORDER BY fixture_key;
        """;
        return queryRunner.query(query, listFromRS(MutleRepository::fixtureFromRS));
    }

    public Optional<MutleFixtureBundle> findBundle(String classKey) {
        @Language("SQL") String query = """
                SELECT * FROM mutle_fixture_bundle WHERE class_key = ?;
        """;
        return queryRunner.query(query, oneFromRS(MutleRepository::bundleFromRS), classKey);
    }

    public List<MutleFixtureBundle> findBundles() {
        @Language("SQL") String query = """
                SELECT * FROM mutle_fixture_bundle ORDER BY class_key;
        """;
        return queryRunner.query(query, listFromRS(MutleRepository::bundleFromRS));
    }

    public void insertBundle(MutleFixtureBundle bundle) {
        @Language("SQL") String query = """
                INSERT INTO mutle_fixture_bundle
                    (class_key, class_json, resources_json, content_hash, installed_at)
                VALUES (?, ?, ?, ?, ?);
        """;
        int inserted = queryRunner.update(query, bundle.classKey(), bundle.classJson(),
                GSON.toJson(bundle.resources(), RESOURCE_MAP), bundle.contentHash(),
                Timestamp.from(bundle.installedAt()));
        if (inserted != 1) {
            throw new UncheckedSQLException("Couldn't store Mutle fixture bundle.");
        }
    }

    /** Installs a bundled fixture without failing on a concurrent duplicate insert. */
    public void insertBundleIfAbsent(MutleFixtureBundle bundle) {
        @Language("SQL") String query = """
                INSERT INTO mutle_fixture_bundle
                    (class_key, class_json, resources_json, content_hash, installed_at)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE class_key = class_key;
        """;
        queryRunner.update(query, bundle.classKey(), bundle.classJson(),
                GSON.toJson(bundle.resources(), RESOURCE_MAP), bundle.contentHash(),
                Timestamp.from(bundle.installedAt()));
    }

    public List<MutlePublication> findPublications() {
        @Language("SQL") String query = """
                SELECT series, fixture_key, release_at FROM mutle_publication ORDER BY series;
        """;
        return queryRunner.query(query, listFromRS(MutleRepository::publicationFromRS));
    }

    public Optional<MutlePublication> findPublication(long series) {
        @Language("SQL") String query = """
                SELECT series, fixture_key, release_at FROM mutle_publication WHERE series = ?;
        """;
        return queryRunner.query(query, oneFromRS(MutleRepository::publicationFromRS), series);
    }

    public Optional<MutlePublication> findCurrentPublication(Instant now) {
        @Language("SQL") String query = """
                SELECT series, fixture_key, release_at
                FROM mutle_publication
                WHERE release_at <= ?
                ORDER BY release_at DESC, series DESC
                LIMIT 1;
        """;
        return queryRunner.query(query, oneFromRS(MutleRepository::publicationFromRS), Timestamp.from(now));
    }

    public void insertPublication(MutlePublication publication) {
        @Language("SQL") String query = """
                INSERT INTO mutle_publication (series, fixture_key, release_at) VALUES (?, ?, ?);
        """;
        int inserted = queryRunner.update(query, publication.series(), publication.fixtureKey(),
                Timestamp.from(publication.releaseAt()));
        if (inserted != 1) {
            throw new UncheckedSQLException("Couldn't store Mutle publication.");
        }
    }

    public void updatePublication(MutlePublication publication) {
        @Language("SQL") String query = """
                UPDATE mutle_publication SET fixture_key = ?, release_at = ? WHERE series = ?;
        """;
        int updated = queryRunner.update(query, publication.fixtureKey(), Timestamp.from(publication.releaseAt()),
                publication.series());
        if (updated != 1) {
            throw new UncheckedSQLException("Couldn't update Mutle publication.");
        }
    }

    public int deleteFuturePublication(long series, Instant now) {
        @Language("SQL") String query = """
                DELETE FROM mutle_publication WHERE series = ? AND release_at > ?;
        """;
        return queryRunner.update(query, series, Timestamp.from(now));
    }

    public int deleteFuturePublications(Instant now) {
        @Language("SQL") String query = """
                DELETE FROM mutle_publication WHERE release_at > ?;
        """;
        return queryRunner.update(query, Timestamp.from(now));
    }

    public boolean hasGames() {
        @Language("SQL") String query = """
                SELECT EXISTS(
                    SELECT 1 FROM mutle_game WHERE rotation_period >= 0
                ) AS has_games;
        """;
        return queryRunner.query(query, oneFromRS(rs -> rs.getBoolean("has_games"))).orElse(false);
    }

    public void insertFixture(MutleFixtureInstallation fixture) {
        @Language("SQL") String query = """
                INSERT INTO mutle_fixture
                    (fixture_key, content_hash, hidden_puzzle_id, template_mutant_id, installed_at)
                VALUES (?, ?, ?, ?, ?);
        """;
        int inserted = queryRunner.update(query,
                fixture.fixtureKey(),
                fixture.contentHash(),
                fixture.hiddenPuzzleId(),
                fixture.templateMutantId(),
                Timestamp.from(fixture.installedAt()));
        if (inserted != 1) {
            throw new UncheckedSQLException("Couldn't store Mutle fixture.");
        }
    }

    public Optional<MutleGame> findGame(int userId, long rotationPeriod) {
        @Language("SQL") String query = """
                SELECT *
                FROM mutle_game
                WHERE user_id = ? AND rotation_period = ?;
        """;
        return queryRunner.query(query, oneFromRS(MutleRepository::gameFromRS), userId, rotationPeriod);
    }

    public Optional<MutleGame> findGame(int gameId) {
        @Language("SQL") String query = """
                SELECT *
                FROM mutle_game
                WHERE game_id = ?;
        """;
        return queryRunner.query(query, oneFromRS(MutleRepository::gameFromRS), gameId);
    }

    public List<MutleGame> findGamesForUser(int userId) {
        @Language("SQL") String query = """
                SELECT *
                FROM mutle_game
                WHERE user_id = ?
                ORDER BY created_at DESC, game_id DESC;
        """;
        return queryRunner.query(query, listFromRS(MutleRepository::gameFromRS), userId);
    }

    public void insertGame(MutleGame game) {
        @Language("SQL") String query = """
                INSERT INTO mutle_game
                    (game_id, fixture_key, rotation_period, user_id, submission_token, submission_started_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?);
        """;
        int inserted = queryRunner.update(query,
                game.gameId(),
                game.fixtureKey(),
                game.rotationPeriod(),
                game.userId(),
                game.submissionToken(),
                toTimestamp(game.submissionStartedAt()),
                Timestamp.from(game.createdAt()));
        if (inserted != 1) {
            throw new UncheckedSQLException("Couldn't store Mutle game.");
        }
    }

    /** Called inside game creation's transaction; the shared user row serializes the capacity check. */
    public void reserveGuestCapacity() {
        queryRunner.query("SELECT User_ID FROM users WHERE User_ID = ? FOR UPDATE;",
                oneFromRS(rs -> rs.getInt("User_ID")), MUTLE_GUEST_USER_ID).orElseThrow();
        int count = queryRunner.query("SELECT COUNT(*) AS total FROM mutle_game WHERE user_id = ?;",
                oneFromRS(rs -> rs.getInt("total")), MUTLE_GUEST_USER_ID).orElseThrow();
        if (count >= 2_000) {
            throw new org.codedefenders.arcade.mutle.service.MutleGuestLimits.RateLimitException();
        }
    }

    public List<Integer> expiredGuestGames(Instant cutoff) {
        @Language("SQL") String query = """
                SELECT game_id FROM mutle_game
                WHERE user_id = ? AND created_at < ?
                  AND (submission_token IS NULL OR submission_started_at < ?)
                ORDER BY created_at LIMIT 100;
                """;
        return queryRunner.query(query, listFromRS(rs -> rs.getInt("game_id")),
                MUTLE_GUEST_USER_ID, Timestamp.from(cutoff), Timestamp.from(Instant.now().minusSeconds(120)));
    }

    /** Delete dependencies explicitly because game/test foreign keys do not all cascade. Requires a transaction. */
    public boolean deleteExpiredGuestGame(int gameId, Instant cutoff) {
        @Language("SQL") String guard = """
                SELECT game_id FROM mutle_game
                WHERE game_id = ? AND user_id = ? AND created_at < ?
                  AND (submission_token IS NULL OR submission_started_at < ?)
                FOR UPDATE;
                """;
        if (queryRunner.query(guard, oneFromRS(rs -> rs.getInt("game_id")), gameId, MUTLE_GUEST_USER_ID,
                Timestamp.from(cutoff), Timestamp.from(Instant.now().minusSeconds(120))).isEmpty()) {
            return false;
        }
        // These tables can reference tests, mutants or players of a game even when their own game ID is absent.
        for (String query : List.of(
                "DELETE FROM llm_messages WHERE Conversation_ID IN (SELECT Conversation_ID FROM llm_conversations "
                        + "WHERE Test_ID IN (SELECT Test_ID FROM tests WHERE Game_ID = ?) "
                        + "OR Mutant_ID IN (SELECT Mutant_ID FROM mutants WHERE Game_ID = ?));",
                "DELETE FROM killmap WHERE Test_ID IN (SELECT Test_ID FROM tests WHERE Game_ID = ?) "
                        + "OR Mutant_ID IN (SELECT Mutant_ID FROM mutants WHERE Game_ID = ?);",
                "DELETE FROM targetexecutions WHERE Test_ID IN (SELECT Test_ID FROM tests WHERE Game_ID = ?) "
                        + "OR Mutant_ID IN (SELECT Mutant_ID FROM mutants WHERE Game_ID = ?);",
                "DELETE FROM intention WHERE Test_ID IN (SELECT Test_ID FROM tests WHERE Game_ID = ?) "
                        + "OR Mutant_ID IN (SELECT Mutant_ID FROM mutants WHERE Game_ID = ?);",
                "DELETE FROM llm_conversations WHERE Test_ID IN (SELECT Test_ID FROM tests WHERE Game_ID = ?) "
                        + "OR Mutant_ID IN (SELECT Mutant_ID FROM mutants WHERE Game_ID = ?);",
                "DELETE FROM equivalences WHERE Defender_ID IN (SELECT ID FROM players WHERE Game_ID = ?) "
                        + "OR Mutant_ID IN (SELECT Mutant_ID FROM mutants WHERE Game_ID = ?);")) {
            queryRunner.update(query, gameId, gameId);
        }
        for (String query : List.of(
                "DELETE FROM test_smell WHERE Test_ID IN (SELECT Test_ID FROM tests WHERE Game_ID = ?);",
                "DELETE FROM test_uploaded_with_class WHERE Test_ID IN (SELECT Test_ID FROM tests WHERE Game_ID = ?);",
                "DELETE FROM mutant_uploaded_with_class WHERE Mutant_ID IN (SELECT Mutant_ID FROM mutants WHERE Game_ID = ?);",
                "DELETE FROM equivalences WHERE Mutant_ID IN (SELECT Mutant_ID FROM mutants WHERE Game_ID = ?);",
                "DELETE FROM rejection_reasons WHERE Reject_ID IN (SELECT Reject_ID FROM rejected_submissions "
                        + "WHERE Player_ID IN (SELECT ID FROM players WHERE Game_ID = ?));",
                "DELETE FROM rejected_submissions WHERE Player_ID IN (SELECT ID FROM players WHERE Game_ID = ?);",
                "DELETE FROM killmap WHERE Game_ID = ?;",
                "DELETE FROM killmapjob WHERE Game_ID = ?;",
                "DELETE FROM intention WHERE Game_ID = ?;",
                "DELETE FROM llm_messages WHERE Conversation_ID IN (SELECT Conversation_ID FROM llm_conversations WHERE Game_ID = ?);",
                "DELETE FROM llm_conversations WHERE Game_ID = ?;",
                "DELETE FROM game_chat_messages WHERE Game_ID = ?;",
                "DELETE FROM events WHERE Game_ID = ?;",
                "DELETE FROM tests WHERE Game_ID = ?;",
                "DELETE FROM mutants WHERE Game_ID = ?;",
                "DELETE FROM players WHERE Game_ID = ?;",
                "DELETE FROM ratings WHERE Game_ID = ?;",
                "DELETE FROM whitelist WHERE Game_Id = ?;",
                "DELETE FROM invitation_links WHERE Game_Id = ?;",
                "DELETE FROM usedaitests WHERE Game_ID = ?;",
                "DELETE FROM usedaimutants WHERE Game_ID = ?;",
                "DELETE FROM games WHERE ID = ?;")) {
            queryRunner.update(query, gameId);
        }
        return true;
    }

    public void retireGame(int gameId, int userId) {
        @Language("SQL") String query = """
                UPDATE mutle_game
                SET rotation_period = ?
                WHERE game_id = ? AND user_id = ?;
        """;
        int updated = queryRunner.update(query, Long.MIN_VALUE + gameId, gameId, userId);
        if (updated != 1) {
            throw new UncheckedSQLException("Couldn't retire unavailable Mutle game.");
        }
    }

    public boolean isMutleGame(int gameId, int userId) {
        @Language("SQL") String query = """
                SELECT EXISTS(
                    SELECT 1 FROM mutle_game WHERE game_id = ? AND user_id = ?
                ) AS mutle_game;
        """;
        return queryRunner.query(query, oneFromRS(rs -> rs.getBoolean("mutle_game")), gameId, userId)
                .orElse(false);
    }

    public boolean reserveSubmission(int gameId, int userId, String token, Instant startedAt, Instant staleBefore) {
        @Language("SQL") String query = """
                UPDATE mutle_game
                SET submission_token = ?, submission_started_at = ?
                WHERE game_id = ?
                  AND user_id = ?
                  AND (submission_token IS NULL OR submission_started_at < ?);
        """;
        return queryRunner.update(query,
                token,
                Timestamp.from(startedAt),
                gameId,
                userId,
                Timestamp.from(staleBefore)) == 1;
    }

    public boolean clearSubmission(int gameId, int userId, String token) {
        @Language("SQL") String query = """
                UPDATE mutle_game
                SET submission_token = NULL, submission_started_at = NULL
                WHERE game_id = ? AND user_id = ? AND submission_token = ?;
        """;
        return queryRunner.update(query, gameId, userId, token) == 1;
    }

    private static MutleFixtureInstallation fixtureFromRS(ResultSet rs) throws SQLException {
        return new MutleFixtureInstallation(
                rs.getString("fixture_key"),
                rs.getString("content_hash"),
                rs.getInt("hidden_puzzle_id"),
                rs.getInt("template_mutant_id"),
                rs.getTimestamp("installed_at").toInstant());
    }

    private static MutleFixtureBundle bundleFromRS(ResultSet rs) throws SQLException {
        Map<String, String> resources = GSON.fromJson(rs.getString("resources_json"), RESOURCE_MAP);
        return new MutleFixtureBundle(rs.getString("class_key"), rs.getString("class_json"), resources,
                rs.getString("content_hash"), rs.getTimestamp("installed_at").toInstant());
    }

    private static MutlePublication publicationFromRS(ResultSet rs) throws SQLException {
        return new MutlePublication(rs.getLong("series"), rs.getString("fixture_key"),
                rs.getTimestamp("release_at").toInstant());
    }

    private static MutleGame gameFromRS(ResultSet rs) throws SQLException {
        Timestamp submissionStartedAt = rs.getTimestamp("submission_started_at");
        return new MutleGame(
                rs.getInt("game_id"),
                rs.getString("fixture_key"),
                rs.getLong("rotation_period"),
                rs.getInt("user_id"),
                rs.getString("submission_token"),
                submissionStartedAt == null ? null : submissionStartedAt.toInstant(),
                rs.getTimestamp("created_at").toInstant());
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
