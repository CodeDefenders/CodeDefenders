package org.codedefenders.execution;

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.game.AbstractGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;

import javax.naming.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

public class KillMap {

    //TODO needs a way to recalculate killmap if it's faulty

    private static final Logger logger = LoggerFactory.getLogger(KillMap.class);

    /* Settings */
    private static boolean useCoverage = true;
    private static boolean parallelize = true;

    /* Get settings if they are set, otherwise use defaults. */
    static {
        InitialContext initialContext;
        try {
            initialContext = new InitialContext();
            Context environmentContext = (Context) initialContext.lookup("java:/comp/env");
            Object useCoverageObj = environmentContext.lookup("mutant.coverage");
            Object parallelizeObj = environmentContext.lookup("parallelize");
            useCoverage = (useCoverageObj == null) ? useCoverage : "enabled".equalsIgnoreCase((String) useCoverageObj);
            parallelize = (parallelizeObj == null) ? parallelize : "enabled".equalsIgnoreCase((String) parallelizeObj);
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /* Game data. */
    AbstractGame game;
    List<Test> tests;
    List<Mutant> mutants;

    /* KillMap data. */
    private List<KillMapEntry> entries;

    private KillMap(List<KillMapEntry> entries, AbstractGame game) {
        this.entries = entries;
        this.game = game;
        this.tests = game.getTests();
        this.mutants = game.getMutants();
    }

    public static synchronized KillMap forGame(AbstractGame game) throws InterruptedException, ExecutionException {
        if (game.hasKillMap()) {
            List<KillMapEntry> entries = DatabaseAccess.getKillMapEntriesForGame(game.getId());
            return new KillMap(entries, game);

        } else {
            List<KillMapEntry> entries = new LinkedList<>();
            List<Test> tests = game.getTests();
            List<Mutant> mutants = game.getMutants();

            List<Future<KillMapEntry>> executionResults = new LinkedList<>();
            ExecutorService executor = parallelize ? Executors.newFixedThreadPool(8) : Executors.newSingleThreadExecutor();
            // TODO what number of threads

            for (Test test : tests) {
                for (Mutant mutant : mutants) {
                    if (useCoverage) {
                        if(test.isMutantCovered(mutant)) {
                            executionResults.add(executor.submit(new TestVsMutantCallable(test, mutant)));
                        } else {
                            entries.add(new KillMapEntry(test.getId(), mutant.getId(), KillMapEntry.Status.NO_COVERAGE));
                        }
                    } else {
                        executionResults.add(executor.submit(new TestVsMutantCallable(test, mutant)));
                    }
                }
            }

            executor.shutdown();

            for (Future<KillMapEntry> antExecution : executionResults) {
                entries.add(antExecution.get());
            }

            KillMap killMap = new KillMap(entries, game);
            try {
                DatabaseAccess.insertKillMap(killMap);
                return killMap;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }
    }


    public AbstractGame getGame() {
        return game;
    }

    public List<KillMapEntry> getMap() {
        return entries;
    }




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

            return new KillMapEntry(test.getId(), mutant.getId(), status);
        }
    }

    public static class KillMapEntry {
        public static enum Status {
            /** Test kills mutant. */
            KILL,
            /** Test covers mutant but doesn't kill it. */
            NO_KILL,
            /** Test doesn't cover mutant. */
            NO_COVERAGE,
            /** An error occured during execution. */
            ERROR,
            /** Status is unknown. */
            UNKNOWN
        }

        //TODO remove getters and setters again

        public int getTestId() {
            return testId;
        }

        public void setTestId(int testId) {
            this.testId = testId;
        }

        public int getMutantId() {
            return mutantId;
        }

        public void setMutantId(int mutantId) {
            this.mutantId = mutantId;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public int testId;
        public int mutantId;
        public Status status;

        public KillMapEntry(int testId, int mutantId, Status status) {
            this.testId = testId;
            this.mutantId = mutantId;
            this.status = status;
        }
    }
}
