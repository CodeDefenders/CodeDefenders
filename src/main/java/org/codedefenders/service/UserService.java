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

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.User;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Provides an API for user retrieval and management.
 */
@ApplicationScoped
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final LoadingCache<Integer, SimpleUser> simpleUserForUserIdCache;

    private final UserRepository userRepo;

    @Inject
    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;

        simpleUserForUserIdCache = CacheBuilder.newBuilder()
                // Entries expire after a relative short time, since User(name) updates are not handled through this
                // class so we can't invalidate the entries on updates. This could lead to some stale data presented in
                // places where SimpleUser objects are used.
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(200)
                //.recordStats() // Nice to have for dev, unnecessary for production  without properly exposing it
                .build(
                        new CacheLoader<Integer, SimpleUser>() {
                            @Override
                            public SimpleUser load(@Nonnull Integer userId) throws Exception {
                                return getSimpleUserByIdInternal(userId)
                                        .orElseThrow(() -> new Exception("No user found for given userId"));
                            }
                        }
                );
    }

    /**
     * Query a {@link User} by its id.
     *
     * <p>In most cases where only the username is needed it is probably better to use {@link #getSimpleUserById(int)}.
     *
     * @param userId The id of the user to retrieve
     * @return An {@code Optional} containing the user for the provided {@code userId} or an empty {@code Optional} if
     * there exists no user for the given {@code userId}
     */
    @Nonnull
    public Optional<User> getUserById(int userId) {
        return userRepo.getUserById(userId).map(this::userFromUserEntity);
    }

    /**
     * Retrieve a {@link SimpleUser} by the given {@code userId}
     *
     * @param userId The Id of the user for which to lookup a simple representation.
     * @return An {@code Optional} containing a simple representation of the user for the provided {@code userId} or an
     * empty {@code Optional} if there exists no user for the given {@code userId}
     * @implNote Since {@link SimpleUser} objects only contain limited information, they are cached, so calling this
     * method multiple times is no big deal.
     */
    @Nonnull
    public Optional<SimpleUser> getSimpleUserById(int userId) {
        try {
            return Optional.of(simpleUserForUserIdCache.get(userId));
        } catch (ExecutionException e) {
            return Optional.empty();
        }
    }

    @Nonnull
    Optional<SimpleUser> getSimpleUserByIdInternal(int userId) {
        return userRepo.getUserById(userId).map(this::simpleUserFromUserEntity);
    }

    /**
     * Lookup the {@code userId} which corresponds to the given {@code playerId} and then retrieve a {@link SimpleUser}
     * for that {@code userId}.
     *
     * @param playerId The Id of the player for which to query a {@link SimpleUser} object.
     * @return An {@code Optional} containing the user for the provided {@code playerId} or an empty {@code Optional} if
     * there exists no user for the given {@code playerId}
     * @apiNote This method will probably be moved from this class to a class which is more involved in game handling/
     * managing like the {@code GameService} classes.
     * @see #getSimpleUserById(int)
     */
    // TODO: Relocate into sth like `PlayerService` or the `GameService`s
    @Nonnull
    public Optional<SimpleUser> getSimpleUserByPlayerId(int playerId) {
        return userRepo.getUserIdForPlayerId(playerId).flatMap(this::getSimpleUserById);
    }

    @Nonnull
    private User userFromUserEntity(@Nonnull UserEntity user) {
        return new User(user.getId(), user.getUsername(), user.isActive(), user.getEmail(), user.isValidated(),
                user.getAllowContact(), user.getKeyMap());
    }

    @Nonnull
    private SimpleUser simpleUserFromUserEntity(@Nonnull UserEntity user) {
        return new SimpleUser(user.getId(), user.getUsername());
    }
}
