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

import org.apache.commons.lang.ArrayUtils;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;

import javax.naming.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;

import static org.codedefenders.execution.KillMap.KillMapEntry.Status.*;

/**
 * Maps tests to their killed mutants in a finished game.
 * Killmaps are computed and saved to the DB on the first time they are requested. This may take a long time.
 * {@link KillmapDAO#hasKillMap(int)}  can be used to check if a game's killmap has already beeen computed before.
 * <p/>
 * Only one killmap can be computed at a time. Further request are queued via {@code synchronized}.
 * This is mostly to prevent multiple calculations of the same killmap at once, e.g. by accidentally refreshing a page.
 */
public class KillMap {

    private static final Logger logger = LoggerFactory.getLogger(KillMap.class);

    private static boolean USE_COVERAGE = true;
    private static boolean PARALLELIZE = true;
    private final static int NUM_THREADS = 40;
    /* TODO Put this into config.properties? MutationTester also has hard-coded number of threads. */

    /* Get settings if they are set, otherwise use defaults. */
    static {
        InitialContext initialContext;
        try {
            initialContext = new InitialContext();
            Context environmentContext = (Context) initialContext.lookup("java:/comp/env");
            Object useCoverageObj = environmentContext.lookup("mutant.coverage");
            Object parallelizeObj = environmentContext.lookup("parallelize");
            USE_COVERAGE = (useCoverageObj == null) ? USE_COVERAGE : "enabled".equalsIgnoreCase((String) useCoverageObj);
            PARALLELIZE = (parallelizeObj == null) ? PARALLELIZE : "enabled".equalsIgnoreCase((String) parallelizeObj);
        } catch (NamingException e) {
            logger.error("Encountered missing option", e);
        }
    }

    /** Filter which allows every test-mutant combination. */
    private static final BiFunction<Test, Mutant, Boolean> NO_FILTER = (test, mutant) -> true;

    /** The tests the killmap is computed for. */
    private List<Test> tests;
    /** The mutants the killmap is computed for. */
    private List<Mutant> mutants;
    /** ID of the class the killmap is computed for. */
    private int classId;
    /** Maps each test to it's index in {@link KillMap#tests}. */
    private TreeMap<Test, Integer> indexOfTest;
    /** Maps each mutant to it's index in {@link KillMap#mutants}. */
    private TreeMap<Mutant, Integer> indexOfMutant;

    /** The killmap data, as a list of "test vs. mutant" execution results. */
    private List<KillMapEntry> entries;
    /** The killmap data, as matrix between tests and mutants. */
    private KillMapEntry[][] matrix;


