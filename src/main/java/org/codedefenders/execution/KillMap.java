package org.codedefenders.execution;

import org.apache.commons.lang.ArrayUtils;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;

import javax.naming.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Maps tests to their killed mutants in a finished game.
 * Killmaps are computed and saved to the DB on the first time they are requested. This may take a long time.
 * {@link DatabaseAccess#hasKillMap(int)}  can be used to check if a game's killmap has already beeen computed before.
 * <p/>
 * Only one killmap can be computed at a time. Further request are queued via {@code synchronized}.
 * This is mostly to prevent multiple calculations of the same killmap at once, e.g. by accidentally refreshing a page.
 */
public class KillMap {

    private static final Logger logger = LoggerFactory.getLogger(KillMap.class);

    private static boolean USE_COVERAGE = true;
    private static boolean PARALLELIZE = true;
    private final static int NUM_THREADS = 8;
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
            e.printStackTrace();
        }
    }

    /** The game the killmap is for. */
    private AbstractGame game;
    /** The tests of the game. */
    private List<Test> tests;
    /** The mutants of the game. */
    private List<Mutant> mutants;

    /** The killmap data, as a list of "test vs. mutant" execution results. */
    private List<KillMapEntry> entries;
    /** The killmap data, as matrix between tests and mutants. */
    private KillMapEntry[][] matrix;
    /** Maps each test to it's index in {@link KillMap#tests}. */
    private TreeMap<Test, Integer> indexOfTest;
    /** Maps each mutant to it's index in {@link KillMap#mutants}. */
    private TreeMap<Mutant, Integer> indexOfMutant;

    /**
     * Creates a new killmap, with the given entries, for the given finished game.
     * The constructor does not compute the killmap, or fetch it from the database.
     * Use {@link KillMap#forGame(AbstractGame, boolean)} to compute a killmap, or fetch it from the database.
     *
     * @param entries The entries of the killmap.
     * @param game The game the killmap is for.
     */
    private KillMap(List<KillMapEntry> entries, AbstractGame game) {
        this.game = game;
        this.tests = new ArrayList<>(game.getTests());
        this.mutants = new ArrayList<>(game.getMutants());

        this.entries = entries;
        this.indexOfTest = new TreeMap<>(Test.orderByIdDescending());
        this.indexOfMutant = new TreeMap<>(Mutant.orderByIdDescending());
        this.matrix = new KillMapEntry[tests.size()][mutants.size()];

        /* Fill the matrix with existing entries. */
        for (int i = 0; i < this.tests.size(); i++) {
            this.indexOfTest.put(tests.get(i), i);
        }
        for (int i = 0; i < this.mutants.size(); i++) {
            this.indexOfMutant.put(mutants.get(i), i);
        }
        for (KillMapEntry entry : entries) {
            matrix[indexOf(entry.test)][indexOf(entry.mutant)] = entry;
        }
    }

    /**
     * Returns the killmap for the given finished game.
     * If the killmap for the game was not computed before, it is computed by {@link #forGame(AbstractGame, boolean)}.
     * This may take a long time.
     * {@link DatabaseAccess#hasKillMap(int)} can be used to check if a game's killmap has already beeen computed before.
     * <p/>
     * Already calculated results (which are saved in the database) are not calculated again, unless {@code recalculate}
     * is {@code true}.
     * <p/>
     * Only one killmap can be computed at a time. Further request are queued via {@code synchronized}.
     * This is mostly to prevent multiple calculations, e.g. by accidentally refreshing a page.
     *
     * @param game The finished game to get the killmap for.
     * @param recalculate Recalculate the whole killmap, even if it was already calculated before.
     * @throws InterruptedException If the computation is interrupted.
     * @throws ExecutionException If an error occured during an execution.
     */
    public static KillMap forGame(AbstractGame game, boolean recalculate) throws InterruptedException, ExecutionException {
        if (game.getState() != GameState.FINISHED) {
            throw new IllegalArgumentException("Game must be finished.");

        } else if (!recalculate && DatabaseAccess.hasKillMap(game.getId())) {
            List<KillMapEntry> entries = DatabaseAccess.getKillMapEntriesForGame(game.getId());
            return new KillMap(entries, game);

        } else {
            /* Synchronized, so only one killmap can be computed at a time. */
            synchronized (KillMap.class) {
                if (recalculate || !DatabaseAccess.hasKillMap(game.getId())) {
                    if (recalculate) {
                        DatabaseAccess.setHasKillMap(game.getId(), false);
                    }
                    List<KillMapEntry> entries = DatabaseAccess.getKillMapEntriesForGame(game.getId());
                    KillMap killmap = new KillMap(entries, game);
                    killmap.compute(recalculate);
                    return killmap;
                } else {
                    List<KillMapEntry> entries = DatabaseAccess.getKillMapEntriesForGame(game.getId());
                    return new KillMap(entries, game);
                }
            }
        }
    }

    /**
     * Gets called by {@link KillMap#forGame(AbstractGame, boolean)} to compute a killmap.
     * @see KillMap#forGame(AbstractGame, boolean)
     */
    private void compute(boolean recalculate) throws InterruptedException, ExecutionException {
        List<Future<KillMapEntry>> executionResults = new LinkedList<>();
        ExecutorService executor = PARALLELIZE ? Executors.newFixedThreadPool(8) : Executors.newSingleThreadExecutor();

        for (int t = 0; t < tests.size(); t++) {
            for (int m = 0; m < mutants.size(); m++) {
                if (matrix[t][m] == null || recalculate) {
                    executionResults.add(executor.submit(new TestVsMutantCallable(tests.get(t), mutants.get(m))));
                }
            }
        }

        executor.shutdown();

        for (Future<KillMapEntry> result : executionResults) {
            KillMapEntry entry = result.get();
            entries.add(entry);
            matrix[indexOf(entry.test)][indexOf(entry.mutant)] = entry;
        }

        DatabaseAccess.setHasKillMap(game.getId(), true);
    }

    /**
     * Returns the game the killmap is for.
     * @return The game the killmap is for.
     */
    public AbstractGame getGame() {
        return game;
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
     * Returns the index of the given test in the matrix.
     * @param test The given test.
     * @return The index of the given test in the matrix.
     */
    public int indexOf(Test test) {
        return indexOfTest.get(test);
    }

    /**
     * Returns the index of the given mutant in the matrix.
     * @param mutant The given mutant.
     * @return The index of the given mutant in the matrix.
     */
    public int indexOf(Mutant mutant) {
        return indexOfMutant.get(mutant);
    }

    /**
     * Executes a test against a mutant, inserts the result into the DB, and returns the result.
     */
    private class TestVsMutantCallable implements Callable<KillMapEntry> {
        private Test test;
        private Mutant mutant;

        private TestVsMutantCallable(Test test, Mutant mutant) {
            this.test = test;
            this.mutant = mutant;
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
                    case "FAIL":    status = KillMapEntry.Status.KILL; break;
                    case "SUCCESS": status = KillMapEntry.Status.NO_KILL; break;
                    case "ERROR":   status = KillMapEntry.Status.ERROR; break;
                    default:        status = KillMapEntry.Status.UNKNOWN; break;
                }

                entry = new KillMapEntry(test, mutant, status);
            }

            if (!DatabaseAccess.insertKillMapEntry(entry)) {
                logger.error("KillMap entry could not be inserted into DB: " + entry);
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
            /** An error occured during execution. */
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
