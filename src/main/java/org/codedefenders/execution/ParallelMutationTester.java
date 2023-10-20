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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.apache.commons.collections.CollectionUtils;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.scoring.Scorer;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.util.Constants.MUTANT_ALIVE_1_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_ALIVE_N_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_KILLED_BY_TEST_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_SUBMITTED_MESSAGE;

/**
 * This is a parallel implementation of IMutationTester. Parallelism is achieved
 * by means of the injected executionService
 *
 * <p>We inject instances using {@link MutationTesterProducer}
 */
public class ParallelMutationTester extends MutationTester {
    private static final Logger logger = LoggerFactory.getLogger(ParallelMutationTester.class);

    private final ExecutorService testExecutorThreadPool;

    // TODO Move the Executor service before useMutantCoverage
    public ParallelMutationTester(BackendExecutorService backend, UserRepository userRepo, EventDAO eventDAO,
            boolean useMutantCoverage, ExecutorService testExecutorThreadPool) {
        super(backend, userRepo, eventDAO, useMutantCoverage);
        this.testExecutorThreadPool = testExecutorThreadPool;
    }

    @Override
    public String runTestOnAllMultiplayerMutants(MultiplayerGame game, Test test) {
        int killed = 0;
        List<Mutant> mutants = game.getAliveMutants();
        mutants.addAll(game.getMutantsMarkedEquivalentPending());
        List<Mutant> killedMutants = new ArrayList<>();

        // Acquire and release the connection
        Optional<UserEntity> u = userRepo.getUserIdForPlayerId(test.getPlayerId()).flatMap(userId -> userRepo.getUserById(userId));
        if (u.isEmpty()) {
            // TODO
            throw new RuntimeException();
        }

        // Fork and Join parallelization
        Map<Mutant, FutureTask<Boolean>> tasks = new HashMap<>();
        for (final Mutant mutant : mutants) {
            if (useMutantCoverage && !test.isMutantCovered(mutant)) {
                // System.out.println("Skipping non-covered mutant "
                // + mutant.getId() + ", test " + test.getId());
                continue;
            }

            FutureTask<Boolean> task = new FutureTask<>(() -> {
                // This automatically update the 'mutants' and 'tests'
                // tables, as well as the test and mutant objects.
                return testVsMutant(test, mutant);
            });

            // This is for checking later
            tasks.put(mutant, task);

            testExecutorThreadPool.execute(task);
        }

        // TODO Mayse use some timeout ?!
        for (final Mutant mutant : mutants) {
            if (useMutantCoverage && !test.isMutantCovered(mutant)) {
                continue;
            }

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

        for (Mutant mutant : mutants) {
            if (mutant.isAlive()) {
                ArrayList<Test> missedTests = new ArrayList<>();

                for (int lm : mutant.getLines()) {
                    boolean found = false;
                    for (int lc : test.getLineCoverage().getLinesCovered()) {
                        if (lc == lm) {
                            found = true;
                            missedTests.add(test);
                        }
                    }
                    if (found) {
                        break;
                    }
                }
                // mutant.setScore(Scorer.score(game, mutant, missedTests));
                // mutant.update();
                int score = Scorer.score(game, mutant, missedTests);
                MutantDAO.incrementMutantScore(mutant, score);
            }
        }

        // test.setScore(Scorer.score(game, test, killedMutants));
        // test.update();
        int score = Scorer.score(game, test, killedMutants);
        TestDAO.incrementTestScore(test, score);

        if (killed > 0) {
            insertDefenderKilledMutantEvent(game.getId(), u.get(), killed);
        }
        return getTestOnMutantResultString(mutants, killed);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.codedefenders.execution.IMutationTester#runAllTestsOnMutant(org.
     * codedefenders.game.AbstractGame, org.codedefenders.game.Mutant,
     * java.util.ArrayList, org.codedefenders.execution.TestScheduler)
     */
    @Override
    public String runAllTestsOnMutant(AbstractGame game, Mutant mutant, TestScheduler scheduler) {
        // Schedule the executable tests submitted by the defenders only (true)
        List<Test> tests = scheduler.scheduleTests(game.getTests(true));

        Optional<UserEntity> u = userRepo.getUserIdForPlayerId(mutant.getPlayerId()).flatMap(userId -> userRepo.getUserById(userId));
        if (u.isEmpty()) {
            // TODO
            throw new RuntimeException();
        }

        final Map<Test, FutureTask<Boolean>> tasks = new HashMap<>();
        for (Test test : tests) {
            if (useMutantCoverage && !test.isMutantCovered(mutant)) {
                logger.info("Skipping non-covered mutant " + mutant.getId() + ", test " + test.getId());
                continue;
            }

            FutureTask<Boolean> task = new FutureTask<>(() -> {
                logger.info("Executing mutant " + mutant.getId() + ", test " + test.getId());
                // TODO Is this testVsMutant thread safe?
                return testVsMutant(test, mutant);
            });

            // Book keeping
            tasks.put(test, task);
        }

        // Submit all the tests in the given order
        for (Test test : tests) {
            if (tasks.containsKey(test)) {
                logger.debug("MutationTester.runAllTestsOnMutant() : Scheduling Task " + test);
                testExecutorThreadPool.execute(tasks.get(test));
            }
        }

        // Wait for the result. Check by the order defined by the scheduler.
        for (Test test : tests) {
            // Why this happens ?
            if (!tasks.containsKey(test)) {
                logger.debug("Tasks does not contain " + test.getId());
                continue;
            }

            Future<Boolean> task = tasks.get(test);
            logger.debug("MutationTester.runAllTestsOnMutant() Checking task " + task + ". Done: " + task.isDone()
                    + ". Cancelled: " + task.isCancelled());
            try {

                boolean hasTestkilledTheMutant = false;

                try {
                    hasTestkilledTheMutant = task.get();
                } catch (CancellationException ce) {
                    logger.warn("Swallowing ", ce);
                }

                if (hasTestkilledTheMutant) {
                    // This test killed the mutant...

                    if (game instanceof MultiplayerGame) {
                        ArrayList<Mutant> mlist = new ArrayList<>();
                        mlist.add(mutant);

                        logger.info(">> Test {} kills mutant {} get {} points. Mutant is still alive ? {}",
                                test.getId(), mutant.getId(), Scorer.score((MultiplayerGame) game, test, mlist),
                                mutant.isAlive());

                        int score = Scorer.score((MultiplayerGame) game, test, mlist);
                        TestDAO.incrementTestScore(test, score);
                    }

                    Event notif = new Event(-1, game.getId(), userRepo.getUserIdForPlayerId(test.getPlayerId()).orElse(0),
                            u.get().getUsername() + "&#39;s mutant is killed", EventType.DEFENDER_KILLED_MUTANT,
                            EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
                    eventDAO.insert(notif);

                    // Early return. No need to check for the other executions.
                    return String.format(MUTANT_KILLED_BY_TEST_MESSAGE, test.getId());
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.warn(
                        "MutationTester.runAllTestsOnMutant() ERROR While waiting results for task " + e.getMessage());
                // e.printStackTrace();
            }
        }

        // TODO In the original implementation (see commit
        // 4fbdc78304374ee31a06d56f8ce67ca80309e24c for example)
        // the first block and the second one are swapped. Why ?
        ArrayList<Test> missedTests = new ArrayList<>();
        if (game instanceof MultiplayerGame) {
            for (Test t : tests) {
                if (CollectionUtils.containsAny(t.getLineCoverage().getLinesCovered(), mutant.getLines())) {
                    missedTests.add(t);
                }
            }
            // mutant.setScore(1 + Scorer.score((MultiplayerGame) game, mutant,
            // missedTests));
            // mutant.update();
            int score = 1 + Scorer.score((MultiplayerGame) game, mutant, missedTests);
            MutantDAO.incrementMutantScore(mutant, score);
        }

        Event notif = new Event(-1, game.getId(), u.get().getId(), u.get().getUsername() + "&#39;s mutant survives the test suite.",
                EventType.ATTACKER_MUTANT_SURVIVED, EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
        eventDAO.insert(notif);

        int nbRelevantTests = missedTests.size();
        // Mutant survived
        if (nbRelevantTests == 0) {
            return MUTANT_SUBMITTED_MESSAGE;
        } else if (nbRelevantTests <= 1) {
            return MUTANT_ALIVE_1_MESSAGE;
        } else {
            return String.format(MUTANT_ALIVE_N_MESSAGE, nbRelevantTests);
        }
    }

}
