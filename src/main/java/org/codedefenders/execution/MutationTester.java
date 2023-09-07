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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.collections.CollectionUtils;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.scoring.Scorer;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.execution.TargetExecution.Status.ERROR;
import static org.codedefenders.execution.TargetExecution.Status.FAIL;
import static org.codedefenders.game.Mutant.Equivalence.ASSUMED_NO;
import static org.codedefenders.game.Mutant.Equivalence.PROVEN_NO;
import static org.codedefenders.util.Constants.MUTANT_ALIVE_1_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_ALIVE_N_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_KILLED_BY_TEST_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_SUBMITTED_MESSAGE;
import static org.codedefenders.util.Constants.TEST_KILLED_LAST_MESSAGE;
import static org.codedefenders.util.Constants.TEST_KILLED_N_MESSAGE;
import static org.codedefenders.util.Constants.TEST_KILLED_ONE_MESSAGE;
import static org.codedefenders.util.Constants.TEST_KILLED_ZERO_MESSAGE;
import static org.codedefenders.util.Constants.TEST_SUBMITTED_MESSAGE;

/**
 * Class that handles compilation and testing by creating a Process with the
 * relevant ant target. Since execution is related to single request, we use a
 * Request Scope.
 *
 * <p>We inject instances using {@link MutationTesterProducer}.
 */
public class MutationTester implements IMutationTester {
    private static final Logger logger = LoggerFactory.getLogger(MutationTester.class);

    protected BackendExecutorService backend;
    protected EventDAO eventDAO;
    protected UserRepository userRepo;

    protected boolean useMutantCoverage;

    public MutationTester(BackendExecutorService backend, UserRepository userRepo, EventDAO eventDAO,
            boolean useMutantCoverage) {
        this.backend = backend;
        this.eventDAO = eventDAO;
        this.userRepo = userRepo;
        this.useMutantCoverage = useMutantCoverage;
    }

    @Override
    public String runTestOnAllMutants(AbstractGame game, Test test) {
        int killed = 0;
        List<Mutant> mutants = game.getAliveMutants();
        for (Mutant mutant : mutants) {
            killed += testVsMutant(test, mutant) ? 1 : 0;
        }

        return getTestOnMutantResultString(mutants, killed);
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

        for (Mutant mutant : mutants) {
            // Skip mutants which changed lines does not match with covered lines by the test
            // Exception for mutants who modify static variables (which also requires a recompile)
            if (!mutant.doesRequireRecompilation() && useMutantCoverage && !test.isMutantCovered(mutant)) {
                // System.out.println("Skipping non-covered mutant "
                // + mutant.getId() + ", test " + test.getId());
                continue;
            }

            if (testVsMutant(test, mutant)) {
                killed++;
                killedMutants.add(mutant);
            }
        }

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
        TestDAO.incrementTestScore(test, Scorer.score(game, test, killedMutants));

        if (killed > 0) {
            insertDefenderKilledMutantEvent(game.getId(), u.get(), killed);
        }
        return getTestOnMutantResultString(mutants, killed);
    }

    @Override
    public String runTestOnAllMeleeMutants(MeleeGame game, Test test) {

        int testOwnerPlayerId = test.getPlayerId();
        Optional<UserEntity> u = userRepo.getUserIdForPlayerId(testOwnerPlayerId).flatMap(userId -> userRepo.getUserById(userId));
        if (u.isEmpty()) {
            // TODO
            throw new RuntimeException();
        }

        List<Mutant> mutants = game.getAliveMutants().stream().filter(m -> m.getPlayerId() != testOwnerPlayerId)
                .collect(Collectors.toList());

        List<Mutant> mutantsMarkedEquivalent = game.getMutantsMarkedEquivalentPending().stream()
                .filter(m -> m.getPlayerId() != testOwnerPlayerId).collect(Collectors.toList());

        mutants.addAll(mutantsMarkedEquivalent);

        int killed = 0;
        List<Mutant> killedMutants = new ArrayList<>();

        for (Mutant mutant : mutants) {
            if (useMutantCoverage && !test.isMutantCovered(mutant)) {
                continue;
            }

            // Notify each and every mutant killed and survived
            if (testVsMutant(test, mutant)) {
                killed++;
                killedMutants.add(mutant);
                Event scoreEvent = new Event(-1, game.getId(), Constants.DUMMY_CREATOR_USER_ID,
                        test.getId() + ":" + mutant.getId(),
                        EventType.PLAYER_KILLED_MUTANT, EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
                eventDAO.insert(scoreEvent);
            } else {
                Event scoreEvent = new Event(-1, game.getId(), Constants.DUMMY_CREATOR_USER_ID,
                        test.getId() + ":" + mutant.getId(),
                        EventType.PLAYER_MUTANT_SURVIVED, EventStatus.GAME,
                        new Timestamp(System.currentTimeMillis()));
                eventDAO.insert(scoreEvent);
            }
        }

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
            }
        }

