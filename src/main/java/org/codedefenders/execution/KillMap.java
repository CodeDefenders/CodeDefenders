package org.codedefenders.execution;

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
 * This is mostly to prevent multiple calculations, e.g. by accidentally refreshing a page.
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

    private AbstractGame game;
    private List<KillMapEntry> entries;

    private KillMap(List<KillMapEntry> entries, AbstractGame game) {
        this.entries = entries;
        this.game = game;
    }

    /**
     * Returns the killmap for the given finished game.
     * If the killmap for the game was not computed before, it is computed by {@link #forGame(AbstractGame, boolean)}.
     * This may take a long time.
     * {@link DatabaseAccess#hasKillMap(int)} can be used to check if a game's killmap has already beeen computed before.
     * <p/>
     * Only one killmap can be computed at a time. Further request are queued via {@code synchronized}.
     * This is mostly to prevent multiple calculations, e.g. by accidentally refreshing a page.
     *
     * @param game The finished game to get the killmap for.
     * @param recalculate Recalculate the killmap, even if it was already calculated before.
     */
    public static KillMap forGame(AbstractGame game, boolean recalculate) throws InterruptedException, ExecutionException {
        if (game.getState() != GameState.FINISHED) {
            throw new IllegalArgumentException("Game must be finished.");

        } else if (!recalculate && DatabaseAccess.hasKillMap(game.getId())) {
            List<KillMapEntry> entries = DatabaseAccess.getKillMapEntriesForGame(game.getId());
            return new KillMap(entries, game);

        } else {
            synchronized (KillMap.class) {
                if (recalculate || !DatabaseAccess.hasKillMap(game.getId())) {
                    KillMap killMap = computeForGame(game);
                    if (!DatabaseAccess.insertKillMap(killMap)) {
                        logger.warn("Failed to insert killmap for game " + game.getId() + " into the DB.");
                    }
                    return killMap;
                } else {
                    List<KillMapEntry> entries = DatabaseAccess.getKillMapEntriesForGame(game.getId());
                    return new KillMap(entries, game);
                }
            }
        }
    }

    /**
     * Computes the killmap for a game by executing every test against the mutants it covers.
     */
    private static KillMap computeForGame(AbstractGame game) throws InterruptedException, ExecutionException {
        List<KillMapEntry> entries = new LinkedList<>();
        List<Test> tests = game.getTests();
        List<Mutant> mutants = game.getMutants();

        List<Future<KillMapEntry>> executionResults = new LinkedList<>();
        ExecutorService executor = PARALLELIZE ? Executors.newFixedThreadPool(8) : Executors.newSingleThreadExecutor();

        for (Test test : tests) {
            for (Mutant mutant : mutants) {
                if (USE_COVERAGE && !test.isMutantCovered(mutant)) {
                    entries.add(new KillMapEntry(test, mutant, KillMapEntry.Status.NO_COVERAGE));
                } else {
                    executionResults.add(executor.submit(new TestVsMutantCallable(test, mutant)));
                }
            }
        }

        executor.shutdown();

        for (Future<KillMapEntry> antExecution : executionResults) {
            entries.add(antExecution.get());
        }

        return new KillMap(entries, game);
    }

    /**
     * Returns the game the killmap is for.
     */
    public AbstractGame getGame() {
        return game;
    }

    /**
     * Returns the kill map as a list of execution results.
     */
    public List<KillMapEntry> getEntries() {
        return entries;
    }

    /**
     * Returns the kill map as a map from tests to their execution results.
     */
    public Map<Test, List<KillMapEntry>> getTestMap() {
        Map<Test, List<KillMapEntry>> map = new TreeMap<>(Test.orderByIdDescending());

        for (Test test : game.getTests()) {
            map.put(test, new LinkedList<>());
        }

        for (KillMapEntry entry : entries) {
            map.get(entry.test).add(entry);
        }

        return map;
    }

    /**
     * Returns the kill map as a map from mutants to their execution results.
     */
    public Map<Mutant, List<KillMapEntry>> getMutantMap() {
        Map<Mutant, List<KillMapEntry>> map = new TreeMap<>(Mutant.orderByIdDescending());

        for (Mutant mutant : game.getMutants()) {
            map.put(mutant, new LinkedList<>());
        }

        for (KillMapEntry entry : entries) {
            map.get(entry.mutant).add(entry);
        }

        return map;
    }

    /**
     * Executes a test against a mutant and returns the result.
     */
    private static class TestVsMutantCallable implements Callable<KillMapEntry> {
        private Test test;
        private Mutant mutant;

        private TestVsMutantCallable(Test test, Mutant mutant) {
            this.test = test;
            this.mutant = mutant;
        }

        @Override
        public KillMapEntry call() {
            TargetExecution executedTarget = AntRunner.testMutant(mutant, test);
            KillMapEntry.Status status;

            switch (executedTarget.status) {
                case "FAIL":    status = KillMapEntry.Status.KILL; break;
                case "SUCCESS": status = KillMapEntry.Status.NO_KILL; break;
                case "ERROR":   status = KillMapEntry.Status.ERROR; break;
                default:        status = KillMapEntry.Status.UNKNOWN; break;
            }

            return new KillMapEntry(test, mutant, status);
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
