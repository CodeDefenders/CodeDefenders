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
package org.codedefenders.servlets.games;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.beans.game.PreviousSubmissionBean;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.execution.AntRunner.ExecutionLimitException;
import org.codedefenders.servlets.games.battleground.MultiplayerGameManager;
import org.codedefenders.servlets.games.melee.MeleeGameManager;
import org.codedefenders.servlets.games.puzzle.PuzzleGameManager;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.URLUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GameExecutionRejectionTest {
    static Stream<HttpServlet> gameServlets() {
        return Stream.of(new MultiplayerGameManager() {
            @Override public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
                dispatch(request, response);
            }
        }, new MeleeGameManager() {
            @Override public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
                dispatch(request, response);
            }
        }, new PuzzleGameManager() {
            @Override public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
                dispatch(request, response);
            }
        });
    }

    private static void dispatch(HttpServletRequest request, HttpServletResponse response) {
        if (request.getAttribute("rejection") instanceof RuntimeException rejection) {
            throw rejection;
        }
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @ParameterizedTest
    @MethodSource("gameServlets")
    void rejectedExecutionPreservesCodeAndReturnsToTheGame(HttpServlet servlet) throws Exception {
        PreviousSubmissionBean previous = new PreviousSubmissionBean();
        MessagesBean messages = new MessagesBean();
        inject(servlet, "previousSubmission", previous);
        inject(servlet, "messages", messages);
        HttpServletRequest request = request();
        when(request.getAttribute("rejection")).thenReturn(new ExecutionLimitException());
        when(request.getParameter("test")).thenReturn("assertEquals(2, value());");
        when(request.getParameter("mutant")).thenReturn("public int value() { return 3; }");
        when(request.getHeader("referer")).thenReturn("http://localhost/game?gameId=42");
        HttpServletResponse response = mock(HttpServletResponse.class);
        try (var cdi = mockStatic(CDIUtil.class)) {
            cdi.when(() -> CDIUtil.getBeanFromCDI(URLUtils.class)).thenReturn(mock(URLUtils.class));
            servlet.service(request, response);
        }
        verify(response).sendRedirect("http://localhost/game?gameId=42");
        verify(response, never()).setStatus(HttpServletResponse.SC_NO_CONTENT);
        assertEquals("assertEquals(2, value());", previous.getTestCode());
        assertEquals("public int value() { return 3; }", previous.getMutantCode());
        assertEquals(1, messages.getCount());
        assertTrue(messages.getMessages().get(0).isAlert());
        assertTrue(messages.getMessages().get(0).getText().contains("execution limits"));
    }

    @ParameterizedTest
    @MethodSource("gameServlets")
    void successfulDispatchDoesNotBecomeARejection(HttpServlet servlet) throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        servlet.service(request(), response);
        verify(response).setStatus(HttpServletResponse.SC_NO_CONTENT);
        verify(response, never()).sendRedirect(anyString());
    }

    @ParameterizedTest
    @MethodSource("gameServlets")
    void unrelatedFailuresAreNotSwallowed(HttpServlet servlet) {
        HttpServletRequest request = request();
        RuntimeException failure = new IllegalStateException("unrelated failure");
        when(request.getAttribute("rejection")).thenReturn(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> servlet.service(request, mock(HttpServletResponse.class))));
    }

    private HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getProtocol()).thenReturn("HTTP/1.1");
        return request;
    }

    private void inject(HttpServlet servlet, String name, Object value) throws Exception {
        Field field = servlet.getClass().getSuperclass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(servlet, value);
    }
}
