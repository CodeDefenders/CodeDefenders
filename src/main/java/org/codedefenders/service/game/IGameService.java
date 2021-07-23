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

import java.util.List;

import org.codedefenders.dto.MutantDTO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.TestDTO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;

public interface IGameService {

    MutantDTO getMutant(int userId, int mutantId);

    MutantDTO getMutant(int userId, Mutant mutant);

    List<MutantDTO> getMutants(int userId, int gameId);

    @Deprecated
    List<MutantDTO> getMutants(SimpleUser user, AbstractGame game);

    TestDTO getTest(int userId, int testId);

    TestDTO getTest(int userId, Test test);

    List<TestDTO> getTests(int userId, int gameId);

    @Deprecated
    List<TestDTO> getTests(SimpleUser user, AbstractGame game);
}
