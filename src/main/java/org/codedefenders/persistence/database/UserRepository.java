/*
 * Copyright (C) 2020 Code Defenders contributors
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

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.codedefenders.database.ConnectionFactory;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.model.KeyMap;
import org.codedefenders.model.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import static org.codedefenders.persistence.database.DatabaseUtils.listFromRS;
import static org.codedefenders.persistence.database.DatabaseUtils.nextFromRS;

/**
 * Provides methods for querying and updating the {@code users} table in the database.
 */
@ApplicationScoped
public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    private final LoadingCache<Integer, Integer> userIdForPlayerIdCache;

    private final ConnectionFactory connectionFactory;

    @Inject
    public UserRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;

        userIdForPlayerIdCache = CacheBuilder.newBuilder()
                .maximumSize(400)
                //.recordStats() // Nice to have for dev, unnecessary for production  without properly exposing it
                .build(
                        new CacheLoader<Integer, Integer>() {
                            @Override
                            public Integer load(@Nonnull Integer playerId) throws Exception {
                                return getUserIdForPlayerIdInternal(playerId)
                                        .orElseThrow(() -> new Exception("No userId found for given playerId"));
                            }
                        }
                );
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

        return new UserEntity(userId, userName, password, email, validated, active, allowContact, keyMap);
    }

    /**
     * Insert a new {@link UserEntity} into the database.
     *
     * <p>The {@code id} of the provided {@code userEntity} has to be 0.
     *
     * @param userEntity The new {@code UserEntity} to store in the database.
     * @return The id of the inserted {@code UserEntity} wrapped in an {@code Optional} or an empty optional if
     * inserting the {@code userEntity} failed.
     * @throws IllegalArgumentException if {@code userEntity.id} is greater then 0.
     */
    // TODO: This gives no information why we couldn't insert the UserEntity into the database
    public Optional<Integer> insert(UserEntity userEntity) {
        if (userEntity.getId() > 0) {
            // TODO: Should we allow this?
            throw new IllegalArgumentException("Can't insert user with id > 0");
        }
        String query = "INSERT INTO users "
                + "(Username, Password, Email, Validated, Active, AllowContact, KeyMap) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?);";
        try {
            return connectionFactory.getQueryRunner()
                    .insert(query, resultSet -> nextFromRS(resultSet, rs -> rs.getInt(1)),
                            userEntity.getUsername(),
                            userEntity.getEncodedPassword(),
                            userEntity.getEmail(),
                            userEntity.isValidated(),
                            userEntity.isActive(),
                            userEntity.getAllowContact(),
                            userEntity.getKeyMap().name());
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Update the given {@code UserEntity} in the database.
     *
     * @param userEntity The {@code UserEntity} to update
     * @return Whether updating the provided {@code UserEntity} was successful or not.
     */
    public boolean update(UserEntity userEntity) {
        String query = "UPDATE users "
                + "SET Username = ?, "
                + "  Email = ?, "
                + "  Password = ?, "
                + "  Validated = ?, "
                + "  Active = ?, "
                + "  AllowContact = ?, "
                + "  KeyMap = ?"
                + "WHERE User_ID = ?;";
        try {
            return connectionFactory.getQueryRunner().update(query,
                    userEntity.getUsername(),
                    userEntity.getEmail(),
                    userEntity.getEncodedPassword(),
                    userEntity.isValidated(),
                    userEntity.isActive(),
                    userEntity.getAllowContact(),
                    userEntity.getKeyMap().name(),
                    userEntity.getId()) == 1;
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Retrieve an {@code UserEntity} for a given {@code userId} from the database.
     */
    public Optional<UserEntity> getUserById(int userId) {
        String query = "SELECT * "
                + "FROM  users "
                + "WHERE User_ID = ?;";
        try {
            return connectionFactory.getQueryRunner().query(query, resultSet -> nextFromRS(resultSet, UserRepository::userFromRS), userId);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Retrieve an {@code UserEntity} identified by the given {@code username} from the database.
     */
    public Optional<UserEntity> getUserByName(String username) {
        String query = "SELECT * "
                + "FROM  users "
                + "WHERE Username = ?;";
        try {
            return connectionFactory.getQueryRunner().query(query, resultSet -> nextFromRS(resultSet, UserRepository::userFromRS), username);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Retrieve an {@code UserEntity} identified by the given {@code email} from the database.
     */
    public Optional<UserEntity> getUserByEmail(String email) {
        String query = "SELECT * "
                + "FROM  users "
                + "WHERE Email = ?;";
        try {
            return connectionFactory.getQueryRunner().query(query, resultSet -> nextFromRS(resultSet, UserRepository::userFromRS), email);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    // TODO: Relocate into sth like `PlayerRepository`
    /**
     * Retrieve the id of the user which corresponds to the player identified by the given {@code playerId}.
     */
    public Optional<Integer> getUserIdForPlayerId(int playerId) {
        try {
            return Optional.of(userIdForPlayerIdCache.get(playerId));
        } catch (ExecutionException e) {
            return Optional.empty();
        }
    }

    @Nonnull
    Optional<Integer> getUserIdForPlayerIdInternal(int playerId) {
        Integer userId;
        String query = "SELECT users.User_ID AS User_ID "
                + "FROM users, players "
                + "WHERE players.User_ID = users.User_ID "
                + "AND players.ID = ?";
        try {
            userId = connectionFactory.getQueryRunner().query(query, new ScalarHandler<>(), playerId);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
        return Optional.ofNullable(userId);
    }

    /**
     * Retrieve a list of all users from the database.
     *
     * <p>This list includes system users.
     */
    public List<UserEntity> getUsers() {
        String query = "SELECT * "
                + "FROM  users;";
        try {
            return connectionFactory.getQueryRunner().query(query, resultSet -> listFromRS(resultSet, UserRepository::userFromRS));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Retrieve a list of unassigned users from the database.
     *
     * <p>This list includes neither system nor inactive users
     */
    public List<UserEntity> getUnassignedUsers() {
        String query = "SELECT DISTINCT u.* "
                + "FROM view_valid_users u "
                + "WHERE u.User_ID NOT IN"
                + "    ("
                + "      SELECT DISTINCT players.User_ID"
                + "      FROM players, games"
                + "      WHERE players.Game_ID = games.ID"
                + "        AND games.Mode <> 'PUZZLE'"
                + "        AND (games.State = 'ACTIVE' OR games.State = 'CREATED')"
                + "        AND players.Role IN ('ATTACKER', 'DEFENDER')"
                + "        AND Active = TRUE"
                + "    )"
                + "ORDER BY Username, User_ID;";
        try {
            return connectionFactory.getQueryRunner().query(query, resultSet -> listFromRS(resultSet, UserRepository::userFromRS));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }
}
