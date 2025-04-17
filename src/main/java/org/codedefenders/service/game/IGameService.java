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

    /**
     * Closes a game.
     * Note that this method may persist the games other attributes in the database.
     *
     * @param game The game to close.
     * @return {@code true} if the game was successfully closed, {@code false} otherwise.
     */
    boolean closeGame(AbstractGame game);

    /**
     * Starts a game.
     * Note that this method may persist the games other attributes in the database.
     *
     * @param game The game to start.
     * @return {@code true} if the game was successfully started, {@code false} otherwise.
     */
    boolean startGame(AbstractGame game);
}
