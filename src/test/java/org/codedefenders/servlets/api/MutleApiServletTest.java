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
package org.codedefenders.servlets.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.codedefenders.arcade.mutle.service.MutleGuestLimits;
import org.codedefenders.arcade.mutle.service.MutleGuestSession;
import org.codedefenders.arcade.mutle.service.MutleService;
import org.codedefenders.arcade.mutle.service.MutleSettings;
import org.codedefenders.auth.CodeDefendersAuth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MutleApiServletTest {
    @Test
    void boundsChunkedBodiesWithoutRelyingOnContentLength() throws Exception {
        assertEquals("{}", MutleApiServlet.readBody(new StringReader("{}")));
        assertEquals(32_768, MutleApiServlet.readBody(new StringReader("a".repeat(32_768))).length());
        assertThrows(MutleGuestLimits.RequestTooLargeException.class,
                () -> MutleApiServlet.readBody(new StringReader("a".repeat(32_769))));
    }

    @Test
    void incompleteRequestDoesNotReserveWorkersOrAllocateSession() throws Exception {
        MutleGuestLimits limits = new MutleGuestLimits();
        MutleGuestSession guest = mock(MutleGuestSession.class);
        MutleSettings settings = mock(MutleSettings.class);
        when(settings.enabled()).thenReturn(true);
        MutleService service = mock(MutleService.class);
        when(service.isReady()).thenReturn(true);
        MutleApiServlet servlet = new MutleApiServlet();
        inject(servlet, "guestLimits", limits);
        inject(servlet, "guest", guest);
        inject(servlet, "settings", settings);
        inject(servlet, "mutleService", service);
        inject(servlet, "login", mock(CodeDefendersAuth.class));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");
        when(request.getPathInfo()).thenReturn("/start");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession()).thenReturn(mock(HttpSession.class));
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        CountDownLatch reading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{}")) {
            @Override
            public int read(char[] buffer, int offset, int length) throws IOException {
                reading.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IOException("Request body did not resume");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
                return super.read(buffer, offset, length);
            }
        });
        var executor = Executors.newSingleThreadExecutor();
        try {
            var pending = executor.submit(() -> {
                servlet.doPost(request, response);
                return null;
            });
            assertTrue(reading.await(5, TimeUnit.SECONDS));
            try (var first = limits.acquire(); var second = limits.acquire()) {
                assertThrows(MutleGuestLimits.BusyException.class, limits::acquire);
                verify(request, never()).getSession();
            }
            release.countDown();
            pending.get(5, TimeUnit.SECONDS);
            verify(guest).start("127.0.0.1");
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private static void inject(MutleApiServlet servlet, String name, Object value) throws Exception {
        var field = MutleApiServlet.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(servlet, value);
    }
}
