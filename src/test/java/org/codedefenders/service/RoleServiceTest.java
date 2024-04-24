package org.codedefenders.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.codedefenders.auth.CodeDefendersRealm;
import org.codedefenders.auth.roles.AdminRole;
import org.codedefenders.auth.roles.AuthRole;
import org.codedefenders.auth.roles.TeacherRole;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.RoleRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RoleServiceTest {

    private RoleService roleService;
    private UserRepository userRepo;
    private RoleRepository roleRepo;

    @BeforeEach
    void beforeEach() {
        roleRepo = mock(RoleRepository.class);
        userRepo = mock(UserRepository.class);

        CodeDefendersRealm realm = mock(CodeDefendersRealm.class);
        lenient().when(realm.resolveRole(AdminRole.name)).thenReturn(new AdminRole());
        lenient().when(realm.resolveRole(TeacherRole.name)).thenReturn(new TeacherRole());

        roleService = new RoleService(realm, roleRepo, userRepo);
    }

    @Test
    public void testAdminSetupAddsRole() {
        when(userRepo.getUserByName("userA")).thenReturn(
                Optional.of(new UserEntity(100, "userA", "", "")));

        roleService.doInitialAdminSetup(List.of("userA"));

        verify(roleRepo).addRoleNameForUser(100, AdminRole.name);
        verifyNoMoreInteractions(roleRepo);
    }

    @Test
    public void testAdminSetupIgnoresMissingUser() {
        when(userRepo.getUserByName("userA")).thenReturn(
                Optional.empty());

        assertDoesNotThrow(() -> roleService.doInitialAdminSetup(List.of("userA")));

        verifyNoInteractions(roleRepo);
    }

    @Test
    public void testAdminSetupAddsMultiple() {
        when(userRepo.getUserByName("userA")).thenReturn(
                Optional.of(new UserEntity(100, "userA", "", "")));
        when(userRepo.getUserByName("userB")).thenReturn(
                Optional.of(new UserEntity(101, "userB", "", "")));
        when(userRepo.getUserByName("userC")).thenReturn(
                Optional.empty());

        assertDoesNotThrow(() -> roleService.doInitialAdminSetup(List.of("userA", "userB", "userC")));

        verify(roleRepo).addRoleNameForUser(100, AdminRole.name);
        verify(roleRepo).addRoleNameForUser(101, AdminRole.name);
        verifyNoMoreInteractions(roleRepo);
    }

    @Test
    public void testAddRole() {
        roleService.addRoleForUser(100, new TeacherRole());

        verify(roleRepo).addRoleNameForUser(100, TeacherRole.name);
        verifyNoMoreInteractions(roleRepo);
    }

    @Test
    public void testRemoveRole() {
        roleService.removeRoleForUser(100, new TeacherRole());

        verify(roleRepo).removeRoleNameForUser(100, TeacherRole.name);
        verifyNoMoreInteractions(roleRepo);
    }

    @Test
    public void testSetRoles() {
        when(roleRepo.getRoleNamesForUser(100)).thenReturn(
                List.of(TeacherRole.name));

        roleService.setRolesForUser(100, Set.of(new AdminRole()));

        verify(roleRepo).removeRoleNameForUser(100, TeacherRole.name);
        verify(roleRepo).addRoleNameForUser(100, AdminRole.name);
        verifyNoMoreInteractions(roleRepo);
    }

    @Test
    public void testGetRoles() {
        when(roleRepo.getRoleNamesForUser(100)).thenReturn(
                List.of());
        when(roleRepo.getRoleNamesForUser(101)).thenReturn(
                List.of(TeacherRole.name));
        when(roleRepo.getRoleNamesForUser(102)).thenReturn(
                List.of(TeacherRole.name, AdminRole.name));

        assertThat(roleService.getRolesForUser(100))
                .isEmpty();
        assertThat(roleService.getRolesForUser(101))
                .containsExactly(new TeacherRole());
        assertThat(roleService.getRolesForUser(102))
                .containsExactly(new TeacherRole(), new AdminRole());

        verify(roleRepo).getRoleNamesForUser(100);
        verify(roleRepo).getRoleNamesForUser(101);
        verify(roleRepo).getRoleNamesForUser(102);
    }

    @Test
    public void testGetAllRoles() {
        Multimap<Integer, String> roleNames = ArrayListMultimap.create();
        roleNames.put(101, TeacherRole.name);
        roleNames.put(102, TeacherRole.name);
        roleNames.put(102, AdminRole.name);
        when(roleRepo.getAllUserRoleNames()).thenReturn(roleNames);

        Multimap<Integer, AuthRole> roles = roleService.getAllUserRoles();
        assertThat(roles.get(100))
                .isEmpty();
        assertThat(roles.get(101))
                .containsExactly(new TeacherRole());
        assertThat(roles.get(102))
                .containsExactly(new TeacherRole(), new AdminRole());
    }
}
