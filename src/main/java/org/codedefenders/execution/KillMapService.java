/*
 * Copyright (C) 2023 Code Defenders contributors
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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.execution.KillMap.KillMapEntry;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.util.concurrent.ExecutorServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import static org.codedefenders.execution.KillMap.KillMapEntry.Status.ERROR;
import static org.codedefenders.execution.KillMap.KillMapEntry.Status.KILL;
import static org.codedefenders.execution.KillMap.KillMapEntry.Status.NO_KILL;
import static org.codedefenders.execution.KillMap.KillMapEntry.Status.UNKNOWN;

@ApplicationScoped
public class KillMapService {
    private static final Logger logger = LoggerFactory.getLogger(KillMapService.class);

    private final BackendExecutorService backendExecutorService;
    private final Configuration config;

    /**
     * @implNote executor is shutdown by {@link ExecutorServiceProvider#shutdown()}
     */
    private final ExecutorService executor;

    @Inject
    public KillMapService(BackendExecutorService backendExecutorService, ExecutorServiceProvider executorServiceProvider,
            @SuppressWarnings("CdiInjectionPointsInspection") Configuration config) {
        this.backendExecutorService = backendExecutorService;
        this.config = config;

        // TODO(Alex): It might be better to allow the threads of this executor to time-out, so we do not keep all the
        //  threads around forever.
        executor = config.isParallelize()
                ? executorServiceProvider.createExecutorService("killMap-executer-parallel", config.getNumberOfKillmapThreads())
                : executorServiceProvider.createExecutorService("killMap-executer-single", 1);
    }

    /**
     * Returns the killmap for the given finished game.
     * This operation is blocking and may take a long time.
     *
     * @param game The finished game to get the killmap for.
     * @throws InterruptedException If the computation is interrupted.
     * @throws ExecutionException   If an error occurred during an execution.
     */
    public KillMap forGame(AbstractGame game) throws InterruptedException, ExecutionException {
        List<Test> tests = game.getTests();
        List<Mutant> mutants = game.getMutants();
        List<KillMapEntry> entries = KillmapDAO.getKillMapEntriesForGame(game.getId());
        KillMap killmap = new KillMap(tests, mutants, game.getClassId(), entries);

        if (tests.size() * mutants.size() != entries.size()) {
            /* Synchronized, so only one killmap can is computed at a time. */
            synchronized (KillMap.class) {
                logger.info(String.format("Computing killmap for %s game %d: %d tests, %d mutants, %d entries provided",
                        game.getMode(), game.getId(), tests.size(), mutants.size(), entries.size()));

                compute(killmap, executor);
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
    public KillMap forClass(int classId) throws InterruptedException, ExecutionException {
        List<Test> tests = TestDAO.getValidTestsForClass(classId);
        List<Mutant> mutants = MutantDAO.getValidMutantsForClass(classId);
        List<KillMapEntry> entries = KillmapDAO.getKillMapEntriesForClass(classId);
        KillMap killmap = new KillMap(tests, mutants, classId, entries);

        if (tests.size() * mutants.size() != entries.size()) {
            /* Synchronized, so only one killmap is computed at a time. */
            synchronized (KillMap.class) {
                logger.info(String.format("Computing killmap for class %d: %d tests, %d mutants, %d entries provided",
                        classId, tests.size(), mutants.size(), entries.size()));

                compute(killmap, executor);
            }
        } else {
            logger.info("Killmap for class " + classId + " already computed");
        }

        return killmap;
    }

    /**
     * Computes killmaps for the mutants/tests of a given classroom.
     * This operation is blocking and may take a long time.
     *
     * @param classroomId The classroom to compute killmaps for.
     * @throws InterruptedException If the computation is interrupted.
     * @throws ExecutionException   If an error occurred during an execution.
     */
    public void forClassroom(int classroomId) throws InterruptedException, ExecutionException {
        Multimap<Integer, Test> testsByClass = TestDAO.getValidTestsForClassroom(classroomId);
        Multimap<Integer, Mutant> mutantsByClass = MutantDAO.getValidMutantsForClassroom(classroomId);
        Multimap<Integer, KillMapEntry> entriesByClass = ArrayListMultimap.create();
        for (KillMapEntry entry : KillmapDAO.getKillMapEntriesForClassroom(classroomId)) {
            entriesByClass.put(entry.mutant.getClassId(), entry);
        }

        for (int classId : testsByClass.keySet()) { // sufficient to iterate over test
            Collection<Test> tests = testsByClass.get(classId);
            Collection<Mutant> mutants = mutantsByClass.get(classId);
            Collection<KillMapEntry> entries = entriesByClass.get(classId);
            forCustom(new ArrayList<>(tests), new ArrayList<>(mutants), classId, new ArrayList<>(entries));
        }
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
    public KillMap forCustom(List<Test> tests, List<Mutant> mutants, int classId, List<KillMapEntry> entries)
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

                    compute(killmap, executor);
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
    public KillMap forMutantValidation(List<Test> tests, Mutant mutant, int classId) {
        List<KillMapEntry> entries = new ArrayList<>();
        KillMap killmap = new KillMap(tests, Arrays.asList(mutant), classId, entries);
        logger.debug("Validating mutant {} using custom killmap (partial results are stored in the db) using: {} tests",
                mutant, tests.size());

        // TODO this creates a surge in the load as the number of executor services might grow
        // by refactoring this into a CDI we should be able to easily control the situation and queue jobs.
        /* TODO(Alex): Do we get any Threading/Concurrency issues if we use a shared ExecutorService here?!
        ExecutorService executor = config.isParallelize()
                ? Executors.newFixedThreadPool(config.getNumberOfKillmapThreads())
                : Executors.newSingleThreadExecutor();
         */

        try {
            compute(killmap, executor);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Exception while validating mutant {} using custom killmap", mutant.getId(), e);
            return null;
        }

        return killmap;
    }

    /**
     * Computes the missing entries of the killmap.
     *
     * @throws InterruptedException If the computation is interrupted.
     * @throws ExecutionException   If an error occurred during an execution.
     */
    void compute(KillMap killMap, ExecutorService executor) throws InterruptedException, ExecutionException {
        Instant startTime = Instant.now();

        List<Future<KillMapEntry>> executionResults = new LinkedList<>();

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Got interrupted before submitting tasks");
        }

        for (int t = 0; t < killMap.getTests().size(); t++) {
            Test test = killMap.getTests().get(t);
            for (int m = 0; m < killMap.getMutants().size(); m++) {
                Mutant mutant = killMap.getMutants().get(m);
                if ((killMap.getMatrix()[t][m] == null)) {
                    executionResults.add(executor.submit(() -> {
                        KillMapEntry entry;

                        if (config.isMutantCoverage() && !test.isMutantCovered(mutant)) {
                            entry = new KillMapEntry(test, mutant, KillMapEntry.Status.NO_COVERAGE);

                        } else {
                            TargetExecution executedTarget = backendExecutorService.testMutant(mutant, test);
                            KillMapEntry.Status status;

                            switch (executedTarget.status) {
                                case FAIL:
                                    status = KILL;
                                    break;
                                case SUCCESS:
                                    status = NO_KILL;
                                    break;
                                case ERROR:
                                    status = ERROR;
                                    break;
                                default:
                                    status = UNKNOWN;
                                    break;
                            }

                            entry = new KillMapEntry(test, mutant, status);
                        }

                        // TODO(Alex): Move out of Lambda/Method?!
                        //  There also exists: KillmapDAO.insertManyKillMapEntries(List<KillMapEntry>, int)
                        if (!KillmapDAO.insertKillMapEntry(entry, killMap.getClassId())) {
                            logger.error("An error occurred while inserting killmap entry into the DB: " + entry);
                        }

                        return entry;
                    }));
                }
            }
        }

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Got interrupted after submitting tasks");
        }

        for (Future<KillMapEntry> result : executionResults) {
            try {
                KillMapEntry entry = result.get();
                killMap.getEntries().add(entry);
                killMap.getMatrix()[killMap.indexOf(entry.test)][killMap.indexOf(entry.mutant)] = entry;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("Got interrupted while waiting for results");
            }
        }

        logger.info("Computation of killmap finished after " + Duration.between(startTime, Instant.now()).getSeconds()
                + " seconds");
    }
}
