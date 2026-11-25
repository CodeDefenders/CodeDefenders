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
package org.codedefenders.service.game;

import java.util.List;
import java.util.Set;

import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.model.Player;
import org.codedefenders.persistence.database.TestRepository;
import org.codedefenders.persistence.database.TestSmellRepository;
import org.codedefenders.service.UserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiplayerTestVisibilityTest {
    @ParameterizedTest
    @CsvSource({
            "ATTACKER, HARD, false, false, false, false",
            "ATTACKER, HARD, false, true, false, true",
            "ATTACKER, MEDIUM, false, true, false, true",
            "ATTACKER, HARD, false, false, true, true",
            "DEFENDER, HARD, false, false, false, true",
            "OBSERVER, HARD, false, false, false, true",
            "ATTACKER, HARD, true, false, false, true",
            "ATTACKER, EASY, false, false, false, true"
    })
    void multiplayerTestVisibilityUsesRoleStateAndKillingTests(Role role, GameLevel level, boolean finished,
                                                                boolean hasKilledMutants, boolean ownsTest,
                                                                boolean canView) {
        var service = mock(MultiplayerGameService.class, CALLS_REAL_METHODS);
        service.testRepo = mock(TestRepository.class);
        service.userService = mock(UserService.class);
        ((AbstractGameService) service).testSmellRepo = mock(TestSmellRepository.class);
        var game = mock(AbstractGame.class);
        when(game.getLevel()).thenReturn(level);
        when(game.isFinished()).thenReturn(finished);
        var test = mock(org.codedefenders.game.Test.class);
        when(test.getId()).thenReturn(323);
        when(test.getPlayerId()).thenReturn(126);
        when(test.getGameId()).thenReturn(103);
        when(test.getLineCoverage()).thenReturn(new LineCoverage(List.of(), List.of()));
        var player = mock(Player.class);
        when(player.getId()).thenReturn(ownsTest ? 126 : 125);
        when(player.getRole()).thenReturn(role);
        var mutant = mock(Mutant.class);
        when(mutant.getId()).thenReturn(193);
        when(service.testRepo.getKilledMutantsForTestId(323))
                .thenReturn(hasKilledMutants ? Set.of(mutant) : Set.of());

        var dto = service.convertTest(test, new SimpleUser(100, "attacker"), player, game);

        assertEquals(canView, dto.isCanView());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void meleePlayersCanViewTestsThatKilledAMutant(boolean hasKilledMutants) {
        var service = mock(MeleeGameService.class, CALLS_REAL_METHODS);
        service.testRepo = mock(TestRepository.class);
        service.userService = mock(UserService.class);
        ((AbstractGameService) service).testSmellRepo = mock(TestSmellRepository.class);
        var game = mock(MeleeGame.class);
        when(game.getLevel()).thenReturn(GameLevel.HARD);
        var test = mock(org.codedefenders.game.Test.class);
        when(test.getId()).thenReturn(323);
        when(test.getPlayerId()).thenReturn(126);
        when(test.getGameId()).thenReturn(103);
        when(test.getLineCoverage()).thenReturn(new LineCoverage(List.of(), List.of()));
        var player = mock(Player.class);
        when(player.getId()).thenReturn(125);
        when(player.getRole()).thenReturn(Role.PLAYER);
        var mutant = mock(Mutant.class);
        when(mutant.getId()).thenReturn(193);
        when(service.testRepo.getKilledMutantsForTestId(323))
                .thenReturn(hasKilledMutants ? Set.of(mutant) : Set.of());

        var dto = service.convertTest(test, new SimpleUser(100, "player"), player, game);

        assertEquals(hasKilledMutants, dto.isCanView());
    }
}
