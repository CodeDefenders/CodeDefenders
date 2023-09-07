/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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

import java.io.File;
import java.io.IOException;

import org.codedefenders.execution.AntRunner;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;

public interface IGameManagingUtils {

    /**
     * Returns a {@link Mutant} instance if for a given game and mutated code an mutant exists already,
     * returns {@code null} otherwise.
     *
     * @param gameId      the identifier of the given game.
     * @param mutatedCode the mutated source code.
     * @return a {@link Mutant} for the game and mutated code, or if not found {@code null}.
     */
    Mutant existingMutant(int gameId, String mutatedCode);

    /**
     * Returns {@code true} a given player has mutants with unresolved equivalence
     * allegations in a game against him, {@code false} otherwise.
     *
     * @param gameId     the identifier of the given game.
     * @param attackerId the player identifier of the attacker.
     * @return {@code true} if a given player has pending equivalences, {@code false} otherwise.
     */
    boolean hasAttackerPendingMutantsInGame(int gameId, int attackerId);

    /**
     * Stores a given mutant and calls {@link AntRunner#compileMutant(File, String, int, GameClass, int)
     * AntRunner#compileMutant()}.
     *
     * <p><b>Here, the mutated code is assumed to be valid.</b>
     *
     * @param gameId       the identifier of the game the mutant is created in.
     * @param classId      the identifier of the class the mutant is mutated from.
     * @param mutatedCode  the source code of the mutant.
     * @param ownerUserId  the identifier of the user who created the mutant.
     * @param subDirectory the directory used for '/mutants/subDirectory/gameId/userId'
     * @return a {@link Mutant}, but never {@code null}.
     * @throws IOException when storing the mutant was not successful.
     */
    Mutant createMutant(int gameId, int classId, String mutatedCode, int ownerUserId, String subDirectory)
            throws IOException;

    Test createTest(int gameId, int classId, String testText, int ownerUserId, String subDirectory) throws IOException;

}
