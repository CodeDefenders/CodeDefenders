package org.codedefenders.game;

import com.google.common.collect.*;
import com.lexicalscope.jewelcli.internal.fluentreflection.$FluentAnnotated;


import java.util.*;
import java.util.Map.Entry;

public class TestCarousel {
    private List<TestCarouselInfo> testCarouselInfos;

    public TestCarousel(GameClass cut, List<Test> tests) {
        List<TestCarouselInfo> actualMethodInfos = cut.getTestCarouselInfos();

        /* Map ranges of methods to their test carousel infos. */
        @SuppressWarnings("UnstableApiUsage")
        RangeMap<Integer, TestCarouselInfo> methodRanges = TreeRangeMap.create();
        for (TestCarouselInfo testCarouselInfo : actualMethodInfos) {
            methodRanges.put(Range.closed(testCarouselInfo.startLine, testCarouselInfo.endLine), testCarouselInfo);
        }

        Range<Integer> beforeFirst = Range.closedOpen(0, methodRanges.span().lowerEndpoint());

        /* Represents code outside of methods. */
        TestCarouselInfo outsideMethods = new TestCarouselInfo(
                "Outside Methods",
                beforeFirst.lowerEndpoint(),
                beforeFirst.upperEndpoint());

        testCarouselInfos = new ArrayList<>();
        testCarouselInfos.add(outsideMethods);
        testCarouselInfos.addAll(cut.getTestCarouselInfos());

        /* For every test, go through all covered lines and find the methods that are covered by it. */
        for (Test test : tests) {

            /* Save the last range a line number fell into to avoid checking a line number in the same method twice. */
            Range<Integer> lastRange = null;

            for (Integer line : test.getLineCoverage().getLinesCovered()) {

                /* Skip if line falls into a method that was already considered. */
                if (lastRange != null && lastRange.contains(line)) {
                    continue;
                }

                Entry<Range<Integer>, TestCarouselInfo> entry = methodRanges.getEntry(line);

                /* Line does not belong to any method -> outside methods. */
                if (entry == null) {
                    outsideMethods.coveringTests.add(test);
                    lastRange = beforeFirst;

                /* Line belongs to a method. */
                } else {
                    lastRange = entry.getKey();
                    entry.getValue().coveringTests.add(test);
                }
            }
        }
    }

    public List<TestCarouselInfo> getInfos() {
        return Collections.unmodifiableList(testCarouselInfos);
    }

    public static class TestCarouselInfo {
        public TestCarouselInfo(String description, int startLine, int endLine) {
            this.description = description;
            this.startLine = startLine;
            this.endLine = endLine;
            this.coveringTests = new HashSet<>();
        }

        private String description;
        private int startLine;
        private int endLine;
        private Set<Test> coveringTests;

        public String getDescription() {
            return description;
        }

        public Set<Test> getCoveringTests() {
            return Collections.unmodifiableSet(coveringTests);
        }
    }
}
