/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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

import org.apache.commons.collections.CollectionUtils;
import org.codedefenders.game.Mutant;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.scoring.Scorer;
import org.codedefenders.game.Test;
import org.codedefenders.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.emory.mathcs.backport.java.util.Collections;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import static org.codedefenders.util.Constants.MUTANT_ALIVE_1_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_ALIVE_N_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_KILLED_BY_TEST_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_SUBMITTED_MESSAGE;
import static org.codedefenders.util.Constants.TEST_KILLED_LAST_MESSAGE;
import static org.codedefenders.util.Constants.TEST_KILLED_N_MESSAGE;
import static org.codedefenders.util.Constants.TEST_KILLED_ONE_MESSAGE;
import static org.codedefenders.util.Constants.TEST_KILLED_ZERO_MESSAGE;
import static org.codedefenders.util.Constants.TEST_SUBMITTED_MESSAGE;
import static org.codedefenders.game.Mutant.Equivalence.ASSUMED_NO;
import static org.codedefenders.game.Mutant.Equivalence.PROVEN_NO;

// Class that handles compilation and testing by creating a Process with the relevant ant target
public class MutationTester {
	private static final Logger logger = LoggerFactory.getLogger(MutationTester.class);

	private static boolean parallelize = false;

	private static boolean useMutantCoverage = true;
	// Use a shared executor pool, prevents thread explosion.
	private static ExecutorService sharedExecutorService = Executors.newFixedThreadPool(30);

	// DO NOT REALLY LIKE THOSE...
	static {
		// First check the Web abb context
		InitialContext initialContext;
		try {
			initialContext = new InitialContext();
			NamingEnumeration<NameClassPair> list = initialContext.list("java:/comp/env");
			Context environmentContext = (Context) initialContext.lookup("java:/comp/env");

			// Looking up a name which is not there causes an exception
			// Some are unsafe !
			while (list.hasMore()) {
				String name = list.next().getName();
				switch (name) {
				case "mutant.coverage":
					useMutantCoverage = "enabled".equalsIgnoreCase((String) environmentContext.lookup(name));
					break;
				case "parallelize":
					parallelize = "enabled".equalsIgnoreCase((String) environmentContext.lookup(name));
					break;
				}
			}

		} catch (NamingException e) {
			e.printStackTrace();
		}
	}

	// RUN MUTATION TESTS: Runs all the mutation tests for a particular game,
	// using all the alive mutants and all tests

	// Inputs: The ID of the game to run mutation tests for
	// Outputs: None

	public static void runTestOnAllMutants(DuelGame game, Test test, ArrayList<String> messages) {
		int killed = 0;
		List<Mutant> mutants = game.getAliveMutants();
		for (Mutant mutant : mutants) {
			killed += testVsMutant(test, mutant) ? 1 : 0;
		}
		if (killed == 0)
			if (mutants.size() == 0)
				messages.add(TEST_SUBMITTED_MESSAGE);
			else
				messages.add(TEST_KILLED_ZERO_MESSAGE);
		else {
			if (killed == 1) {
				if (mutants.size() == 1)
					messages.add(TEST_KILLED_LAST_MESSAGE);
				else
					messages.add(TEST_KILLED_ONE_MESSAGE);
			} else {
				messages.add(String.format(TEST_KILLED_N_MESSAGE, killed));
			}

		}
	}

