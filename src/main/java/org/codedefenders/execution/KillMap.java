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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.lang3.ArrayUtils;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.util.CDIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.execution.KillMap.KillMapEntry.Status.KILL;
import static org.codedefenders.execution.KillMap.KillMapEntry.Status.NO_KILL;
import static org.codedefenders.execution.KillMap.KillMapEntry.Status.UNKNOWN;

/**
 * Maps tests to their killed mutants in a finished game.
 * Killmaps are computed and saved to the DB on the first time they are requested. This may take a long time.
 * {@link KillmapDAO#hasKillMap(int)}  can be used to check if a finished game's killmap has already been computed.
 * <p/>
 * Only one killmap can be computed at a time. Further request are queued via {@code synchronized}.
 * This is mostly to prevent multiple calculations of the same killmap at once, e.g. by accidentally refreshing a page.
 */
public class KillMap {

    private static final Logger logger = LoggerFactory.getLogger(KillMap.class);

    // @Inject // This does not work for static classes
    private static BackendExecutorService backend;

    private static boolean USE_COVERAGE;
    private static boolean PARALLELIZE;
    private static int NUM_THREADS;

    static {
        /* Get the BackendExecutorService and Configuration since dependency injection does not work on this class. */
        try {
            backend = CDIUtil.getBeanFromCDI(BackendExecutorService.class);
            Configuration config = CDIUtil.getBeanFromCDI(Configuration.class);
            USE_COVERAGE = config.isMutantCoverage();
            PARALLELIZE = config.isParallelize();
            NUM_THREADS = config.getNumberOfKillmapThreads();
        } catch (IllegalStateException e) {
            // TODO
        }
        /*
         * If we are running this outside a container, DI must be done manually by looking up the JNDI resource.
         */
        if (backend == null) {
            try {
                InitialContext initialContext = new InitialContext();
                backend = (BackendExecutorService) initialContext.lookup("java:comp/env/codedefenders/backend");
            } catch (NamingException e) {
                logger.error("Could not acquire BackendExecutorService", e);
            }
        }
    }

    /**
     * The tests the killmap is computed for.
     */
    private List<Test> tests;
    /**
     * The mutants the killmap is computed for.
     */
    private List<Mutant> mutants;
    /**
     * ID of the class the killmap is computed for.
     */
    private int classId;
    /**
     * Maps each test to it's index in {@link KillMap#tests}.
     */
    private Map<Test, Integer> indexOfTest;
    /**
     * Maps each mutant to it's index in {@link KillMap#mutants}.
     */
    private Map<Mutant, Integer> indexOfMutant;

    /**
     * The killmap data, as a list of "test vs. mutant" execution results.
     */
    private List<KillMapEntry> entries;
    /**
     * The killmap data, as matrix between tests and mutants.
     */
    private KillMapEntry[][] matrix;


    /**
     * Constructs a new killmap.
     *
     * @param tests   The tests of the killmap.
     * @param mutants The mutants of the killmap.
     * @param classId The id of the class the tests and mutants are for.
     * @param entries The already computed entries of the killmap. If no entries have been computed before, this can be
     *                an empty list.
     */
    private KillMap(List<Test> tests, List<Mutant> mutants, int classId, List<KillMapEntry> entries) {
        this.tests = new ArrayList<>(tests);
        this.mutants = new ArrayList<>(mutants);
        this.classId = classId;
        this.indexOfTest = new TreeMap<>(Test.orderByIdDescending());
        this.indexOfMutant = new TreeMap<>(Mutant.orderByIdDescending());

        this.entries = entries;
        this.matrix = new KillMapEntry[tests.size()][mutants.size()];

        /* Fill the maps and the matrix. */
        for (int i = 0; i < this.tests.size(); i++) {
            this.indexOfTest.put(tests.get(i), i);
        }
        for (int i = 0; i < this.mutants.size(); i++) {
            this.indexOfMutant.put(mutants.get(i), i);
        }
        for (KillMapEntry entry : entries) {
            Integer testIndex = indexOf(entry.test);
            Integer mutantIndex = indexOf(entry.mutant);
            if (testIndex != null && mutantIndex != null) {
                matrix[indexOf(entry.test)][indexOf(entry.mutant)] = entry;
            }
        }
    }

