/*
 * Copyright (C) 2021 Code Defenders contributors
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

package org.codedefenders.service;

import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.User;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.UserRepository;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@ApplicationScoped
public class UserService {

    // TODO: This could use some tuning
    private final Cache<Integer, SimpleUser> simpleUserForUserIdCache;

    private final UserRepository userRepo;

    @Inject
    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;

        simpleUserForUserIdCache = CacheBuilder.newBuilder()
                .maximumSize(200)
                .recordStats()
                .build();
    }

    public User getUserById(int userId) {
        UserEntity user = userRepo.getUserById(userId);
        if (user != null) {
            return userFromUserEntity(user);
        } else {
            return null;
        }
    }

    public SimpleUser getSimpleUserById(final int userId) {
        try {
            // If the key wasn't in the "easy to compute" group, we need to
            // do things the hard way.
            return simpleUserForUserIdCache.get(userId, () -> {
                UserEntity user = userRepo.getUserById(userId);
                if (user == null) {
                    throw new Exception();
                } else {
                    return simpleUserFromUserEntity(user);
                }
            });
        } catch (ExecutionException e) {
            return null;
        }

    }

    public SimpleUser getSimpleUserByPlayerId(final int playerId) {
        Integer userId = userRepo.getUserIdForPlayerId(playerId);
        if (userId == null) {
            return null;
        } else {
            return getSimpleUserById(userId);
        }
    }

    private User userFromUserEntity(UserEntity user) {
        return new User(user.getId(), user.getUsername(), user.isActive(), user.getEmail(), user.isValidated(),
                user.getAllowContact(), user.getKeyMap());
    }

    private SimpleUser simpleUserFromUserEntity(UserEntity user) {
        return new SimpleUser(user.getId(), user.getUsername());
    }
}