    /**
     * Constructs a new killmap.
     *
     * @param tests The tests of the killmap.
     * @param mutants The mutants of the killmap.
     * @param classId The id of the class the tests and mutants are for.
     * @param entries The already computed entries of the killmap. If no entries have been computed before, this can be
     *                an empty list.
     * @param filter A filter, which decides what test-mutant combinations should be computed.
     */
    private KillMap(List<Test> tests, List<Mutant> mutants, int classId, List<KillMapEntry> entries,
                    BiFunction<Test, Mutant, Boolean> filter) {
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
            if (filter.apply(entry.test, entry.mutant)) {
                Integer testIndex = indexOf(entry.test);
                Integer mutantIndex = indexOf(entry.mutant);
                if (testIndex != null && mutantIndex != null) {
                    matrix[indexOf(entry.test)][indexOf(entry.mutant)] = entry;
                }
            }
        }
    }

    /**
     * Computed the missing entries of a killmaps, Or recalculates all entries, if {@code recalculate} is {@code true}.
     *
     * @param recalculate If {@code true}, recalculate all entries, Even if they were computed before.
     * @param filter A filter, which decides what test-mutant combinations should be computed.
     * @throws InterruptedException If the computation is interrupted.
     * @throws ExecutionException If an error occured during an execution.
     */
    private void compute(boolean recalculate, BiFunction<Test, Mutant, Boolean> filter) throws InterruptedException, ExecutionException {
        Instant startTime = Instant.now();

        List<Future<KillMapEntry>> executionResults = new LinkedList<>();
        ExecutorService executor = PARALLELIZE ? Executors.newFixedThreadPool(NUM_THREADS) : Executors.newSingleThreadExecutor();

        if (Thread.currentThread().isInterrupted()) {
            executor.shutdownNow();
            throw new InterruptedException("Got interrupted before submiting tasks");
        }

        for (int t = 0; t < tests.size(); t++) {
            Test test = tests.get(t);
            for (int m = 0; m < mutants.size(); m++) {
                Mutant mutant = mutants.get(m);
                if ((matrix[t][m] == null || recalculate) && filter.apply(test, mutant)) {
                    executionResults.add(executor.submit(new TestVsMutantCallable(test, mutant, classId)));
                }
            }
        }

        if (Thread.currentThread().isInterrupted()) {
            executor.shutdownNow();
            throw new InterruptedException("Got interrupted after submiting tasks");
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

        logger.info("Computation of killmap finished after " + Duration.between(startTime, Instant.now()).getSeconds() + " seconds");
    }

    /**
     * Returns the killmap for the given finished game.
     * This operation is blocking and may take a long time,
     * {@link KillmapDAO#hasKillMap(int)} can be used to check if a game's killmap has already beeen computed before.
     *
     * @param game The finished game to get the killmap for.
     * @param recalculate Recalculate the whole killmap, even if it was already calculated before.
     * @throws InterruptedException If the computation is interrupted.
     * @throws ExecutionException If an error occured during an execution.
     */
    public static KillMap forGame(AbstractGame game, boolean recalculate) throws InterruptedException, ExecutionException {
        if (game.getState() != GameState.FINISHED) {
            throw new IllegalArgumentException("Game must be finished.");

        } else if (!recalculate && KillmapDAO.hasKillMap(game.getId())) {
            List<Test> tests = game.getTests();
            List<Mutant> mutants = game.getMutants();
            List<KillMapEntry> entries = KillmapDAO.getKillMapEntriesForGame(game.getId());
            return new KillMap(tests, mutants, game.getClassId(), entries, NO_FILTER);

        } else {
            /* Synchronized, so only one killmap can be computed at a time. */
            synchronized (KillMap.class) {
                /* If killmap was calculated in the mean time, just return the already computed killmap. */
                if (recalculate || !KillmapDAO.hasKillMap(game.getId())) {
                    KillmapDAO.setHasKillMap(game.getId(), false);

                    List<Test> tests = game.getTests();
                    List<Mutant> mutants = game.getMutants();
                    List<KillMapEntry> entries = KillmapDAO.getKillMapEntriesForGame(game.getId());

                    logger.info(String.format("Computing killmap for game %d: %d tests, %d mutants, %d entries provided",
                            game.getId(), tests.size(), mutants.size(), entries.size()));

                    KillMap killmap = new KillMap(tests, mutants, game.getClassId(), entries, NO_FILTER);
                    killmap.compute(recalculate, NO_FILTER);

                    KillmapDAO.setHasKillMap(game.getId(), true);
                    return killmap;

                } else {
                    List<Test> tests = game.getTests();
                    List<Mutant> mutants = game.getMutants();
                    List<KillMapEntry> entries = KillmapDAO.getKillMapEntriesForGame(game.getId());
                    return new KillMap(tests, mutants, game.getClassId(), entries, NO_FILTER);
                }
            }
        }
    }

    /**
     * Returns the killmap for the given class.
     * This operation is blocking and may take a long time,
     *
     * @param classId The class to get the killmap for.
     * @param recalculate Recalculate the whole killmap, even if it was already calculated before.
     * @throws InterruptedException If the computation is interrupted.
     * @throws ExecutionException If an error occured during an execution.
     */
    public static KillMap forClass(int classId, boolean recalculate) throws InterruptedException, ExecutionException {
        /* Synchronized, so only one killmap can be computed at a time. */
        synchronized (KillMap.class) {
            List<Test> tests = TestDAO.getValidTestsForClass(classId);
            List<Mutant> mutants = MutantDAO.getValidMutantsForClass(classId);
            List<KillMapEntry> entries = KillmapDAO.getKillMapEntriesForClass(classId);

            logger.info(String.format("Computing killmap for class %d: %d tests, %d mutants, %d entries provided",
                    classId, tests.size(), mutants.size(), entries.size()));

            KillMap killmap = new KillMap(tests, mutants, classId, entries, NO_FILTER);
            killmap.compute(recalculate, NO_FILTER);

            return killmap;
        }
    }

    /**
     * Returns the killmap for the given test and mutants.
     * The tests and mutants must belong to the same class (with the same class id).
     * This operation is blocking and may take a long time,
     *
     * @param tests The tests to get the killmap for. The list must not include a mutant twice.
     * @param mutants The mutants to get the killmap for. The list must not include a test twice.
     * @param classId The class id of the class the tests and mutants belong to.
     * @param entries Already computed entries for the killmap.
     * @param recalculate Recalculate the whole killmap, even if it was already calculated before.
     * @param filter A filter, which decides what test-mutant combinations should be computed.
     * @throws InterruptedException If the computation is interrupted.
     * @throws ExecutionException If an error occured during an execution.
     */
    public static KillMap forCustom(List<Test> tests, List<Mutant> mutants, int classId, List<KillMapEntry> entries,
                                    boolean recalculate, BiFunction<Test, Mutant, Boolean> filter)
                                    throws InterruptedException, ExecutionException {
        /* Synchronized, so only one killmap can be computed at a time. */
        synchronized (KillMap.class) {
            logger.info(String.format("Computing killmap: %d tests, %d mutants, %d entries provided",
                    tests.size(), mutants.size(), entries.size()));

            KillMap killmap = new KillMap(tests, mutants, classId, entries, filter);
            killmap.compute(recalculate, filter);
            return killmap;
        }
    }

    /**
     * Returns the tests of the game the killmap is for.
     * @return The tests of the game the killmap is for.
     */
    public List<Test> getTests() {
        return new ArrayList<>(tests);
    }

    /**
     * Returns the mutants of the game the killmap is for.
     * @return The mutants of the game the killmap is for.
     */
    public List<Mutant> getMutants() {
        return new ArrayList<>(mutants);
    }

    /**
     * Returns a list of results of "test vs. mutant" executions.
     * @return A list of results of "test vs. mutant" executions.
     */
    public List<KillMapEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    /**
     * Returns a matrix that maps tests and mutants to their execution result.
     * The first dimension describes the tests, the second dimension describes the mutants.
     * {@link KillMap#indexOf(Test)} / {@link KillMap#indexOf(Mutant)} can be used to get the index of a test / mutant.
     * @return A matrix that maps tests and mutants to their execution result.
     */
    public KillMapEntry[][] getMatrix() {
        return (KillMapEntry[][]) ArrayUtils.clone(matrix);
    }

    /**
     * Returns all "test vs. mutant" execution results for the given test.
     * @param test The given test.
     * @return All "test vs. mutant" execution results for the given test.
     */
    public KillMapEntry[] getEntriesForTest (Test test) {
        return (KillMapEntry[]) ArrayUtils.clone(matrix[indexOf(test)]);
    }

    /**
     * Returns all "test vs. mutant" execution results for the given mutant.
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
     * @param test The given test.
     * @param mutant The given mutant.
     * @return The "test vs. mutant" execution result for the given test and mutant.
     */
    public KillMapEntry getEntry(Test test, Mutant mutant) {
        return matrix[indexOf(test)][indexOf(mutant)];
    }

    /**
     * Returns the index of the given test in the matrix, or null if the mutant is not part of the matrix.
     * @param test The given test.
     * @return The index of the given test in the matrix, or null if the mutant is not part of the matrix.
     */
    public Integer indexOf(Test test) {
        if (test == null) return null;
        return indexOfTest.get(test);
    }

    /**
     * Returns the index of the given mutant in the matrix, or null if the mutant is not part of the matrix.
     * @param mutant The given mutant.
     * @return The index of the given mutant in the matrix, or null if the mutant is not part of the matrix.
     */
    public Integer indexOf(Mutant mutant) {
        if (mutant == null) return null;
        return indexOfMutant.get(mutant);
    }

    /**
     * Executes a test against a mutant, inserts the result into the DB, and returns the result.
     */
    private static class TestVsMutantCallable implements Callable<KillMapEntry> {
        private Test test;
        private Mutant mutant;
        private int classId;

        public TestVsMutantCallable(Test test, Mutant mutant, int classId) {
            this.test = test;
            this.mutant = mutant;
            this.classId = classId;
        }

        @Override
        public KillMapEntry call() {
            KillMapEntry entry = null;

            if (USE_COVERAGE && !test.isMutantCovered(mutant)) {
                entry = new KillMapEntry(test, mutant, KillMapEntry.Status.NO_COVERAGE);

            } else {
                TargetExecution executedTarget = AntRunner.testMutant(mutant, test);
                KillMapEntry.Status status;

                switch (executedTarget.status) {
                    case "FAIL":    status = KILL;      break;
                    case "SUCCESS": status = NO_KILL;   break;
                    case "ERROR":   status = ERROR;     break;
                    default:        status = UNKNOWN;   break;
                }

                entry = new KillMapEntry(test, mutant, status);
            }

            if (!KillmapDAO.insertKillMapEntry(entry, classId)) {
                logger.error("An error occured while inserting killmap entry into the DB: " + entry);
            }

            return entry;
        }
    }

    /**
     * Represents a result of executing a test against a mutant.
     */
    public static class KillMapEntry {
        public static enum Status {
            /** Test kills mutant. */
            KILL,
            /** Test covers mutant but doesn't kill it. */
            NO_KILL,
            /** Test doesn't cover mutant. Only used if "mutant.coverage" is enabled. */
            NO_COVERAGE,
            /** An error occured during execution. If no errors occured elsewhere, then this means,
             *  that the test execution resulted in an exception (and the mutant was killed). */
            ERROR,
            /** Status is unknown. */
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
