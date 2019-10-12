package org.codedefenders.game;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.gson.annotations.Expose;
import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class TestCarousel {
    @Expose private List<TestCarouselCategory> categories;
    @Expose private Map<Integer, TCTestDTO> tests;

    public TestCarousel(GameClass cut, List<Test> testsList, List<Mutant> mutantsList) {
        tests = new HashMap<>();
        for (Test test : testsList) {
            tests.put(test.getId(), new TCTestDTO(test, mutantsList));
        }

        TestCarouselCategory allTests = new TestCarouselCategory("All Tests", -1, -1);
        allTests.testIds.addAll(tests.keySet());
        List<TestCarouselCategory> methods = cut.getTestCarouselMethodDescriptions();
        categories = new ArrayList<>();
        categories.add(allTests);
        categories.addAll(methods);

        /* Map ranges of methods to their test carousel infos. */
        @SuppressWarnings("UnstableApiUsage")
        RangeMap<Integer, TestCarouselCategory> methodRanges = TreeRangeMap.create();
        for (TestCarouselCategory method : methods) {
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

                Entry<Range<Integer>, TestCarouselCategory> entry = methodRanges.getEntry(line);

                /* Line does not belong to any method. */
                if (entry == null) {
                    lastRange = beforeFirst;

                /* Line belongs to a method. */
                } else {
                    lastRange = entry.getKey();
                    entry.getValue().testIds.add(test.getId());
                }
            }
        }
    }

    public List<TestCarouselCategory> getInfos() {
        return Collections.unmodifiableList(categories);
    }

    public static class TestCarouselCategory {
        @Expose private String description;
        private int startLine;
        private int endLine;
        @Expose private Set<Integer> testIds;

        public TestCarouselCategory(String description, int startLine, int endLine) {
            this.description = description;
            this.startLine = startLine;
            this.endLine = endLine;
            this.testIds = new HashSet<>();
        }

        public String getDescription() {
            return description;
        }

        public Set<Integer> getTestIds() {
            return Collections.unmodifiableSet(testIds);
        }
    }

    public static class TCTestDTO {
        @Expose private int id;
        @Expose private String creatorName;
        @Expose private List<Integer> coveredMutantIds;
        @Expose private List<Integer> killedMutantIds;
        @Expose private int points;
        @Expose private List<String> smells;

        public TCTestDTO(Test test, List<Mutant> mutants) {
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
