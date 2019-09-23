package org.codedefenders.servlets.games;

import java.io.File;
import java.io.IOException;

import org.codedefenders.execution.AntRunner;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorException;

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
     * <p>
     * <b>Here, the mutated code is assumed to be valid.</b>
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

    /**
     * Calls {@link #createTest(int, int, String, int, String, int)},but with the default number
     * of assertions specified in {@link CodeValidator#DEFAULT_NB_ASSERTIONS}.
     */
    Test createTest(int gameId, int classId, String testText, int ownerUserId, String subDirectory)
            throws IOException, CodeValidatorException;

//    /**
//     * Stores and validates a given test.
//     * Calls {@link AntRunner#compileTest(File, String, int, GameClass, int)} AntRunner#compileTest()} after.
//     *
//     * @param gameId       the identifier of the game the test is created in.
//     * @param classId      the identifier of the class the test is created for.
//     * @param testText the source code of the test.
//     * @param ownerUserId  the identifier of the user who created the mutant.
//     * @param subDirectory the directory used for '/tests/subDirectory/gameId/userId/original'
//     * @return a {@link Test} is valid, {@code null} otherwise.
//     * @throws IOException when storing the test was not successful.
//     * @throws CodeValidatorException when code validation resulted in an error.
//     */
//    Test createTest(int gameId, int classId, String testText, int ownerUserId, String subDirectory,
//            int maxNumberOfAssertions) throws IOException, CodeValidatorException;

}
