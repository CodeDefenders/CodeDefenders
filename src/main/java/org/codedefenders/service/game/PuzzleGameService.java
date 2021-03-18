/*
 * Copyright (C) 2020 Code Defenders contributors
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

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.TestDTO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.model.Player;
import org.codedefenders.model.UserEntity;

@ApplicationScoped
public class PuzzleGameService extends AbstractGameService {

    @Override
    protected MutantDTO convertMutant(Mutant mutant, UserEntity user, Player player, AbstractGame game) {
        if (player == null) {
            return new MutantDTO(mutant);
        } else {
            return new MutantDTO(mutant)
                    .setCovered(mutant.isCovered(game.getTests(true)))
                    .setViewable(player.getRole() != null && player.getRole() != Role.NONE);
        }
    }

    @Override
    protected TestDTO convertTest(Test test, UserEntity user, Player player, AbstractGame game) {
        boolean viewable = true;

        UserEntity creator = userRepository.getUserById(userRepository.getUserIdForPlayerId(test.getPlayerId()));
        SimpleUser simpleCreator = new SimpleUser(creator.getId(), creator.getUsername());

        return new TestDTO(test.getId(), simpleCreator, test.getScore(), viewable,
                test.getCoveredMutants(game.getMutants()).stream().map(Mutant::getId).collect(Collectors.toList()),
                test.getKilledMutants().stream().map(Mutant::getId).collect(Collectors.toList()),
                (new TestSmellsDAO()).getDetectedTestSmellsForTest(test),
                test.getGameId(),
                test.getPlayerId(),
                test.getLineCoverage().getLinesCovered(),
                test.getAsString()
        );
    }
}