	public static void runTestOnAllMultiplayerMutants(MultiplayerGame game, Test test, ArrayList<String> messages) {
		int killed = 0;
		List<Mutant> mutants = game.getAliveMutants();
		mutants.addAll(game.getMutantsMarkedEquivalentPending());
		List<Mutant> killedMutants = new ArrayList<Mutant>();

		// Acquire and release the connection
		User u = DatabaseAccess.getUserFromPlayer(test.getPlayerId());

		if (parallelize) {
			// Fork and Join parallelization
			Map<Mutant, FutureTask<Boolean>> tasks = new HashMap<Mutant, FutureTask<Boolean>>();
			for (final Mutant mutant : mutants) {
				if (useMutantCoverage && !test.isMutantCovered(mutant)) {
					// System.out.println("Skipping non-covered mutant "
					// + mutant.getId() + ", test " + test.getId());
					continue;
				}

				FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {

					@Override
					public Boolean call() throws Exception {
						// This automatically update the 'mutants' and 'tests'
						// tables, as well as the test and mutant objects.
						return testVsMutant(test, mutant);
					}
				});

				// This is for checking later
				tasks.put(mutant, task);

				sharedExecutorService.execute(task);
			}

			// TODO Mayse use some timeout ?!
			for (final Mutant mutant : mutants) {
				if (useMutantCoverage && !test.isMutantCovered(mutant))
					continue;

				// checks if task done
				// System.out.println(
				// "Is mutant done? " + tasks.get(mutant).isDone());
				// checks if task canceled
				// System.out.println("Is mutant cancelled? "
				// + tasks.get(mutant).isCancelled());
				// fetches result and waits if not ready

				// THIS IS BLOCKING !!!
				try {
					if (tasks.get(mutant).get()) {
						killed++;
						killedMutants.add(mutant);
					}
				} catch (InterruptedException | ExecutionException | CancellationException e) {
                    logger.error("While waiting results for mutant " + mutant, e);
                }
			}

			tasks.clear();

		} else {
			// Normal execution
			for (Mutant mutant : mutants) {
				if (useMutantCoverage && !test.isMutantCovered(mutant)) {
					// System.out.println("Skipping non-covered mutant "
					// + mutant.getId() + ", test " + test.getId());
					continue;
				}

				if (testVsMutant(test, mutant)) {
					killed++;
					killedMutants.add(mutant);
				}
			}
		}

		for (Mutant mutant : mutants){
			if (mutant.isAlive()){
				ArrayList<Test> missedTests = new ArrayList<Test>();

				for (int lm : mutant.getLines()){
					boolean found = false;
					for (int lc : test.getLineCoverage().getLinesCovered()){
						if (lc == lm){
							found = true;
							missedTests.add(test);
						}
					}
					if (found){
						break;
					}
				}
				// mutant.setScore(Scorer.score(game, mutant, missedTests));
				// mutant.update();
				mutant.incrementScore(Scorer.score(game, mutant, missedTests));
			}
		}

		// test.setScore(Scorer.score(game, test, killedMutants));
		// test.update();
		test.incrementScore(Scorer.score(game, test, killedMutants));

		if (killed == 0)
			if (mutants.size() == 0)
				messages.add(TEST_SUBMITTED_MESSAGE);
			else
				messages.add(TEST_KILLED_ZERO_MESSAGE);
		else {
			Event notif = new Event(-1, game.getId(), u.getId(),
					u.getUsername() + "&#39;s test kills " + killed + " " + "mutants.",
					EventType.DEFENDER_KILLED_MUTANT, EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
			notif.insert();
			if (killed == 1) {
				if (mutants.size() == 1)
					messages.add(TEST_KILLED_LAST_MESSAGE);
				else
					messages.add(TEST_KILLED_ONE_MESSAGE);
			} else {
				messages.add(String.format(TEST_KILLED_N_MESSAGE, killed));
			}

		}
	}

