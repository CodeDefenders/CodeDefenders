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

import org.codedefenders.auth.roles.AdminRole;
import org.codedefenders.auth.roles.TeacherRole;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.instrumentation.MetricsRegistry;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.util.DatabaseExtension;
import org.codedefenders.util.tags.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@DatabaseTest
@ExtendWith({MockitoExtension.class, DatabaseExtension.class})
public class RoleRepositoryIT {

    private UserRepository userRepo;
    private RoleRepository roleRepo;

    @BeforeEach
    void beforeEach(QueryRunner queryRunner) {
        MetricsRegistry metricsRegistry = mock(MetricsRegistry.class);

        roleRepo = new RoleRepository(queryRunner);
        userRepo = new UserRepository(queryRunner, metricsRegistry);
    }

    @Test
    public void testAddRoleName() {
        int userId = userRepo.insert(new UserEntity("userA", "", "userA@example.org"))
                .orElseThrow();

        assume().that(roleRepo.getRoleNamesForUser(userId))
                .isEmpty();

        roleRepo.addRoleNameForUser(userId, AdminRole.name);

        assertThat(roleRepo.getRoleNamesForUser(userId))
                .containsExactly(AdminRole.name);

        roleRepo.addRoleNameForUser(userId, TeacherRole.name);

        assertThat(roleRepo.getRoleNamesForUser(userId))
                .containsExactly(AdminRole.name, TeacherRole.name);
    }

    @Test
    public void testAddInvalidRoleName() {
        int userId = userRepo.insert(new UserEntity("userA", "", "userA@example.org"))
                .orElseThrow();

        assertThrows(UncheckedSQLException.class,
                () -> roleRepo.addRoleNameForUser(userId, "roleThatWillNeverExist"));
    }

    @Test
    public void testRemoveRoleName() {
        int userId = userRepo.insert(new UserEntity("userA", "", "userA@example.org"))
                .orElseThrow();

        roleRepo.addRoleNameForUser(userId, AdminRole.name);
        roleRepo.addRoleNameForUser(userId, TeacherRole.name);

        assume().that(roleRepo.getRoleNamesForUser(userId))
                .containsExactly(AdminRole.name, TeacherRole.name);

        roleRepo.removeRoleNameForUser(userId, AdminRole.name);

        assertThat(roleRepo.getRoleNamesForUser(userId))
                .containsExactly(TeacherRole.name);

        roleRepo.removeRoleNameForUser(userId, TeacherRole.name);

        assertThat(roleRepo.getRoleNamesForUser(userId))
                .isEmpty();
    }

    @Test
    public void getAllUserRoleNames() {
        int idA = userRepo.insert(new UserEntity("userA", "", "userA@example.org"))
                .orElseThrow();
        roleRepo.addRoleNameForUser(idA, AdminRole.name);
        roleRepo.addRoleNameForUser(idA, TeacherRole.name);

        int idB = userRepo.insert(new UserEntity("userB", "", "userB@example.org"))
                .orElseThrow();
        roleRepo.addRoleNameForUser(idB, AdminRole.name);

        int idC = userRepo.insert(new UserEntity("userC", "", "userC@example.org"))
                .orElseThrow();

        var roleNames = roleRepo.getAllUserRoleNames();

        assertThat(roleNames.get(idA))
                .containsExactly(AdminRole.name, TeacherRole.name);
        assertThat(roleNames.get(idB))
                .containsExactly(AdminRole.name);
        assertThat(roleNames.get(idC))
                .isEmpty();
    }
}
