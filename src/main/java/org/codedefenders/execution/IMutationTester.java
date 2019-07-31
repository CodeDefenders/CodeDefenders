package org.codedefenders.execution;

import java.util.ArrayList;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;

public interface IMutationTester {

    void runTestOnAllMutants(AbstractGame game, Test test, ArrayList<String> messages);

    void runTestOnAllMultiplayerMutants(MultiplayerGame game, Test test, ArrayList<String> messages);

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
     * mutant, using a the given TestScheduler for ordering the execution of
     * tests.
     *
     * @param game
     * @param mutant
     * @param messages
     * @param scheduler
     */
    void runAllTestsOnMutant(AbstractGame game, Mutant mutant, ArrayList<String> messages, TestScheduler scheduler);

    /**
     * Runs an equivalence test using an attacker supplied test and a mutant thought to be equivalent.
     * Kills mutant either with ASSUMED_YES if test passes on the mutant or with PROVEN_NO otherwise
     *
     * @param test   attacker-created test
     * @param mutant a mutant
     */
    void runEquivalenceTest(Test test, Mutant mutant);

    boolean testVsMutant(Test test, Mutant mutant);

}
