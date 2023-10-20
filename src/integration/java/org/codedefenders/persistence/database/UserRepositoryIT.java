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

package org.codedefenders.persistence.database;

import java.util.List;
import java.util.Optional;

import org.codedefenders.auth.PasswordEncoderProvider;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.instrumentation.MetricsRegistry;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.util.DatabaseExtension;
import org.codedefenders.util.tags.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

@DatabaseTest
@ExtendWith(DatabaseExtension.class)
public class UserRepositoryIT {

    private UserRepository userRepo;

    @BeforeEach
    public void setUp(QueryRunner queryRunner) throws Exception {
        userRepo = new UserRepository(queryRunner, mock(MetricsRegistry.class));
    }

    private final String username1 = "user";
    private final String username2 = "another_user";
    private final String password1 = "6E76F176EB3D32121C36805631CBC080";
    private final String password2 = "F7596A8258ABE15B03DA5FA44E6DE370";
    private final String email1 = "user@example.com";
    private final String email2 = "another_user@example.com";

    private final PasswordEncoder passwordEncoder = PasswordEncoderProvider.getPasswordEncoder();

    private static void assertEntityAttributesEqual(UserEntity expected, UserEntity actual) {
        assertAll(
                () -> assertEquals(expected.getUsername(), actual.getUsername()),
                () -> assertEquals(expected.getEmail(), actual.getEmail()),
                () -> assertEquals(expected.isValidated(), actual.isValidated()),
                () -> assertEquals(expected.getAllowContact(), actual.getAllowContact()),
                () -> assertEquals(expected.getKeyMap(), actual.getKeyMap())
        );
    }

    private void assertSanePassword(String rawPassword, UserEntity actual) {
        assertNotEquals("Password should not be stored in plain text", rawPassword, actual.getEncodedPassword());
        assertTrue(passwordEncoder.matches(rawPassword, actual.getEncodedPassword()));
    }

    @Test
    public void insertAndQueryUserById() {
        UserEntity user = new UserEntity(username1, passwordEncoder.encode(password1), email1);

        Integer userId = userRepo.insert(user).get();
        assertNotNull(userId, "Couldn't store user to the database");

        UserEntity userFromDB = userRepo.getUserById(userId).get();
        assertAll(
                () -> assertEquals(userId.intValue(), userFromDB.getId()),
                () -> assertEntityAttributesEqual(user, userFromDB),
                () -> assertSanePassword(password1, userFromDB)
        );
    }

    @Test
    public void insertAndQueryUserByName() {
        UserEntity user = new UserEntity(username1, passwordEncoder.encode(password1), email1);

        Optional<Integer> userId = userRepo.insert(user);
        assertTrue(userId.isPresent(), "Couldn't store user to the database");

        Optional<UserEntity> userFromDB = userRepo.getUserByName(username1);
        assertAll(
                () -> assertEquals(userId.get(), userFromDB.get().getId()),
                () -> assertEntityAttributesEqual(user, userFromDB.get()),
                () -> assertSanePassword(password1, userFromDB.get())
        );
    }

    @Test
    public void insertAndQueryUserByEmail() {
        UserEntity user = new UserEntity(username1, passwordEncoder.encode(password1), email1);

        Integer userId = userRepo.insert(user).orElse(null);
        assertNotNull(userId, "Couldn't store user to the database");

        UserEntity userFromDB = userRepo.getUserByEmail(email1).orElse(null);
        assertAll(
                () -> assertEquals(userId.intValue(), userFromDB.getId()),
                () -> assertEntityAttributesEqual(user, userFromDB),
                () -> assertSanePassword(password1, userFromDB)
        );
    }

    @Test
    public void queryNonExistentUserId() {
        assertFalse(userRepo.getUserById(4684487).isPresent());
    }

    @Test
    public void insertUserTwice() {
        UserEntity user = new UserEntity(username1, passwordEncoder.encode(password1), email1);

        Integer userId = userRepo.insert(user).orElse(null);
        assumeTrue(userId != null);

        assertThrows(UncheckedSQLException.class, () -> userRepo.insert(user));
    }

    @Test
    public void insertUserWithValidId() {
        UserEntity user = new UserEntity(username1, passwordEncoder.encode(password1), email1);
        user.setId(1);

        assertThrows(IllegalArgumentException.class, () -> userRepo.insert(user));
    }

    @Test
    public void updateUser() {
        UserEntity user = new UserEntity(username1, passwordEncoder.encode(password1), email1);

        Integer userId = userRepo.insert(user).orElse(null);
        assumeTrue(userId != null);
        user.setId(userId);
        user.setEncodedPassword(passwordEncoder.encode(password2));
        user.setUsername(username2);
        user.setEmail(email2);

        assertTrue(userRepo.update(user));
        UserEntity userFromDB = userRepo.getUserById(user.getId()).get();
        assertAll(
                () -> assertEquals(userId.intValue(), userFromDB.getId()),
                () -> assertEntityAttributesEqual(user, userFromDB),
                () -> assertSanePassword(password2, userFromDB)
        );
    }

    @Test
    public void updateUserViolatesDBConstraint() {
        UserEntity user1 = new UserEntity(username1, passwordEncoder.encode(password1), email1);
        Integer userId1 = userRepo.insert(user1).orElse(null);
        assumeTrue(userId1 != null);
        user1.setId(userId1);
        UserEntity user2 = new UserEntity(username2, passwordEncoder.encode(password2), email2);
        Integer userId2 = userRepo.insert(user2).orElse(null);
        assumeTrue(userId2 != null);
        user2.setId(userId2);

        user2.setEmail(email1);

        assertThrows(UncheckedSQLException.class, () -> userRepo.update(user2));
    }

    @Test
    public void queryUserList() {
        UserEntity user1 = new UserEntity(username1, passwordEncoder.encode(password1), email1);
        Integer userId1 = userRepo.insert(user1).orElse(null);
        assumeTrue(userId1 != null);
        UserEntity user2 = new UserEntity(username2, passwordEncoder.encode(password2), email2);
        Integer userId2 = userRepo.insert(user2).orElse(null);
        assumeTrue(userId2 != null);

        List<UserEntity> users = userRepo.getUsers();

        UserEntity userFromDB1 = users.stream().filter(u -> u.getId() == userId1).findAny().orElse(null);
        UserEntity userFromDB2 = users.stream().filter(u -> u.getId() == userId2).findAny().orElse(null);


        assertAll(
                () -> assertNotNull(userFromDB1),
                () -> assertNotNull(userFromDB2)
        );
    }

    @Test
    public void queryEmptyAssignedUsers() {
        assertTrue(userRepo.getAssignedUsers().isEmpty());
    }
}
