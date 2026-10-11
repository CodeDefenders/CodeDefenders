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
package org.codedefenders.servlets.registration;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.util.URLUtils;
import org.junit.jupiter.api.Test;

import static org.codedefenders.util.Constants.MUTLE_GUEST_USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordServletGuestTest {
    @Test
    void guestCannotRequestOrRedeemAPasswordReset() throws Exception {
        UserRepository users = mock(UserRepository.class);
        UserEntity guest = mock(UserEntity.class);
        when(guest.getId()).thenReturn(MUTLE_GUEST_USER_ID);
        when(users.getUserByEmail("mutle-guest@codedefenders.invalid")).thenReturn(Optional.of(guest));
        when(users.getUserIdForPasswordResetSecret("existing-token")).thenReturn(Optional.of(MUTLE_GUEST_USER_ID));
        PasswordServlet servlet = new PasswordServlet();
        inject(servlet, "userRepo", users);
        inject(servlet, "messages", mock(MessagesBean.class));
        inject(servlet, "url", mock(URLUtils.class));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getParameter("formType")).thenReturn("resetPassword");
        when(request.getParameter("accountEmail")).thenReturn("mutle-guest@codedefenders.invalid");
        when(request.getParameter("accountUsername")).thenReturn("Mutle Guest");
        servlet.doPost(request, response);
        verify(users, never()).setPasswordResetSecret(anyInt(), any());

        when(request.getParameter("formType")).thenReturn("changePassword");
        when(request.getParameter("resetPwSecret")).thenReturn("existing-token");
        servlet.doPost(request, response);
        verify(users, never()).update(any());
        verify(users, never()).getUserById(MUTLE_GUEST_USER_ID);
    }

    private static void inject(Object target, String name, Object value) throws Exception {
        var field = PasswordServlet.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