    /**
     * Computes the missing entries of the killmap.
     *
     * @throws InterruptedException If the computation is interrupted.
     * @throws ExecutionException   If an error occurred during an execution.
     */
    private void compute(ExecutorService executor) throws InterruptedException, ExecutionException {
        Instant startTime = Instant.now();

        List<Future<KillMapEntry>> executionResults = new LinkedList<>();

        if (Thread.currentThread().isInterrupted()) {
            executor.shutdownNow();
            throw new InterruptedException("Got interrupted before submitting tasks");
        }

        for (int t = 0; t < tests.size(); t++) {
            Test test = tests.get(t);
            for (int m = 0; m < mutants.size(); m++) {
                Mutant mutant = mutants.get(m);
                if ((matrix[t][m] == null)) {
                    executionResults.add(executor.submit(new TestVsMutantCallable(test, mutant, classId)));
                }
            }
        }

        if (Thread.currentThread().isInterrupted()) {
            executor.shutdownNow();
            throw new InterruptedException("Got interrupted after submitting tasks");
        }

        executor.shutdown();

        for (Future<KillMapEntry> result : executionResults) {
            try {
                KillMapEntry entry = result.get();
                entries.add(entry);
                matrix[indexOf(entry.test)][indexOf(entry.mutant)] = entry;
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
                throw new InterruptedException("Got interrupted while waiting for results");
            }
        }

        logger.info("Computation of killmap finished after " + Duration.between(startTime, Instant.now()).getSeconds()
                + " seconds");
    }

    /**
     * Returns the killmap for the given finished game.
     * This operation is blocking and may take a long time.
     *
     * @param game The finished game to get the killmap for.
     * @throws InterruptedException If the computation is interrupted.
     * @throws ExecutionException   If an error occurred during an execution.
     */
    public static KillMap forGame(AbstractGame game) throws InterruptedException, ExecutionException {
        List<Test> tests = game.getTests();
        List<Mutant> mutants = game.getMutants();
        List<KillMapEntry> entries = KillmapDAO.getKillMapEntriesForGame(game.getId());
        KillMap killmap = new KillMap(tests, mutants, game.getClassId(), entries);

        if (tests.size() * mutants.size() != entries.size()) {
            /* Synchronized, so only one killmap can is computed at a time. */
            synchronized (KillMap.class) {
                logger.info(String.format("Computing killmap for %s game %d: %d tests, %d mutants, %d entries provided",
                        game.getMode(), game.getId(), tests.size(), mutants.size(), entries.size()));

                // TODO refactor this into a CDI
                ExecutorService executor = PARALLELIZE
                        ? Executors.newFixedThreadPool(NUM_THREADS)
                        : Executors.newSingleThreadExecutor();

                killmap.compute(executor);

                if (game.isFinished()) {
                    KillmapDAO.setHasKillMap(game.getId(), true);
                }
            }
        } else {
            logger.info("Killmap for game " + game.getId() + " already computed");
        }

        return killmap;
    }

    /**
     * Returns the killmap for the given class.
     * This operation is blocking and may take a long time.
     *
     * @param classId The class to get the killmap for.
     * @throws InterruptedException If the computation is interrupted.
     * @throws ExecutionException   If an error occurred during an execution.
     */
    public static KillMap forClass(int classId) throws InterruptedException, ExecutionException {
        List<Test> tests = TestDAO.getValidTestsForClass(classId);
        List<Mutant> mutants = MutantDAO.getValidMutantsForClass(classId);
        List<KillMapEntry> entries = KillmapDAO.getKillMapEntriesForClass(classId);
        KillMap killmap = new KillMap(tests, mutants, classId, entries);

        if (tests.size() * mutants.size() != entries.size()) {
            /* Synchronized, so only one killmap is computed at a time. */
            synchronized (KillMap.class) {
                logger.info(String.format("Computing killmap for class %d: %d tests, %d mutants, %d entries provided",
                        classId, tests.size(), mutants.size(), entries.size()));
                // TODO refactor this into a CDI
                ExecutorService executor = PARALLELIZE
                        ? Executors.newFixedThreadPool(NUM_THREADS)
                        : Executors.newSingleThreadExecutor();

                killmap.compute(executor);
            }
        } else {
            logger.info("Killmap for class " + classId + " already computed");
        }

        return killmap;
    }