        if (killed > 0) {
            insertDefenderKilledMutantEvent(game.getId(), u.get(), killed);
        }
        return getTestOnMutantResultString(mutants, killed);
    }

    protected void insertDefenderKilledMutantEvent(int gameId, @Nonnull UserEntity user, int killedMutantCount) {
        Event notif = new Event(-1, gameId, user.getId(),
                user.getUsername() + "&#39;s test kills " + killedMutantCount + " " + "mutants.",
                EventType.DEFENDER_KILLED_MUTANT, EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
        eventDAO.insert(notif);
    }

    @Nonnull
    protected static String getTestOnMutantResultString(@Nonnull List<Mutant> mutants, int killed) {
        if (killed == 0) {
            if (mutants.size() == 0) {
                return TEST_SUBMITTED_MESSAGE;
            } else {
                return TEST_KILLED_ZERO_MESSAGE;
            }
        } else {
            if (killed == 1) {
                if (mutants.size() == 1) {
                    return TEST_KILLED_LAST_MESSAGE;
                } else {
                    return TEST_KILLED_ONE_MESSAGE;
                }
            } else {
                return String.format(TEST_KILLED_N_MESSAGE, killed);
            }
        }
    }

    @Override
    public String runAllTestsOnMutant(AbstractGame game, Mutant mutant) {
        return runAllTestsOnMutant(game, mutant, new RandomTestScheduler());
    }

    // TODO This clashes with our abstraction as for melee games we need to double
    // check tests and mutants do not belong to the same user
    @Override
    public String runAllTestsOnMutant(AbstractGame game, Mutant mutant, TestScheduler scheduler) {
        // Schedule the executable tests submitted by the defenders only (true)
        List<Test> tests = scheduler.scheduleTests(game.getTests(true));

        Optional<UserEntity> u = userRepo.getUserIdForPlayerId(mutant.getPlayerId()).flatMap(userId -> userRepo.getUserById(userId));
        if (u.isEmpty()) {
            // TODO
            throw new RuntimeException();
        }

        for (Test test : tests) {
            if (useMutantCoverage && !test.isMutantCovered(mutant)) {
                logger.info("Skipping non-covered mutant " + mutant.getId() + ", test " + test.getId());
                continue;
            }

            if (testVsMutant(test, mutant)) {
                logger.info("Test {} kills mutant {}", test.getId(), mutant.getId());
                if (game instanceof MultiplayerGame) {
                    ArrayList<Mutant> mlist = new ArrayList<>();
                    mlist.add(mutant);
                    // test.setScore(Scorer.score((MultiplayerGame) game, test,
                    // mlist));
                    // test.update();
                    int score = Scorer.score((MultiplayerGame) game, test, mlist);
                    TestDAO.incrementTestScore(test, score);
                }

                Event notif = new Event(-1, game.getId(), userRepo.getUserIdForPlayerId(test.getPlayerId()).orElse(0),
                        u.get().getUsername() + "&#39;s mutant is killed", EventType.DEFENDER_KILLED_MUTANT, EventStatus.GAME,
                        new Timestamp(System.currentTimeMillis()));
                eventDAO.insert(notif);

                // return as soon as the first test kills the mutant we return
                return String.format(MUTANT_KILLED_BY_TEST_MESSAGE, test.getId());
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

    /**
     * Runs a test against a mutant.
     *
     * @param test   The test to run
     * @param mutant The mutant we run the test against
     * @return {@code true} if the test killed the mutant, {@code false} otherwise
     */
    public boolean testVsMutant(Test test, Mutant mutant) {
        if (TargetExecutionDAO.getTargetExecutionForPair(test.getId(), mutant.getId()) != null) {
            logger.error("Execution result found for Mutant {} and Test {}.", mutant.getId(), test.getId());
            return false;
        }

        final TargetExecution executedTarget = backend.testMutant(mutant, test);

        Integer gameId = test.getGameId();
        String scoringMessage = "%d:%d".formatted(test.getId(), mutant.getId());
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        // If the test did NOT pass, the mutant was detected and should be
        // killed.
        if (!executedTarget.status.equals(FAIL) && !executedTarget.status.equals(ERROR)) {
            logger.debug("Test {} did not kill Mutant {}", test.getId(), mutant.getId());
            // Mutant survived
            return false;
        }
        if (!MutantDAO.killMutant(mutant, ASSUMED_NO)) {
            logger.info("Test {} would have killed Mutant {}, but Mutant {} was already dead!", test.getId(),
                    mutant.getId(), mutant.getId());
            return false;
        }

        logger.info("Test {} killed Mutant {}", test.getId(), mutant.getId());
        TestDAO.killMutant(test);
        mutant.setKillMessage(executedTarget.message);
        MutantDAO.updateMutantKillMessageForMutant(mutant);

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.codedefenders.execution.IMutationTester#runEquivalenceTest(org.
     * codedefenders.game.Test, org.codedefenders.game.Mutant)
     */
    @Override
    public void runEquivalenceTest(Test test, Mutant mutant) {
        logger.info("Running equivalence test for test {} and mutant {}.", test.getId(), mutant.getId());
        // The test created is new and was made by the attacker (there is no
        // need to check if the mutant/test pairing has been run already)

        // As a result of this test, either the test the attacker has written
        // kills the mutant or doesnt.
        TargetExecution executedTarget = backend.testMutant(mutant, test);

        // Kill the mutant if it was killed by the test or if it's marked
        // equivalent
        if (executedTarget.status.equals(ERROR) || executedTarget.status.equals(FAIL)) {
            // If the test did NOT pass, the mutant was detected and is proven
            // to be non-equivalent
            if (MutantDAO.killMutant(mutant, PROVEN_NO)) {
                logger.info("Test {} kills mutant {} and resolve equivalence.", test.getId(), mutant.getId());
                TestDAO.killMutant(test);
                mutant.setKillMessage(executedTarget.message);
                MutantDAO.updateMutantKillMessageForMutant(mutant);
            } else {
                logger.info(
                        "Test {} would have killed Mutant {} and resolve equivalence,"
                                + "but Mutant {} was alredy dead. No need to resolve equivalence.!",
                        test.getId(), mutant.getId(), mutant.getId());
            }
        } else {
            // If the test DID pass, the mutant went undetected and it is
            // assumed to be equivalent.
            // Avoid killing, let player accept as equivalent instead
            // mutant.kill(ASSUMED_YES);
        }
    }

    @Override
    public String runAllTestsOnMeleeMutant(MeleeGame game, Mutant mutant) {
        return runAllTestsOnMeleeMutant(game, mutant, new RandomTestScheduler());
    }

    private String runAllTestsOnMeleeMutant(MeleeGame game, Mutant mutant, TestScheduler scheduler) {

        int mutantOwnerPlayerId = mutant.getPlayerId();
        Optional<UserEntity> u = userRepo.getUserIdForPlayerId(mutantOwnerPlayerId).flatMap(userId -> userRepo.getUserById(userId));
        if (u.isEmpty()) {
            // TODO
            throw new RuntimeException();
        }

        // Get all the tests from the game which DO NOT belong to mutantOwnerPlayerId
        // Note we need to return the test for all the roles to include PLAYER
        List<Test> testsToExecute = game.getAllTests().stream().filter(t -> t.getPlayerId() != mutantOwnerPlayerId)
                .collect(Collectors.toList());
        List<Test> tests = scheduler.scheduleTests(testsToExecute);

        /// Follows duplicate code

        for (Test test : tests) {
            if (useMutantCoverage && !test.isMutantCovered(mutant)) {
                logger.info("Skipping non-covered mutant " + mutant.getId() + ", test " + test.getId());
                continue;
            }

            if (testVsMutant(test, mutant)) {
                logger.info("Test {} kills mutant {}", test.getId(), mutant.getId());
                ArrayList<Mutant> mlist = new ArrayList<>();
                mlist.add(mutant);
                // TODO Scoring MeleeGames is still open
                //test.incrementScore(Scorer.score(game, test, mlist));

                Event notif = new Event(-1, game.getId(), userRepo.getUserIdForPlayerId(test.getPlayerId()).orElse(0),
                        u.get().getUsername() + "&#39;s mutant is killed", EventType.PLAYER_KILLED_MUTANT, EventStatus.GAME,
                        new Timestamp(System.currentTimeMillis()));
                eventDAO.insert(notif);

                Event scoreEvent = new Event(-1, game.getId(), Constants.DUMMY_CREATOR_USER_ID,
                        test.getId() + ":" + mutant.getId(),
                        EventType.PLAYER_KILLED_MUTANT, EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
                eventDAO.insert(scoreEvent);

                // return as soon as the first test kills the mutant
                return String.format(MUTANT_KILLED_BY_TEST_MESSAGE, test.getId());
            } else {
                // Notify mutant survived for each test !
                Event scoreEvent = new Event(-1, game.getId(), Constants.DUMMY_CREATOR_USER_ID,
                        test.getId() + ":" + mutant.getId(),
                        EventType.PLAYER_MUTANT_SURVIVED, EventStatus.GAME,
                        new Timestamp(System.currentTimeMillis()));
                eventDAO.insert(scoreEvent);
            }
        }

        // TODO In the original implementation (see commit
        // 4fbdc78304374ee31a06d56f8ce67ca80309e24c for example)
        // the first block and the second one are swapped. Why ?
        ArrayList<Test> missedTests = new ArrayList<>();
        for (Test t : tests) {
            if (CollectionUtils.containsAny(t.getLineCoverage().getLinesCovered(), mutant.getLines())) {
                missedTests.add(t);
            }
        }
        Event notif = new Event(-1, game.getId(), u.get().getId(), u.get().getUsername() + "&#39;s mutant survives the test suite.",
                EventType.PLAYER_MUTANT_SURVIVED, EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
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
