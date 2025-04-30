/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
package org.codedefenders.game;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.codedefenders.util.Constants;

import com.google.gson.annotations.Expose;

/**
 * Saves data for the game highlighting in order to convert it to JSON.
 */
public class GameHighlightingDTO {

    /**
     * Maps line numbers to the mutant ids of mutants that modify the line.
     */
    @Expose public Map<Integer, List<Integer>> mutantIdsPerLine;

    /**
     * Maps line numbers to the test ids of tests that cover the line.
     */
    @Expose public Map<Integer, List<Integer>> testIdsPerLine;

    /**
     * Maps test ids to mutants.
     */
    @Expose public Map<Integer, GHMutantDTO> mutants;

    /**
     * Maps test ids to tests.
     */
    @Expose public Map<Integer, GHTestDTO> tests;

    /**
     * Maps test ids to the alternative tests.
     */
    @Expose public Map<Integer, GHTestDTO> alternativeTests;

    /**
     * Maps line numbers to the test ids of the alternative tests that cover the line.
     */
    @Expose public Map<Integer, List<Integer>> alternativeTestIdsPerLine;

    /**
     * Constructs the game highlighting data from the list of mutants and the list of tests in the game.
     * The object is ready to be converted to JSON after the constructor has been called.
     * @param mutants The mutants in the game.
     * @param tests The tests in the game.
     */
    public GameHighlightingDTO(List<Mutant> mutants, List<Test> tests, Integer userId) {
        this.mutantIdsPerLine = new TreeMap<>();
        this.testIdsPerLine = new TreeMap<>();
        this.mutants = new TreeMap<>();
        this.tests = new TreeMap<>();

        /* Construct the test maps. */
        for (Test test : tests) {
            this.tests.put(test.getId(), new GHTestDTO(test));
            List<Integer> linesCovered = test
                    .getLineCoverage()
                    .getLinesCovered()
                    .stream()
                    .distinct()
                    .collect(Collectors.toList());
            for (Integer line : linesCovered) {
                List<Integer> list = testIdsPerLine.computeIfAbsent(line, key -> new LinkedList<>());
                list.add(test.getId());
            }
        }

        /* Construct the mutant maps. */
        for (Mutant mutant : mutants) {
            this.mutants.put(mutant.getId(), new GHMutantDTO(mutant, userId));
            List<Integer> lines = mutant.getLines();
            for (Integer line : lines) {
                List<Integer> list = mutantIdsPerLine.computeIfAbsent(line, key -> new LinkedList<>());
                list.add(mutant.getId());
            }
        }
    }

    public void setAlternativeTestData(List<Test> tests) {
        this.alternativeTestIdsPerLine = new TreeMap<>();
        this.alternativeTests = new TreeMap<>();

        /* Construct the test maps. */
        for (Test test : tests) {
            this.alternativeTests.put(test.getId(), new GHTestDTO(test));
            List<Integer> linesCovered = test
                    .getLineCoverage()
                    .getLinesCovered()
                    .stream()
                    .distinct()
                    .collect(Collectors.toList());
            for (Integer line : linesCovered) {
                List<Integer> list = alternativeTestIdsPerLine.computeIfAbsent(line, key -> new LinkedList<>());
                list.add(test.getId());
            }
        }
    }

    /**
     * Represents a mutant for the game highlighting.
     */
    public static class GHMutantDTO {
        @Expose public int id;
        @Expose public int score;
        @Expose public String lines;
        @Expose public String creatorName;
        @Expose public Mutant.State status;
        @Expose public boolean canClaim;

        public GHMutantDTO(Mutant mutant, Integer userId) {
            this.id = mutant.getId();
            this.score = mutant.getScore();
            this.creatorName = mutant.getCreatorName();
            this.lines = mutant.getSummaryString();
            this.status = mutant.getState();
            this.canClaim = mutant.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO)
                    && mutant.isAlive()
                    && mutant.getCreatorId() != Constants.DUMMY_ATTACKER_USER_ID
                    && (userId == null || mutant.getCreatorId() != userId)
                    && mutant.getLines().size() >= 1;
        }
    }

    /**
     * Represents a test for the game highlighting.
     */
    public static class GHTestDTO {
        @Expose public int id;

        public GHTestDTO(Test test) {
            this.id = test.getId();
        }
    }
}