    /**
     * Returns the killmap for the given test and mutants.
     * The tests and mutants must belong to the same class (with the same class id).
     * This operation is blocking and may take a long time,
     *
     * @param tests   The tests to get the killmap for. The list must not include a mutant twice.
     * @param mutants The mutants to get the killmap for. The list must not include a test twice.
     * @param classId The class id of the class the tests and mutants belong to.
     * @param entries Already computed entries for the killmap.
     * @throws InterruptedException If the computation is interrupted.
     * @throws ExecutionException   If an error occurred during an execution.
     */
    public static KillMap forCustom(List<Test> tests, List<Mutant> mutants, int classId, List<KillMapEntry> entries)
            throws InterruptedException, ExecutionException {
        /* Synchronized, so only one killmap can be computed at a time. */
        synchronized (KillMap.class) {
            KillMap killmap = new KillMap(tests, mutants, classId, entries);

            if (tests.size() * mutants.size() != entries.size()) {
                /* Synchronized, so only one killmap is computed at a time. */
                synchronized (KillMap.class) {
                    logger.info(String.format(
                            "Computing custom killmap (class %d): %d tests, %d mutants, %d entries provided",
                            classId, tests.size(), mutants.size(), entries.size()));
                    // TODO refactor this into a CDI
                    ExecutorService executor = PARALLELIZE
                            ? Executors.newFixedThreadPool(NUM_THREADS)
                            : Executors.newSingleThreadExecutor();

                    killmap.compute(executor);
                }
            } else {
                logger.info("Custom killmap for class " + classId + " already computed");
            }

            return killmap;
        }
    }

    /**
     * Returns the killmap for the given tests against a mutant to validate if the claimed equivalent
     * mutant is killable.
     *
     * <p>The tests and mutants must belong to the same class (with the same class id).
     *
     * <p><bf>This operation is blocking and may take a long time</bf>
     *
     * @param tests   The tests used for the validation.
     * @param mutant  The mutant to validate.
     * @param classId The class id of the class the tests and mutants belong to.
     */
    public static KillMap forMutantValidation(List<Test> tests, Mutant mutant, int classId) {
        List<KillMapEntry> entries = new ArrayList<>();
        KillMap killmap = new KillMap(tests, Arrays.asList(mutant), classId, entries);
        logger.debug("Validating mutant {} using custom killmap (partial results are stored in the db) using: {} tests",
                mutant, tests.size());

        // TODO this creates a surge in the load as the number of executor services might grow
        // by refactoring this into a CDI we should be able to easily control the situation and queue jobs.
        ExecutorService executor = PARALLELIZE
                ? Executors.newFixedThreadPool(NUM_THREADS)
                : Executors.newSingleThreadExecutor();

        try {
            killmap.compute(executor);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Exception while validating mutant {} using custom killmap", mutant.getId(), e);
            return null;
        }

        return killmap;
    }

    /**
     * Returns the tests of the killmap.
     *
     * @return The tests of the killmap.
     */
    public List<Test> getTests() {
        return new ArrayList<>(tests);
    }

    /**
     * Returns the mutants of the killmap.
     *
     * @return The mutants of the killmap.
     */
    public List<Mutant> getMutants() {
        return new ArrayList<>(mutants);
    }

    /**
     * Returns The results of all "test vs. mutant" executions.
     *
     * @return The results of all "test vs. mutant" executions.
     */
    public List<KillMapEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    /**
     * Returns a matrix that maps tests and mutants to their execution result.
     * The first dimension describes the tests, the second dimension describes the mutants.
     * {@link KillMap#indexOf(Test)} / {@link KillMap#indexOf(Mutant)} can be used to get the index of a test / mutant.
     *
     * @return A matrix that maps tests and mutants to their execution result.
     */
    public KillMapEntry[][] getMatrix() {
        return ArrayUtils.clone(matrix);
    }