	/**
	 * Execute all the tests registered for the defenders against the provided
	 * mutant, using a random scheduling of test execution.
	 *
	 * @param game
	 * @param mutant
	 * @param messages
	 */
	public static void runAllTestsOnMutant(AbstractGame game, Mutant mutant, ArrayList<String> messages) {
		runAllTestsOnMutant(game, mutant, messages, new RandomTestScheduler());
	}

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
	public static void runAllTestsOnMutant(AbstractGame game, Mutant mutant, ArrayList<String> messages,
			TestScheduler scheduler) {
		// Schedule the executable tests submitted by the defenders only (true)
		List<Test> tests = scheduler.scheduleTests( game.getTests(true) ) ;

		User u = DatabaseAccess.getUserFromPlayer(mutant.getPlayerId());

		if (parallelize) {
			final Map<Test, FutureTask<Boolean>> tasks = new HashMap<Test, FutureTask<Boolean>>();
			for (Test test : tests) {
				if (useMutantCoverage && !test.isMutantCovered(mutant)) {
					logger.info("Skipping non-covered mutant " + mutant.getId() + ", test " + test.getId());
					continue;
				}

				FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {

					@Override
					public Boolean call() throws Exception {
						logger.info("Executing mutant " + mutant.getId() + ", test " + test.getId());
						// TODO Is this testVsMutant thread safe?
						return testVsMutant(test, mutant);
                    }
                });

                // Book keeping
                tasks.put(test, task);
            }

            // Submit all the tests in the given order
            for (Test test : tests ) {
                if (tasks.containsKey(test)) {
                	logger.debug("MutationTester.runAllTestsOnMutant() : Scheduling Task " + test);
                    sharedExecutorService.execute(tasks.get(test));
                }
            }

            // Wait for the result. Check by the order defined by the scheduler.
            for (Test test : tests ) { 
                Future<Boolean> task = tasks.get(test);
                logger.debug("MutationTester.runAllTestsOnMutant() Checking task " + task + ". Done: "
                        + task.isDone() + ". Cancelled: " + task.isCancelled());
                try {

                    boolean hasTestkilledTheMutant = false;

                    try {
                        hasTestkilledTheMutant = task.get();
                    } catch (CancellationException ce) {
                        //
                        logger.warn("Swallowing ", ce);
                    }

                    if (hasTestkilledTheMutant) {
                        // This test killede the mutant...
                        messages.add(String.format(MUTANT_KILLED_BY_TEST_MESSAGE, test.getId()));
                        
                        if (game instanceof MultiplayerGame) {
                            ArrayList<Mutant> mlist = new ArrayList<Mutant>();
                            mlist.add(mutant);

							logger.info(">> Test {} kills mutant {} get {} points. Mutant is still alive ? {}",
									test.getId(), mutant.getId(), Scorer.score((MultiplayerGame) game, test, mlist),
									mutant.isAlive());
							test.incrementScore(Scorer.score((MultiplayerGame) game, test, mlist));
						}

                        Event notif = new Event(-1, game.getId(),
                                DatabaseAccess.getUserFromPlayer(test.getPlayerId()).getId(),
                                u.getUsername() + "&#39;s mutant is killed", EventType.DEFENDER_KILLED_MUTANT,
                                EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
                        notif.insert();

                        // Early return. No need to check for the other executions.
                        return;

                    }
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println(
                            "MutationTester.runAllTestsOnMutant() ERROR While waiting results for task " + task);
                    e.printStackTrace();
                }
            }

        } else {

            for (Test test : tests) {
                if (useMutantCoverage && !test.isMutantCovered(mutant)) {
                    logger.info("Skipping non-covered mutant " + mutant.getId() + ", test " + test.getId());
                    continue;
                }

                if (testVsMutant(test, mutant)) {
                    logger.info("Test {} kills mutant {}", test.getId(), mutant.getId());
                    messages.add(String.format(MUTANT_KILLED_BY_TEST_MESSAGE, test.getId()));
                    if (game instanceof MultiplayerGame) {
                        ArrayList<Mutant> mlist = new ArrayList<Mutant>();
                        mlist.add(mutant);
						// test.setScore(Scorer.score((MultiplayerGame) game, test, mlist));
						// test.update();
						test.incrementScore(Scorer.score((MultiplayerGame) game, test, mlist));
					}

                    Event notif = new Event(-1, game.getId(),
                            DatabaseAccess.getUserFromPlayer(test.getPlayerId()).getId(),
                            u.getUsername() + "&#39;s mutant is killed", EventType.DEFENDER_KILLED_MUTANT,
                            EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
                    notif.insert();

                    return; // return as soon as the first test kills the mutant we return
                }
            }
        }

		// TODO In the original implementation (see commit 4fbdc78304374ee31a06d56f8ce67ca80309e24c for example)
		// the first block and the second one are swapped. Why ?
        ArrayList<Test> missedTests = new ArrayList<Test>();
        if (game instanceof MultiplayerGame) {
            for (Test t : tests) {
                if (CollectionUtils.containsAny(t.getLineCoverage().getLinesCovered(), mutant.getLines()))
                    missedTests.add(t);
            }
//            mutant.setScore(1 + Scorer.score((MultiplayerGame) game, mutant, missedTests));
//            mutant.update();
            mutant.incrementScore(1 + Scorer.score((MultiplayerGame) game, mutant, missedTests));
        }

        int nbRelevantTests = missedTests.size();
        // Mutant survived
        if (nbRelevantTests == 0)
            messages.add(MUTANT_SUBMITTED_MESSAGE);
        else if (nbRelevantTests <= 1)
            messages.add(MUTANT_ALIVE_1_MESSAGE);
        else
            messages.add(String.format(MUTANT_ALIVE_N_MESSAGE, nbRelevantTests));
        Event notif = new Event(-1, game.getId(), u.getId(), u.getUsername() + "&#39;s mutant survives the test suite.",
                EventType.ATTACKER_MUTANT_SURVIVED, EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
        notif.insert();
    }

	/**
	 * Returns {@code true} iff {@code test} kills {@code mutant}.
	 * 
	 * @param test
	 * @param mutant
	 * @return
	 */
	public static boolean testVsMutant(Test test, Mutant mutant) {
		// Check if the test vs mutant was already executed
		TargetExecution executedTarget = DatabaseAccess.getTargetExecutionForPair(test.getId(), mutant.getId());

		if (executedTarget == null) {
			// Run the test against the mutant and get the result
			executedTarget = AntRunner.testMutant(mutant, test);
		}

		if (executedTarget == null) {
			logger.error("No execution result found for (m: {},t: {})", mutant.getId(), test.getId());
			// TODO Maybe an exception shall be thrown here?
			return false;
		}

		// If the test did NOT pass, the mutant was detected and should be
		// killed
		if (executedTarget.status.equals("FAIL") || executedTarget.status.equals("ERROR")) {
			// This returns true ONLY if the mutant in the DB is still alive
			if (mutant.kill(ASSUMED_NO)) {
				logger.info("Test {} kills Mutant {}", test.getId(), mutant.getId());
				// Increment the score for the test
				test.killMutant();
			} else {
				logger.info("Test {} would have killed Mutant {}, but Mutant {} was alredy dead!", test.getId(),
						mutant.getId(), mutant.getId());
			}
			return true;
		} else {
			logger.debug("Test {} did not kill Mutant {}", test.getId(), mutant.getId());
		}
		return false;
	}

    /**
     * Runs an equivalence test using an attacker supplied test and a mutant thought to be equivalent.
     * Kills mutant either with ASSUMED_YES if test passes on the mutant or with PROVEN_NO otherwise
     *
     * @param test   attacker-created test
     * @param mutant a mutant
     */
    public static void runEquivalenceTest(Test test, Mutant mutant) {
        logger.info("Running equivalence test for test {} and mutant {}.", test.getId(), mutant.getId());
        // The test created is new and was made by the attacker (there is no
        // need to check if the mutant/test pairing has been run already)

        // As a result of this test, either the test the attacker has written
        // kills the mutant or doesnt.
        TargetExecution executedTarget = AntRunner.testMutant(mutant, test);

        // Kill the mutant if it was killed by the test or if it's marked
        // equivalent
        if (executedTarget.status.equals("ERROR") || executedTarget.status.equals("FAIL")) {
            // If the test did NOT pass, the mutant was detected and is proven
            // to be non-equivalent
        	if (mutant.kill(PROVEN_NO)) {
        		logger.info("Test {} kills mutant {} and resolve equivalence.", test.getId(), mutant.getId());
        		test.killMutant();
        	} else {
				logger.info("Test {} would have killed Mutant {} and resolve equivalence, but Mutant {} was alredy dead. No need to resolve equivalence.!", test.getId(), mutant.getId(), mutant.getId());
			}
        } else {
            // If the test DID pass, the mutant went undetected and it is
            // assumed to be equivalent.
            // Avoid killing, let player accept as equivalent instead
            // mutant.kill(ASSUMED_YES);
        }
    }

}