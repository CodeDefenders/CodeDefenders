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
package org.codedefenders.auth;

import java.util.List;
import java.util.Optional;

import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.SettingsRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.RoleService;
import org.junit.jupiter.api.Test;

import static org.codedefenders.util.Constants.MUTLE_GUEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CodeDefendersRealmGuestTest {
    @Test
    void guestCannotAuthenticateEvenWithValidCredentialsAndEmailValidationDisabled() {
        UserRepository users = mock(UserRepository.class);
        SettingsRepository settings = mock(SettingsRepository.class);
        RoleService roles = mock(RoleService.class);
        UserEntity guest = mock(UserEntity.class);
        when(guest.getId()).thenReturn(MUTLE_GUEST_USER_ID);
        when(guest.isActive()).thenReturn(true);
        when(guest.isValidated()).thenReturn(true);
        when(guest.getEncodedPassword()).thenReturn("a valid encoded password after reset");
        when(users.getUserByName("Mutle Guest")).thenReturn(Optional.of(guest));
        CodeDefendersRealm realm = new CodeDefendersRealm(mock(CodeDefendersRealm.CodeDefendersCacheManager.class),
                mock(CodeDefendersRealm.CodeDefendersCredentialsMatcher.class), settings, users, roles);
        assertThrows(LockedAccountException.class,
                () -> realm.doGetAuthenticationInfo(new UsernamePasswordToken("Mutle Guest", "password")));

        UserEntity account = mock(UserEntity.class);
        when(account.getId()).thenReturn(104);
        when(account.isActive()).thenReturn(true);
        when(account.getEncodedPassword()).thenReturn("encoded password");
        when(users.getUserByName("account")).thenReturn(Optional.of(account));
        when(roles.getRolesForUser(104)).thenReturn(List.of());
        assertNotNull(realm.doGetAuthenticationInfo(new UsernamePasswordToken("account", "password")));
    }
}
