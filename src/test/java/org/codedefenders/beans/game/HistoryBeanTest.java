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
package org.codedefenders.beans.game;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.EventDAO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.service.UserService;
import org.codedefenders.util.CDIUtil;
import org.junit.jupiter.api.Test;
import org.xnap.commons.i18n.I18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class HistoryBeanTest {
    @Test
    void includesAiEventsAndHidesOtherReservedAccounts() {
        EventDAO eventDao = mock(EventDAO.class);
        UserService userService = mock(UserService.class);
        when(eventDao.getEventsForGame(106)).thenReturn(List.of(
                event(1, -1, EventType.GAME_STARTED),
                event(2, 3, EventType.PLAYER_MUTANT_CREATED),
                event(3, 5, EventType.ATTACKER_MUTANT_CREATED),
                event(4, 6, EventType.DEFENDER_TEST_CREATED),
                event(5, 7, EventType.PLAYER_TEST_CREATED),
                event(6, 100, EventType.GAME_STARTED)));
        when(userService.getSimpleUserById(anyInt())).thenAnswer(invocation -> {
            int userId = invocation.getArgument(0);
            return Optional.of(new SimpleUser(userId, "user-" + userId));
        });

        try (var ignored = mockStatic(CDIUtil.class)) {
            ignored.when(() -> CDIUtil.getBeanFromCDI(EventDAO.class)).thenReturn(eventDao);
            ignored.when(() -> CDIUtil.getBeanFromCDI(UserService.class)).thenReturn(userService);

            HistoryBean history = new HistoryBean();
            history.setGameId(106);
            history.setLogin(mock(CodeDefendersAuth.class));
            List<String> users = history.getEvents(mock(I18n.class)).stream()
                    .map(HistoryBean.HistoryBeanEventDTO::getUserName)
                    .toList();

            assertEquals(List.of("user-5", "user-6", "user-7", "user-100"), users);
        }
    }

    private static Event event(int id, int userId, EventType type) {
        return new Event(id, 106, userId, "", type, EventStatus.GAME, new Timestamp(0));
    }
}
