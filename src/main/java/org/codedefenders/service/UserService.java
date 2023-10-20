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
import javax.annotation.Nullable;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.NotImplementedException;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.User;
import org.codedefenders.instrumentation.MetricsRegistry;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.transaction.Transactional;
import org.codedefenders.validation.input.CodeDefendersValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Provides an API for user retrieval and management.
 */
@Transactional
@ApplicationScoped
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final LoadingCache<Integer, SimpleUser> simpleUserForUserIdCache;

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Inject
    public UserService(UserRepository userRepo, MetricsRegistry metricsRegistry, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;

        simpleUserForUserIdCache = CacheBuilder.newBuilder()
                // Entries expire after a relative short time, since User(name) updates are not handled through this
                // class, so we can't invalidate the entries on updates. This could lead to some stale data presented in
                // places where SimpleUser objects are used.
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(200)
                .recordStats()
                .build(
                        new CacheLoader<>() {
                            @Nonnull
                            @Override
                            public SimpleUser load(@Nonnull Integer userId) throws Exception {
                                return getSimpleUserByIdInternal(userId)
                                        .orElseThrow(() -> new Exception("No user found for given userId"));
                            }
                        }
                );

        metricsRegistry.registerGuavaCache("simpleUserForUserId", simpleUserForUserIdCache);
    }


    //
    // User querying
    //

    /**
     * Query a {@link User} by its id.
     *
     * <p>In most cases where only the username is needed it is probably better to use {@link #getSimpleUserById(int)}.
     *
     * @param userId The id of the user to retrieve
     * @return An {@code Optional} containing the user for the provided {@code userId} or an empty {@code Optional} if
     *         there exists no user for the given {@code userId}
     */
    @Nonnull
    public Optional<User> getUserById(int userId) {
        return userRepo.getUserById(userId).map(this::userFromUserEntity);
    }

    /**
     * Retrieve a {@link SimpleUser} by the given {@code userId}.
     *
     * @param userId The Id of the user for which to look up a simple representation.
     * @return An {@code Optional} containing a simple representation of the user for the provided {@code userId} or an
     *         empty {@code Optional} if there exists no user for the given {@code userId}
     * @implNote Since {@link SimpleUser} objects only contain limited information, they are cached, so calling this
     *         method multiple times is no big deal.
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
     *         there exists no user for the given {@code playerId}
     * @apiNote This method will probably be moved from this class to a class which is more involved in game handling/
     *         managing like the {@code GameService} classes.
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


    //
    // Mutations (aka User Creation/Editing/Deletion)
    //

    /**
     * Perform a registration with the provided parameters.
     *
     * <p>Note: This method is mainly for self-registration. For the creation of (multiple) accounts with sth. like
     * {@link org.codedefenders.servlets.admin.AdminUserManagement#createUserAccounts(HttpServletRequest, String)}
     * may be required.
     */
    @Nonnull
    public Optional<String> registerUser(String username, String password, String email) {
        CodeDefendersValidator validator = new CodeDefendersValidator();

        String result = null;

        // TODO(Alex): Change result messages so enumeration attacks are not possible.
        //  See: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html#authentication-and-error-messages
        //  Examples can be found at https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html#incorrect-and-correct-response-examples
        //  Note: Usernames can currently be discovered through the leaderboard!
        // TODO(Alex): Harden against timing attacks!
        if (!(validator.validUsername(username))) {
            // This check should be performed in the user interface too.
            result = "Could not create user. Invalid username.";
        } else if (!validator.validPassword(password)) {
            // This check should be performed in the user interface too.
            result = "Could not create user. Invalid password.";
        } else if (!validator.validEmailAddress(email)) {
            // This check should be performed in the user interface too.
            result = "Could not create user. Invalid Email address.";
        } else if (userRepo.getUserByName(username).isPresent()) {
            result = "Could not create user. Username is already taken.";
        } else if (userRepo.getUserByEmail(email).isPresent()) {
            result = "Could not create user. Email has already been used. You can reset your password.";
        } else {
            UserEntity newUser = new UserEntity(username, passwordEncoder.encode(password), email);
            if (userRepo.insert(newUser).isEmpty()) {
                // TODO: How about some error handling?
                result = "Could not create user.";
            }
        }

        return Optional.ofNullable(result);
    }

    public void changeUsername(int userId, @Nonnull String newUsername) {
        simpleUserForUserIdCache.invalidate(userId);
        throw new NotImplementedException();
    }

    /**
     * See {@link org.codedefenders.servlets.UserSettingsManager#changeUserPassword(UserEntity, String)}
     */
    public void changePassword(@Nonnull String newPassword) {
        throw new NotImplementedException();
    }

    /**
     * See {@link org.codedefenders.servlets.UserSettingsManager#updateUserInformation(UserEntity, Optional, boolean)}
     */
    public void changeEmail(@Nonnull String newEmail) {
        throw new NotImplementedException();
    }


    /**
     * Admin only method!
     */
    @Nonnull
    public Optional<String> updateUser(int userId, @Nonnull String newUsername, @Nonnull String newEmail,
            @Nullable String newPassword) {
        CodeDefendersValidator validator = new CodeDefendersValidator();

        Optional<UserEntity> u = userRepo.getUserById(userId);
        if (u.isEmpty()) {
            return Optional.of("Error. User " + userId + " cannot be retrieved from database.");
        }
        UserEntity user = u.get();

        if (!newUsername.equals(user.getUsername()) && userRepo.getUserByName(newUsername).isPresent()) {
            return Optional.of("Username " + newUsername + " is already taken");
        }

        if (!newEmail.equals(user.getEmail()) && userRepo.getUserByEmail(newEmail).isPresent()) {
            return Optional.of("Email " + newEmail + " is already in use");
        }

        if (!validator.validEmailAddress(newEmail)) {
            return Optional.of("Email Address is not valid");
        }

        if (newPassword != null) {
            if (!validator.validPassword(newPassword)) {
                return Optional.of("Password is not valid");
            }
            user.setEncodedPassword(passwordEncoder.encode(newPassword));
        }
        user.setUsername(newUsername);
        user.setEmail(newEmail);

        if (!userRepo.update(user)) {
            return Optional.of("Error trying to update info for user " + userId + "!");
        }

        simpleUserForUserIdCache.invalidate(userId);
        return Optional.empty();
    }


    /**
     * For {@link org.codedefenders.servlets.registration.PasswordServlet#doPost(HttpServletRequest, HttpServletResponse)}
     * {@code case "resetPassword"}.
     */
    public void requestPasswordReset() {
        throw new NotImplementedException();
    }

    /**
     * For {@link org.codedefenders.servlets.registration.PasswordServlet#doPost(HttpServletRequest, HttpServletResponse)}
     * {@code case "changePassword"}.
     */
    public void resetPassword() {
        throw new NotImplementedException();
    }


    // TODO(Alex): How does this differ from {@link #deactivateAccount} ?!
    public void setAccountInactive() {
        throw new NotImplementedException();
    }

    public void setAccountActive() {
        throw new NotImplementedException();
    }


    /**
     * See {@link org.codedefenders.servlets.UserSettingsManager#removeUserInformation(UserEntity)}
     */
    public void deactivateAccount() {
        throw new NotImplementedException();
    }


    //
    // Session Recording
    //

    public boolean recordSession(int userId, String ipAddress) {
        return deleteRecordedSessions(userId) && userRepo.insertSession(userId, ipAddress);
    }

    public boolean deleteRecordedSessions(int userId) {
        return userRepo.deleteSessions(userId);
    }
}
