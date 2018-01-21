package org.codedefenders;

import static org.codedefenders.Constants.MUTANT_ALIVE_1_MESSAGE;
import static org.codedefenders.Constants.MUTANT_ALIVE_N_MESSAGE;
import static org.codedefenders.Constants.MUTANT_KILLED_BY_TEST_MESSAGE;
import static org.codedefenders.Constants.MUTANT_SUBMITTED_MESSAGE;
import static org.codedefenders.Constants.TEST_KILLED_LAST_MESSAGE;
import static org.codedefenders.Constants.TEST_KILLED_N_MESSAGE;
import static org.codedefenders.Constants.TEST_KILLED_ONE_MESSAGE;
import static org.codedefenders.Constants.TEST_KILLED_ZERO_MESSAGE;
import static org.codedefenders.Constants.TEST_SUBMITTED_MESSAGE;
import static org.codedefenders.Mutant.Equivalence.ASSUMED_NO;
import static org.codedefenders.Mutant.Equivalence.PROVEN_NO;

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

import org.apache.commons.collections.CollectionUtils;
import org.codedefenders.duel.DuelGame;
import org.codedefenders.events.Event;
import org.codedefenders.events.EventStatus;
import org.codedefenders.events.EventType;
import org.codedefenders.multiplayer.MultiplayerGame;
import org.codedefenders.scoring.Scorer;
import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public static void runAllTestsOnMutant(AbstractGame game, Mutant mutant, ArrayList<String> messages) {
		List<Test> tests = game.getTests(true); // executable tests submitted by defenders

		User u = DatabaseAccess.getUserFromPlayer(mutant.getPlayerId());

		if (parallelize) {
			// Start all the test in parallel, but skip the ones already covered
			// Keep checking if a test killed a mutant, by iterating over the
			// completed
			// tasks. As soon as you find one, discard all the other results
			// (basically do not wait for them to complete
			// before

			// Book-keeping running tasks

			final Map<Test, FutureTask<Boolean>> tasks = new HashMap<Test, FutureTask<Boolean>>();
			// Assumption: submitting tasks is faster than processing tasks...
			for (Test test : tests) {
				if (useMutantCoverage && !test.isMutantCovered(mutant)) {
					logger.info("Skipping non-covered mutant " + mutant.getId() + ", test " + test.getId());
					continue;
				}

				FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {

					@Override
					public Boolean call() throws Exception {

						if (testVsMutant(test, mutant)) {
							// Notify the *other* tasks we already killed
							// the mutant. Note, we cannot call t.cancel on this
							// and then get its result...
							logger.debug("MutationTester Cancelling running tasks");
							for (Entry<Test, FutureTask<Boolean>> e : tasks.entrySet()) {

								if (e.getKey().equals(test)) {
									logger.debug("MutationTester.worker: skipping myself ");
									continue;
								}

								FutureTask<Boolean> t = e.getValue();

								if (!(t.isDone() || t.isCancelled())) {
									logger.debug("MutationTester Cancel task " + t);
									/*
									 * We do not really care if the task can be
									 * forcefully stopped...it's enough to be
									 * sure that when isDone is called.
									 * Additionally, this should not screw up
									 * this very same task
									 */
                                    t.cancel(false);
                                } else {
                                	logger.debug("MutationTester task " + t + " already cancelled or done");
                                }
                            }
                            return true;
                        } else {
                            return false;
                        }
                    }
                });

                // This is for checking later which test killed which mutant
                tasks.put(test, task);
            }

            // Start all
            for (Test test : tasks.keySet()) {
                if (tasks.containsKey(test)) {
                	logger.debug("MutationTester.runAllTestsOnMutant() : Scheduling Task " + test);
                    sharedExecutorService.execute(tasks.get(test));
                }
            }

            // Wait for the result
            for (Test test : tasks.keySet()) {
                Future<Boolean> task = tasks.get(test);
                logger.debug("MutationTester.runAllTestsOnMutant() Checking task " + task + ". Done: "
                        + task.isDone() + ". Cancelled: " + task.isCancelled());
                try {

                    boolean hasTestkilledTheMutant = false;

                    // Since we might cancel a task at anytime but we need to
                    // get their result, this might raise an exception
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
							// test.setScore(Scorer.score((MultiplayerGame) game, test, mlist));
							// test.update();
							test.incrementScore(Scorer.score((MultiplayerGame) game, test, mlist));
						}

                        Event notif = new Event(-1, game.getId(),
                                DatabaseAccess.getUserFromPlayer(test.getPlayerId()).getId(),
                                u.getUsername() + "&#39;s mutant is killed", EventType.DEFENDER_KILLED_MUTANT,
                                EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
                        notif.insert();

                        // Early return. Just forget about the other running
                        // (yet cancelled) tasks
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

                // If this mutant/test pairing hasnt been run before and the
                // test
                // might kill the mutant
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

                    return; // return as soon as a test kills the mutant
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
    private static boolean testVsMutant(Test test, Mutant mutant) {
        if (DatabaseAccess.getTargetExecutionForPair(test.getId(), mutant.getId()) == null) {
            // Run the test against the mutant and get the result
            TargetExecution executedTarget = AntRunner.testMutant(mutant, test);

			// If the test did NOT pass, the mutant was detected and should be killed.
			if (executedTarget.status.equals("FAIL") || executedTarget.status.equals("ERROR")) {
				if (mutant.kill(ASSUMED_NO)) {
					logger.info("Test {} kills Mutant {}", test.getId(), mutant.getId());
					test.killMutant();
					return true;
				} else {
					logger.info("Test {} would have killed Mutant {}, but Mutant {} was alredy dead!", test.getId(), mutant.getId(), mutant.getId());
					return false;
				}
			} else {
				logger.info("Test {} did not kill Mutant {}", test.getId(), mutant.getId());
			}
		} else {
			logger.error("No execution result found for (m: {},t: {})", mutant.getId(), test.getId());
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