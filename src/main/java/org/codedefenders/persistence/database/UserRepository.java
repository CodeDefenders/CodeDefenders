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
package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.instrumentation.MetricsRegistry;
import org.codedefenders.model.KeyMap;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.transaction.Transactional;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import static org.codedefenders.persistence.database.util.ResultSetUtils.generatedKeyFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.oneFromRS;

/**
 * Provides methods for querying and updating the {@code users} table in the database.
 */
@Transactional
@ApplicationScoped
public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    private final LoadingCache<Integer, Integer> userIdForPlayerIdCache;

    private final QueryRunner queryRunner;

    @Inject
    public UserRepository(QueryRunner queryRunner, MetricsRegistry metricsRegistry) {
        this.queryRunner = queryRunner;

        userIdForPlayerIdCache = CacheBuilder.newBuilder()
                .maximumSize(400)
                .recordStats()
                .build(
                        new CacheLoader<>() {
                            @Override
                            @Nonnull
                            public Integer load(@Nonnull Integer playerId) throws Exception {
                                return getUserIdForPlayerIdInternal(playerId)
                                        .orElseThrow(() -> new Exception("No userId found for given playerId"));
                            }
                        }
                );

        metricsRegistry.registerGuavaCache("userIdForPlayerId", userIdForPlayerIdCache);
    }

    /**
     * Maps a result set from the {@code users} table to a {@link UserEntity} objet.
     *
     * @param rs The result set to map.
     * @return A fully constructed {@code UserEntity}
     * @throws SQLException if a {@code SQLException} occurs while accessing the {@code ResultSet}
     */
    public static UserEntity userFromRS(ResultSet rs) throws SQLException {
        int userId = rs.getInt("User_ID");
        String userName = rs.getString("Username");
        String password = rs.getString("Password");
        String email = rs.getString("Email");
        boolean validated = rs.getBoolean("Validated");
        boolean active = rs.getBoolean("Active");
        boolean allowContact = rs.getBoolean("AllowContact");
        KeyMap keyMap = KeyMap.valueOrDefault(rs.getString("KeyMap"));
        boolean keepPreviousTest = rs.getBoolean("KeepPreviousTest");

        return new UserEntity(userId, userName, password, email, validated, active, allowContact, keyMap,
                keepPreviousTest);
    }

    /**
     * Insert a new {@link UserEntity} into the database.
     *
     * <p>The {@code id} of the provided {@code userEntity} has to be 0.
     *
     * @param userEntity The new {@code UserEntity} to store in the database.
     * @return The id of the inserted {@code UserEntity} wrapped in an {@code Optional} or an empty optional if
     *         inserting the {@code userEntity} failed.
     * @throws IllegalArgumentException if {@code userEntity.id} is greater than 0.
     */
    // TODO: This gives no information why we couldn't insert the UserEntity into the database
    @Nonnull
    public Optional<Integer> insert(@Nonnull UserEntity userEntity) {
        if (userEntity.getId() > 0) {
            // TODO: Should we allow this?
            throw new IllegalArgumentException("Can't insert user with id > 0");
        }
        @Language("SQL") String query = """
                        INSERT INTO users (Username, Password, Email, Validated, Active, AllowContact, KeyMap, KeepPreviousTest)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?);
        """;

        return queryRunner
                .insert(query, generatedKeyFromRS(),
                        userEntity.getUsername(),
                        userEntity.getEncodedPassword(),
                        userEntity.getEmail(),
                        userEntity.isValidated(),
                        userEntity.isActive(),
                        userEntity.getAllowContact(),
                        userEntity.getKeyMap().name(),
                        userEntity.getKeepPreviousTest());
    }

    /**
     * Update the given {@code UserEntity} in the database.
     *
     * @param userEntity The {@code UserEntity} to update
     * @return Whether updating the provided {@code UserEntity} was successful or not.
     */
    public boolean update(@Nonnull UserEntity userEntity) {
        @Language("SQL") String query = """
                UPDATE users
                SET Username = ?,
                  Email = ?,
                  Password = ?,
                  Validated = ?,
                  Active = ?,
                  AllowContact = ?,
                          KeyMap = ?,
                          KeepPreviousTest = ?
                WHERE User_ID = ?;
        """;

        return queryRunner.update(query,
                userEntity.getUsername(),
                userEntity.getEmail(),
                userEntity.getEncodedPassword(),
                userEntity.isValidated(),
                userEntity.isActive(),
                userEntity.getAllowContact(),
                userEntity.getKeyMap().name(),
                userEntity.getKeepPreviousTest(),
                userEntity.getId()) == 1;
    }

    /**
     * Retrieve an {@code UserEntity} for a given {@code userId} from the database.
     */
    @Nonnull
    public Optional<UserEntity> getUserById(int userId) {
        @Language("SQL") String query = """
                SELECT *
                FROM  users
                WHERE User_ID = ?;
        """;

        return queryRunner.query(query, oneFromRS(UserRepository::userFromRS), userId);
    }

    /**
     * Retrieve an {@code UserEntity} identified by the given {@code username} from the database.
     */
    @Nonnull
    public Optional<UserEntity> getUserByName(@Nonnull String username) {
        @Language("SQL") String query = """
                SELECT *
                FROM  users
                WHERE Username = ?;
        """;

        return queryRunner.query(query, oneFromRS(UserRepository::userFromRS), username);
    }

    /**
     * Retrieve an {@code UserEntity} identified by the given {@code email} from the database.
     */
    @Nonnull
    public Optional<UserEntity> getUserByEmail(@Nonnull String email) {
        @Language("SQL") String query = """
                SELECT *
                FROM  users
                WHERE Email = ?;
        """;

        return queryRunner.query(query, oneFromRS(UserRepository::userFromRS), email);
    }

    // TODO: Relocate into sth like `PlayerRepository`
    /**
     * Retrieve the id of the user which corresponds to the player identified by the given {@code playerId}.
     */
    @Nonnull
    public Optional<Integer> getUserIdForPlayerId(int playerId) {
        try {
            return Optional.of(userIdForPlayerIdCache.get(playerId));
        } catch (ExecutionException e) {
            return Optional.empty();
        }
    }

    @Nonnull
    Optional<Integer> getUserIdForPlayerIdInternal(int playerId) {
        @Language("SQL") String query = """
                SELECT users.User_ID AS User_ID
                FROM users, players
                WHERE players.User_ID = users.User_ID
                  AND players.ID = ?;
        """;

        return queryRunner.query(query, oneFromRS(rs -> rs.getInt("User_ID")), playerId);
    }

    /**
     * Retrieve a list of all users from the database.
     *
     * <p>This list includes system users.
     */
    @Nonnull
    public List<UserEntity> getUsers() {
        @Language("SQL") String query = "SELECT * FROM  users;";
        return queryRunner.query(query, listFromRS(UserRepository::userFromRS));
    }

    @Nonnull
    public List<UserEntity> getAssignedUsers() {
        @Language("SQL") String query = """
                SELECT DISTINCT users.*
                FROM view_valid_users users
                LEFT JOIN players on players.User_ID = users.User_ID
                LEFT JOIN games on games.ID = players.Game_ID
                WHERE games.Mode <> 'PUZZLE'
                  AND (games.State = 'ACTIVE' OR games.State = 'CREATED')
                  AND players.Role IN ('ATTACKER', 'DEFENDER')
                  AND users.Active = TRUE
                ORDER BY User_ID;
        """;

        return queryRunner.query(query, listFromRS(UserRepository::userFromRS));
    }

    @Nonnull
    public List<UserEntity> getAssignedUsersForClassroom(int classroomId) {
        @Language("SQL") String query = """
                SELECT DISTINCT users.*
                FROM view_valid_users users
                LEFT JOIN players on players.User_ID = users.User_ID
                LEFT JOIN games on games.ID = players.Game_ID
                WHERE games.Mode <> 'PUZZLE'
                  AND (games.State = 'ACTIVE' OR games.State = 'CREATED')
                  AND players.Role IN ('ATTACKER', 'DEFENDER')
                  AND users.Active = TRUE
                  AND games.Classroom_ID = ?
                ORDER BY User_ID;
        """;

        return queryRunner.query(query, listFromRS(UserRepository::userFromRS), classroomId);
    }

    public void insertSession(int userId, String ipAddress) {
        @Language("SQL") String query = "INSERT INTO sessions (User_ID, IP_Address) VALUES (?, ?);";
        queryRunner.update(query, userId, ipAddress);
    }

    public void deleteSessions(int userId) {
        @Language("SQL") String query = "DELETE FROM sessions WHERE User_ID = ?;";
        queryRunner.update(query, userId);
    }

    public void setPasswordResetSecret(int userId, @Nullable String passwordResetSecret) {
        @Language("SQL") String query = """
                UPDATE users
                SET pw_reset_secret = ?,
                    pw_reset_timestamp = CURRENT_TIMESTAMP
                WHERE User_ID = ?;
        """;

        queryRunner.update(query, passwordResetSecret, userId);
    }

    @Nonnull
    public Optional<Integer> getUserIdForPasswordResetSecret(@Nullable String passwordResetSecret) {
        @Language("SQL") String query = """
                SELECT User_ID
                FROM users
                WHERE TIMESTAMPDIFF(HOUR, pw_reset_timestamp, CURRENT_TIMESTAMP) < (
                         SELECT INT_VALUE
                         FROM settings
                         WHERE name = 'PASSWORD_RESET_SECRET_LIFESPAN'
                ) AND pw_reset_secret = ?;
        """;

        return queryRunner.query(query, oneFromRS(rs -> rs.getInt("User_ID")), passwordResetSecret);
    }
}
