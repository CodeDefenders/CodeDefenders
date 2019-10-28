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
package org.codedefenders.game;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.gson.annotations.Expose;
import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes and saves data for the test accordion.
 */
public class TestAccordionDTO {
    /**
     * The categories of the test accordion, in sorted order.
     * One category containing all tests and one category for each method of the class,
     * containing the tests that cover the method.
     */
    @Expose private List<TestAccordionCategory> categories;

    /**
     * Maps test ids to tests.
     */
    @Expose private Map<Integer, TATestDTO> tests;

    /**
     * Constructs the test accordion data.
     * @param cut The class the tests and mutants belong to.
     * @param testsList The tests.
     * @param mutantsList The mutants.
     */
    public TestAccordionDTO(GameClass cut, List<Test> testsList, List<Mutant> mutantsList) {
        tests = new HashMap<>();
        categories = new ArrayList<>();

        for (Test test : testsList) {
            tests.put(test.getId(), new TATestDTO(test, mutantsList));
        }

        TestAccordionCategory allTests = new TestAccordionCategory("All Tests", "all");
        allTests.addTestIds(tests.keySet());

        List<TestAccordionCategory> methods = cut.getTestAccordionMethodDescriptions();
        categories.add(allTests);
        categories.addAll(methods);

        if (methods.isEmpty()) {
            return;
        }

        /* Map ranges of methods to their test accordion infos. */
        @SuppressWarnings("UnstableApiUsage")
        RangeMap<Integer, TestAccordionCategory> methodRanges = TreeRangeMap.create();
        for (TestAccordionCategory method : methods) {
            methodRanges.put(Range.closed(method.startLine, method.endLine), method);
        }

        Range<Integer> beforeFirst = Range.closedOpen(0, methodRanges.span().lowerEndpoint());

        /* For every test, go through all covered lines and find the methods that are covered by it. */
        for (Test test : testsList) {

            /* Save the last range a line number fell into to avoid checking a line number in the same method twice. */
            Range<Integer> lastRange = null;

            for (Integer line : test.getLineCoverage().getLinesCovered()) {

                /* Skip if line falls into a method that was already considered. */
                if (lastRange != null && lastRange.contains(line)) {
                    continue;
                }

                Entry<Range<Integer>, TestAccordionCategory> entry = methodRanges.getEntry(line);

                /* Line does not belong to any method. */
                if (entry == null) {
                    lastRange = beforeFirst;

                /* Line belongs to a method. */
                } else {
                    lastRange = entry.getKey();
                    entry.getValue().addTestId(test.getId());
                }
            }
        }
    }

    /**
     * Returns the categories of the test accordion.
     * @return The categories of the test accordion.
     */
    public List<TestAccordionCategory> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    /**
     * Represents a category (accordion section) of the test accordion.
     */
    public static class TestAccordionCategory {
        @Expose private String description;
        private Integer startLine;
        private Integer endLine;
        @Expose private Set<Integer> testIds;
        @Expose private String id;

        public TestAccordionCategory(String description, String id) {
            this.description = description;
            this.testIds = new HashSet<>();
            this.id = id;
        }

        public TestAccordionCategory(String description, int startLine, int endLine, String id) {
            this(description, id);
            this.startLine = startLine;
            this.endLine = endLine;
        }

        public void addTestId(int testId) {
            this.testIds.add(testId);
        }

        public void addTestIds(Collection<Integer> testIds) {
            this.testIds.addAll(testIds);
        }

        public String getDescription() {
            return description;
        }

        public Set<Integer> getTestIds() {
            return Collections.unmodifiableSet(testIds);
        }

        public String getId() {
            return id;
        }
    }

    /**
     * Represents a test for the test accordion.
     */
    public static class TATestDTO {
        @Expose private int id;
        @Expose private String creatorName;
        @Expose private List<Integer> coveredMutantIds;
        @Expose private List<Integer> killedMutantIds;
        @Expose private int points;
        @Expose private List<String> smells;

        public TATestDTO(Test test, List<Mutant> mutants) {
            User creator = UserDAO.getUserForPlayer(test.getPlayerId());
            this.id = test.getId();
            this.creatorName = creator.getUsername();
            this.coveredMutantIds = test.getCoveredMutants(mutants).stream()
                    .map(Mutant::getId)
                    .collect(Collectors.toList());
            this.killedMutantIds = test.getKilledMutants().stream()
                    .map(Mutant::getId)
                    .collect(Collectors.toList());
            this.points = test.getScore();
            this.smells = (new TestSmellsDAO()).getDetectedTestSmellsForTest(test);
        }
    }
}
