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

import org.codedefenders.DatabaseTest;
import org.codedefenders.model.UserEntity;
import org.codedefenders.rules.DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Category(DatabaseTest.class)
public class UserRepositoryIT {

    @Rule
    public DatabaseRule databaseRule = new DatabaseRule();

    private UserRepository userRepo;

    @Before
    public void setUp() throws Exception {
        userRepo = new UserRepository(databaseRule.getConnectionFactory());
    }

    private final String username1 = "user";
    private final String username2 = "another_user";
    private final String password1 = "6E76F176EB3D32121C36805631CBC080";
    private final String password2 = "F7596A8258ABE15B03DA5FA44E6DE370";
    private final String email1 = "user@example.com";
    private final String email2 = "another_user@example.com";

    private static void assertEntityAttributesEqual(UserEntity expected, UserEntity actual) {
        assertAll(
                () -> assertEquals(expected.getUsername(), actual.getUsername()),
                () -> assertEquals(expected.getEmail(), actual.getEmail()),
                () -> assertEquals(expected.isValidated(), actual.isValidated()),
                () -> assertEquals(expected.getAllowContact(), actual.getAllowContact()),
                () -> assertEquals(expected.getKeyMap(), actual.getKeyMap())
        );
    }

    private static void assertSanePassword(String rawPassword, UserEntity actual) {
        assertNotEquals("Password should not be stored in plain text", rawPassword, actual.getEncodedPassword());
        assertTrue(UserEntity.passwordMatches(rawPassword, actual.getEncodedPassword()));
    }

    @Test
    public void insertAndQueryUserById() {
        UserEntity user = new UserEntity(username1, UserEntity.encodePassword(password1), email1);

        Integer userId = userRepo.insert(user);
        assertNotNull(userId, "Couldn't store user to the database");

        UserEntity userFromDB = userRepo.getUserById(userId);
        assertAll(
                () -> assertEquals(userId.intValue(), userFromDB.getId()),
                () -> assertEntityAttributesEqual(user, userFromDB),
                () -> assertSanePassword(password1, userFromDB)
        );
    }

    @Test
    public void insertAndQueryUserByName() {
        UserEntity user = new UserEntity(username1, UserEntity.encodePassword(password1), email1);

        Integer userId = userRepo.insert(user);
        assertNotNull(userId, "Couldn't store user to the database");

        UserEntity userFromDB = userRepo.getUserByName(username1);
        assertAll(
                () -> assertEquals(userId.intValue(), userFromDB.getId()),
                () -> assertEntityAttributesEqual(user, userFromDB),
                () -> assertSanePassword(password1, userFromDB)
        );
    }

    @Test
    public void insertAndQueryUserByEmail() {
        UserEntity user = new UserEntity(username1, UserEntity.encodePassword(password1), email1);

        Integer userId = userRepo.insert(user);
        assertNotNull(userId, "Couldn't store user to the database");

        UserEntity userFromDB = userRepo.getUserByEmail(email1);
        assertAll(
                () -> assertEquals(userId.intValue(), userFromDB.getId()),
                () -> assertEntityAttributesEqual(user, userFromDB),
                () -> assertSanePassword(password1, userFromDB)
        );
    }

    @Test
    public void queryNonExistentUserId() {
        assertNull(userRepo.getUserById(4684487));
    }

    @Test
    public void insertUserTwice() {
        UserEntity user = new UserEntity(username1, UserEntity.encodePassword(password1), email1);

        Integer userId = userRepo.insert(user);
        assumeTrue(userId != null);

        assertNull(userRepo.insert(user));
    }

    @Test
    public void insertUserWithValidId() {
        UserEntity user = new UserEntity(username1, UserEntity.encodePassword(password1), email1);
        user.setId(1);

        assertThrows(IllegalArgumentException.class, () -> userRepo.insert(user));
    }

    @Test
    public void updateUser() {
        UserEntity user = new UserEntity(username1, UserEntity.encodePassword(password1), email1);

        Integer userId = userRepo.insert(user);
        assumeTrue(userId != null);
        user.setId(userId);
        user.setEncodedPassword(UserEntity.encodePassword(password2));
        user.setUsername(username2);
        user.setEmail(email2);

        assertTrue(userRepo.update(user));
        UserEntity userFromDB = userRepo.getUserById(user.getId());
        assertAll(
                () -> assertEquals(userId.intValue(), userFromDB.getId()),
                () -> assertEntityAttributesEqual(user, userFromDB),
                () -> assertSanePassword(password2, userFromDB)
        );
    }

    @Test
    public void updateUserViolatesDBConstraint() {
        UserEntity user1 = new UserEntity(username1, UserEntity.encodePassword(password1), email1);
        Integer userId1 = userRepo.insert(user1);
        assumeTrue(userId1 != null);
        user1.setId(userId1);
        UserEntity user2 = new UserEntity(username2, UserEntity.encodePassword(password2), email2);
        Integer userId2 = userRepo.insert(user2);
        assumeTrue(userId2 != null);
        user2.setId(userId2);

        user2.setEmail(email1);

        assertFalse(userRepo.update(user2));
    }

    @Test
    public void queryUserList() {
        UserEntity user1 = new UserEntity(username1, UserEntity.encodePassword(password1), email1);
        Integer userId1 = userRepo.insert(user1);
        assumeTrue(userId1 != null);
        UserEntity user2 = new UserEntity(username2, UserEntity.encodePassword(password2), email2);
        Integer userId2 = userRepo.insert(user2);
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
        assertTrue(userRepo.getUnassignedUsers().isEmpty());
    }
}
