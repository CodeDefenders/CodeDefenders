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
package org.codedefenders.execution;

import java.util.ArrayList;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;

public interface IMutationTester {

    void runTestOnAllMutants(AbstractGame game, Test test, ArrayList<String> messages);

    void runTestOnAllMultiplayerMutants(MultiplayerGame game, Test test, ArrayList<String> messages);

    // TODO Note that we need duplicate methods because the basic runAll methods use
    // AbstractGame !
    /**
     * Execute the test against all the other players' mutants
     * 
     * @param game
     * @param newMutant
     * @param messages
     */
    void runTestOnAllMeleeMutants(MeleeGame game, Test test, ArrayList<String> messages);

    /**
     * Execute all the tests registered from all the other players against the
     * provided mutant, using a random scheduling of test execution.
     * 
     * @param game
     * @param newMutant
     * @param messages
     */
    void runAllTestsOnMeleeMutant(MeleeGame game, Mutant newMutant, ArrayList<String> messages);

    /**
     * Execute all the tests registered for the defenders against the provided
     * mutant, using a random scheduling of test execution.
     *
     * @param game
     * @param mutant
     * @param messages
     */
    void runAllTestsOnMutant(AbstractGame game, Mutant mutant, ArrayList<String> messages);

    /**
     * Execute all the tests registered for the defenders against the provided
     * mutant, using a the given TestScheduler for ordering the execution of tests.
     *
     * @param game
     * @param mutant
     * @param messages
     * @param scheduler
     */
    void runAllTestsOnMutant(AbstractGame game, Mutant mutant, ArrayList<String> messages, TestScheduler scheduler);

    /**
     * Runs an equivalence test using an attacker supplied test and a mutant thought
     * to be equivalent. Kills mutant either with ASSUMED_YES if test passes on the
     * mutant or with PROVEN_NO otherwise
     *
     * @param test   attacker-created test
     * @param mutant a mutant
     */
    void runEquivalenceTest(Test test, Mutant mutant);

    boolean testVsMutant(Test test, Mutant mutant);

}
