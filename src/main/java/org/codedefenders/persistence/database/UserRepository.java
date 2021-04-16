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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.codedefenders.database.ConnectionFactory;
import org.codedefenders.model.KeyMap;
import org.codedefenders.model.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@ApplicationScoped
public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    private final Cache<Integer, Integer> userIdForPlayerIdCache;

    private final ConnectionFactory connectionFactory;

    @Inject
    public UserRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;

        userIdForPlayerIdCache = CacheBuilder.newBuilder()
                .maximumSize(400)
                //.recordStats() // Nice to have for dev, unnecessary for production  without properly exposing it
                .build();
    }

    // TODO extract to utility class?!
    // TODO does common-dbutils already offers something like this?!
    public static <T> T nextFromRS(ResultSet rs, ResultSetHandler<T> handler) throws SQLException {
        if (rs.next()) {
            return handler.handle(rs);
        } else {
            return null;
        }
    }

    // TODO extract to utility class?!
    // TODO does common-dbutils already offers something like this?!
    public static <T> List<T> listFromRS(ResultSet rs, ResultSetHandler<T> handler) throws SQLException {
        List<T> result = new ArrayList<>();
        while (rs.next()) {
            result.add(handler.handle(rs));
        }
        return result;
    }

    public static UserEntity userFromRS(ResultSet rs) throws SQLException {
        int userId = rs.getInt("User_ID");
        String password = rs.getString("Password");
        String userName = rs.getString("Username");
        String email = rs.getString("Email");
        boolean validated = rs.getBoolean("Validated");
        boolean active = rs.getBoolean("Active");
        boolean allowContact = rs.getBoolean("AllowContact");
        KeyMap keyMap = KeyMap.valueOrDefault(rs.getString("KeyMap"));

        return new UserEntity(userId, userName, password, email, validated, active, allowContact, keyMap);
    }


    public UserEntity getUserById(int userId) {
        String query = "SELECT * "
                + "FROM  users "
                + "WHERE User_ID = ?;";
        try {
            return connectionFactory.getQueryRunner().query(query, resultSet -> nextFromRS(resultSet,UserRepository::userFromRS), userId);
        } catch (SQLException e) {
            logger.error("SQLException while loading user", e);
            return null;
        }
    }

    public UserEntity getUserByName(String username) {
        String query = "SELECT * "
                + "FROM  users "
                + "WHERE Username = ?;";
        try {
            return connectionFactory.getQueryRunner().query(query, resultSet -> nextFromRS(resultSet, UserRepository::userFromRS), username);
        } catch (SQLException e) {
            logger.error("SQLException", e);
            return null;
        }
    }

    public UserEntity getUserByEmail(String email) {
        String query = "SELECT * "
                + "FROM  users "
                + "WHERE Email = ?;";
        try {
            return connectionFactory.getQueryRunner().query(query, resultSet -> nextFromRS(resultSet ,UserRepository::userFromRS), email);
        } catch (SQLException e) {
            logger.error("SQLException", e);
            return null;
        }
    }

    public Integer getUserIdForPlayerId(int playerId) {
        String query = "SELECT users.User_ID AS User_ID "
                + "FROM users, players "
                + "WHERE players.User_ID = users.User_ID "
                + "AND players.ID = ?";

        try {
            // If the key wasn't in the "easy to compute" group, we need to
            // do things the hard way.
            return userIdForPlayerIdCache.get(playerId, () -> {
                Integer userId;
                userId = connectionFactory.getQueryRunner().query(query, new ScalarHandler<>(), playerId);
                if (userId == null) {
                    throw new Exception();
                } else {
                    return userId;
                }
            });
        } catch (ExecutionException e) {
            logger.error("SQLException", e);
            return null;
        }
    }

    public List<UserEntity> getUsers() {
        String query = "SELECT * "
                + "FROM  users;";
        try {
            return connectionFactory.getQueryRunner().query(query, resultSet -> listFromRS(resultSet, UserRepository::userFromRS));
        } catch (SQLException e) {
            logger.error("SQLException", e);
            return new ArrayList<>();
        }
    }

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
            logger.error("SQLException", e);
            return new ArrayList<>();
        }
    }
}
