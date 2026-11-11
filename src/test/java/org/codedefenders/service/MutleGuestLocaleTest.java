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
package org.codedefenders.service;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MutleGuestLocaleTest {
    @Test
    void errorLocalizationDoesNotCreateASessionForRejectedRequests() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getLocale()).thenReturn(Locale.GERMAN);
        assertEquals(Locale.GERMAN, I18nService.getSessionLocale(request));
        verify(request, never()).getSession();

        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("locale")).thenReturn(Locale.ENGLISH);
        assertEquals(Locale.ENGLISH, I18nService.getSessionLocale(request));
    }
}
