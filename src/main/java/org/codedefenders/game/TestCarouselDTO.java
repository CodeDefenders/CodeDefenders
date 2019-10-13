package org.codedefenders.game;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.gson.annotations.Expose;
import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.model.User;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Computes and saves data for the test carousel.
 */
public class TestCarouselDTO {

    /**
     * The categories of the test carousel, in sorted order.
     * One category containing all tests and one category for each method of the class,
     * containing the tests that cover the method.
     */
    @Expose private List<TestCarouselCategory> categories;

    /**
     * Maps test ids to tests.
     */
    @Expose private Map<Integer, TCTestDTO> tests;

    /**
     * Constructs the test carousel data.
     * @param cut The class the tests and mutants belong to.
     * @param testsList The tests.
     * @param mutantsList The mutants.
     */
    public TestCarouselDTO(GameClass cut, List<Test> testsList, List<Mutant> mutantsList) {
        tests = new HashMap<>();
        categories = new ArrayList<>();

        for (Test test : testsList) {
            tests.put(test.getId(), new TCTestDTO(test, mutantsList));
        }

        TestCarouselCategory allTests = new TestCarouselCategory("All Tests", "all");
        allTests.addTestIds(tests.keySet());

        List<TestCarouselCategory> methods = cut.getTestCarouselMethodDescriptions();
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
                    entry.getValue().addTestId(test.getId());
                }
            }
        }
    }

    /**
     * Returns the categories of the test carousel.
     * @return The categories of the test carousel.
     */
    public List<TestCarouselCategory> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    /**
     * Represents a category (accordion section) of the test carousel.
     */
    public static class TestCarouselCategory {
        @Expose private String description;
        private Integer startLine;
        private Integer endLine;
        @Expose private Set<Integer> testIds;
        @Expose private String id;

        public TestCarouselCategory(String description, String id) {
            this.description = description;
            this.testIds = new HashSet<>();
            this.id = id;
        }

        public TestCarouselCategory(String description, int startLine, int endLine, String id) {
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
     * Represents a test for the test carousel.
     */
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