    /**
     * Returns all "test vs. mutant" execution results for the given test.
     *
     * @param test The given test.
     * @return All "test vs. mutant" execution results for the given test.
     */
    public KillMapEntry[] getEntriesForTest(Test test) {
        return ArrayUtils.clone(matrix[indexOf(test)]);
    }

    /**
     * Returns all "test vs. mutant" execution results for the given mutant.
     *
     * @param mutant The given mutant.
     * @return All "test vs. mutant" execution results for the given mutant.
     */
    public KillMapEntry[] getEntriesForMutant(Mutant mutant) {
        int mutantIndex = indexOf(mutant);
        KillMapEntry[] result = new KillMapEntry[tests.size()];

        for (int i = 0; i < result.length; i++) {
            result[i] = matrix[i][mutantIndex];
        }

        return result;
    }

    /**
     * Returns the "test vs. mutant" execution result for the given test and mutant.
     *
     * @param test   The given test.
     * @param mutant The given mutant.
     * @return The "test vs. mutant" execution result for the given test and mutant.
     */
    public KillMapEntry getEntry(Test test, Mutant mutant) {
        return matrix[indexOf(test)][indexOf(mutant)];
    }

    /**
     * Returns the index of the given test in the matrix, or null if the mutant is not part of the matrix.
     *
     * @param test The given test.
     * @return The index of the given test in the matrix, or null if the mutant is not part of the matrix.
     */
    public Integer indexOf(Test test) {
        return indexOfTest.get(test);
    }

    /**
     * Returns the index of the given mutant in the matrix, or null if the mutant is not part of the matrix.
     *
     * @param mutant The given mutant.
     * @return The index of the given mutant in the matrix, or null if the mutant is not part of the matrix.
     */
    public Integer indexOf(Mutant mutant) {
        return indexOfMutant.get(mutant);
    }

    /**
     * Executes a test against a mutant, inserts the result into the DB, and returns the result.
     */
    private static class TestVsMutantCallable implements Callable<KillMapEntry> {
        private Test test;
        private Mutant mutant;
        private int classId;

        private TestVsMutantCallable(Test test, Mutant mutant, int classId) {
            this.test = test;
            this.mutant = mutant;
            this.classId = classId;
        }

        @Override
        public KillMapEntry call() {
            KillMapEntry entry;

            if (USE_COVERAGE && !test.isMutantCovered(mutant)) {
                entry = new KillMapEntry(test, mutant, KillMapEntry.Status.NO_COVERAGE);

            } else {
                TargetExecution executedTarget = backend.testMutant(mutant, test);
                KillMapEntry.Status status;

                switch (executedTarget.status) {
                    case FAIL:
                        status = KILL;
                        break;
                    case SUCCESS:
                        status = NO_KILL;
                        break;
                    case ERROR:
                        status = KillMapEntry.Status.ERROR;
                        break;
                    default:
                        status = UNKNOWN;
                        break;
                }

                entry = new KillMapEntry(test, mutant, status);
            }

            if (!KillmapDAO.insertKillMapEntry(entry, classId)) {
                logger.error("An error occurred while inserting killmap entry into the DB: " + entry);
            }

            return entry;
        }
    }

    /**
     * The killmap types. Game or class killmap.
     */
    public enum KillMapType {
        CLASS, GAME
    }

    /**
     * Represents a result of executing a test against a mutant.
     */
    public static class KillMapEntry {
        public enum Status {
            /**
             * Test kills mutant.
             */
            KILL,
            /**
             * Test covers mutant but doesn't kill it.
             */
            NO_KILL,
            /**
             * Test doesn't cover mutant. Only used if "mutant.coverage" is enabled.
             */
            NO_COVERAGE,
            /**
             * An error occurred during execution. If no errors occurred elsewhere, then this means,
             * that the test execution resulted in an exception (and the mutant was killed).
             */
            ERROR,
            /**
             * Status is unknown.
             */
            UNKNOWN
        }

        public Test test;
        public Mutant mutant;
        public Status status;

        public KillMapEntry(Test test, Mutant mutant, Status status) {
            this.test = test;
            this.mutant = mutant;
            this.status = status;
        }

        public String toString() {
            return String.format("Test %d - Mutant %d: %s", test.getId(), mutant.getId(), status.toString());
        }
    }
}
