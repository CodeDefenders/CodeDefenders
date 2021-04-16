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

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.codedefenders.database.ConnectionFactory;
import org.codedefenders.database.UserDAO;
import org.codedefenders.model.UserEntity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@ApplicationScoped
public class UserRepository {

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

    public UserEntity getUserById(int userId) {
        return UserDAO.getUserById(userId);
    }

    public UserEntity getUserByName(String name) {
        return UserDAO.getUserByName(name);
    }

    public UserEntity getUserByEmail(String email) {
        return UserDAO.getUserByEmail(email);
    }

    public Integer getUserIdForPlayerId(int playerId) {
        QueryRunner queryRunner = new QueryRunner();
        String query = "SELECT users.User_ID AS User_ID "
                + "FROM users, players "
                + "WHERE players.User_ID = users.User_ID "
                + "AND players.ID = ?";

        try {
            // If the key wasn't in the "easy to compute" group, we need to
            // do things the hard way.
            return userIdForPlayerIdCache.get(playerId, () -> {
                Integer userId;
                try (Connection conn = connectionFactory.getConnection()) {
                    userId = queryRunner.query(conn, query, new ScalarHandler<>(), playerId);
                }
                if (userId == null) {
                    throw new Exception();
                } else {
                    return userId;
                }
            });
        } catch (ExecutionException e) {
            return null;
        }
    }

    public List<UserEntity> getUsers() {
        return UserDAO.getUsers();
    }

    public List<UserEntity> getUnassignedUsers() {
        return UserDAO.getUnassignedUsers();
    }
}
