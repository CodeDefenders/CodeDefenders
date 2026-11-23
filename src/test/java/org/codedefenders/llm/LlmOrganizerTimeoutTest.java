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
package org.codedefenders.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import jakarta.enterprise.context.control.RequestContextController;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmDefaultStrategies;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.model.llm.LlmType;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.servlets.admin.AdminSystemSettings.SettingsDTO;
import org.codedefenders.util.CDIUtil;
import org.junit.jupiter.api.Test;

import dev.langchain4j.exception.TimeoutException;

import static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.LLM_INTERVAL_SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class LlmOrganizerTimeoutTest {
    @Test
    void aPlayerCanBeStartedAgainAfterARequestTimesOut() throws Exception {
        var scheduler = mock(ScheduledExecutorService.class);
        var workers = mock(ExecutorService.class);
        var organizer = new LlmOrganizer(scheduler, workers);
        var repository = mock(GameRepository.class);
        FieldUtils.writeField(organizer, "gameRepository", repository, true);
        List<Runnable> actions = new ArrayList<>();
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(scheduler).execute(any(Runnable.class));
        doAnswer(invocation -> {
            actions.add(invocation.getArgument(0));
            return null;
        }).when(workers).execute(any(Runnable.class));
        var game = mock(AbstractGame.class);
        when(game.getId()).thenReturn(123);
        when(repository.getGame(123)).thenReturn(game);
        when(repository.isGameActive(123)).thenReturn(true);
        var strategy = LlmStrategy.of(LlmDefaultStrategies.TEST_DEFAULT);
        var model = new LlModel("test-model", LlmType.OPENAI);
        var implementation = mock(TestStrategyDefault.class);
        doThrow(new TimeoutException("request timed out")).when(implementation).run(any());

        try (var cdi = mockStatic(CDIUtil.class); var admin = mockStatic(AdminDAO.class)) {
            cdi.when(() -> CDIUtil.getBeanFromCDI(GameRepository.class)).thenReturn(repository);
            cdi.when(() -> CDIUtil.getBeanFromCDI(RequestContextController.class))
                    .thenReturn(mock(RequestContextController.class));
            cdi.when(() -> CDIUtil.getBeanFromCDI(TestStrategyDefault.class)).thenReturn(implementation);
            admin.when(() -> AdminDAO.getSystemSetting(LLM_INTERVAL_SECONDS))
                    .thenReturn(new SettingsDTO(LLM_INTERVAL_SECONDS, 10));

            organizer.setPlayerModel(game, Role.DEFENDER, model, null, strategy, null);
            assertEquals(1, actions.size(), "The first action is queued immediately.");
            actions.get(0).run();
            assertTrue(organizer.getErrorMessage(123, Role.DEFENDER).isPresent());

            organizer.setPlayerModel(game, Role.DEFENDER, model, null, strategy, null);
            assertEquals(2, actions.size(), "A timed-out player must not remain marked as running.");
        }
    }
}
