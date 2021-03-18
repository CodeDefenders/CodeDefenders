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

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.dbutils.QueryRunner;
import org.codedefenders.database.ConnectionFactory;
import org.codedefenders.database.UserDAO;
import org.codedefenders.model.UserEntity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@ApplicationScoped
public class UserRepository {

    private final Cache<Integer, Integer> playerIdCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .recordStats()
            .build();

    private final ConnectionFactory connectionFactory;

    @Inject
    UserRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
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
            return playerIdCache.get(playerId, () -> {
                Integer userId = queryRunner.query(connectionFactory.getConnection(), query, resultSet -> {
                    if (resultSet.next()) {
                        return resultSet.getInt(1);
                    } else {
                        throw new SQLException();
                    }
                }, playerId);
                if (userId == null) {
                    throw new SQLException();
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
