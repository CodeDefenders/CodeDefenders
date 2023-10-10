/*
 * Copyright (C) 2016-2023 Code Defenders contributors
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;

/**
 * Maps tests to their killed mutants in a finished game.
 *
 * <p>Killmaps are computed and saved to the DB on the first time they are requested. This may take a long time.
 * {@link KillmapDAO#getKillMapProgressForGame(int)} and can be used to check if a finished game's killmap has already
 * been computed.
 *
 * <p>Only one killmap can be computed at a time. Further request are queued via {@code synchronized}.
 * This is mostly to prevent multiple calculations of the same killmap at once, e.g. by accidentally refreshing a page.
 */
public class KillMap {

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
    KillMap(List<Test> tests, List<Mutant> mutants, int classId, List<KillMapEntry> entries) {
        this.tests = new ArrayList<>(tests);
        this.mutants = new ArrayList<>(mutants);
        this.classId = classId;
        this.indexOfTest = new TreeMap<>(Comparator.comparing(Test::getId).reversed());
        this.indexOfMutant = new TreeMap<>(Comparator.comparing(Mutant::getId).reversed());

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

    public int getClassId() {
        return classId;
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
     * The killmap types. Game or class killmap.
     */
    public enum KillMapType {
        CLASS, GAME, CLASSROOM
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

    /**
     * Represents a job for computing a killmap.
     */
    public static class KillMapJob {
        private final KillMapType type;
        private final Integer id;

        public KillMapJob(KillMapType type, Integer id) {
            this.type = type;
            this.id = id;
        }

        public KillMapType getType() {
            return type;
        }

        public Integer getId() {
            return id;
        }
    }
}
