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
package org.codedefenders.execution;

import java.io.File;

import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;

/**
 * @author gambi
 */
public interface BackendExecutorService {

    /**
     * Execute a {@link Test test} against a {@link GameClass class under test}.
     * Calling this method results in an added {@code jacoco.exec} file to the given test directory.
     *
     * @param cut the class under test that is tested.
     * @param testDir the directory the generated {@code jacoco.exec} file is placed as a {@link String}.
     * @param testClassName the qualified name of the test class to be executed.
     * @throws ExecutionException when the test failed to execute or during execution.
     */
    void testOriginal(GameClass cut, String testDir, String testClassName) throws ExecutionException;

    /**
     * Executes a test against the original code.
     *
     * @param dir  Test directory
     * @param test A {@link Test} object
     * @return A {@link TargetExecution} object
     */
    TargetExecution testOriginal(File dir, Test test); // TODO: add ExecutionException to signature?

    /**
     * Executes a test against a mutant.
     *
     * @param m A {@link Mutant} object
     * @param t A {@link Test} object
     * @return A {@link TargetExecution} object
     */
    TargetExecution testMutant(Mutant m, Test t); // TODO: add ExecutionException to signature?


    class ExecutionException extends Exception {
        public ExecutionException(String message) {
            super(message);
        }
        public ExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
