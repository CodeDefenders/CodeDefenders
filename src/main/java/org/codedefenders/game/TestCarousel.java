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

public class TestCarousel {
    @Expose private List<TCMethodDescription> methodDescriptions;
    @Expose private Map<Integer, TCTestDTO> tests;

    public TestCarousel(GameClass cut, List<Test> testsList, List<Mutant> mutantsList) {
        this.tests = new HashMap<>();
        for (Test test : testsList) {
            this.tests.put(test.getId(), new TCTestDTO(test, mutantsList));
        }

        List<TCMethodDescription> actualMethodInfos = cut.getTestCarouselMethodDescriptions();

        /* Map ranges of methods to their test carousel infos. */
        @SuppressWarnings("UnstableApiUsage")
        RangeMap<Integer, TCMethodDescription> methodRanges = TreeRangeMap.create();
        for (TCMethodDescription TCMethodDescription : actualMethodInfos) {
            methodRanges.put(Range.closed(TCMethodDescription.startLine, TCMethodDescription.endLine), TCMethodDescription);
        }

        Range<Integer> beforeFirst = Range.closedOpen(0, methodRanges.span().lowerEndpoint());

        /* Represents code outside of methods. */
        TCMethodDescription outsideMethods = new TCMethodDescription(
                "Code Outside Of Methods",
                beforeFirst.lowerEndpoint(),
                beforeFirst.upperEndpoint());

        methodDescriptions = new ArrayList<>();
        methodDescriptions.add(outsideMethods);
        methodDescriptions.addAll(cut.getTestCarouselMethodDescriptions());

        /* For every test, go through all covered lines and find the methods that are covered by it. */
        for (Test test : testsList) {

            /* Save the last range a line number fell into to avoid checking a line number in the same method twice. */
            Range<Integer> lastRange = null;

            for (Integer line : test.getLineCoverage().getLinesCovered()) {

                /* Skip if line falls into a method that was already considered. */
                if (lastRange != null && lastRange.contains(line)) {
                    continue;
                }

                Entry<Range<Integer>, TCMethodDescription> entry = methodRanges.getEntry(line);

                /* Line does not belong to any method -> outside methods. */
                if (entry == null) {
                    outsideMethods.coveringTestIds.add(test.getId());
                    lastRange = beforeFirst;

                    /* Line belongs to a method. */
                } else {
                    lastRange = entry.getKey();
                    entry.getValue().coveringTestIds.add(test.getId());
                }
            }
        }
    }

    public List<TCMethodDescription> getInfos() {
        return Collections.unmodifiableList(methodDescriptions);
    }

    public static class TCMethodDescription {
        @Expose private String description;
        private int startLine;
        private int endLine;
        @Expose private Set<Integer> coveringTestIds;

        public TCMethodDescription(String description, int startLine, int endLine) {
            this.description = description;
            this.startLine = startLine;
            this.endLine = endLine;
            this.coveringTestIds = new HashSet<>();
        }

        public String getDescription() {
            return description;
        }

        public Set<Integer> getCoveringTests() {
            return Collections.unmodifiableSet(coveringTestIds);
        }
    }

    public static class TCTestDTO {
        @Expose private int id;
        @Expose private int creatorId;
        @Expose private String creatorName;
        @Expose private int numCoveredMutants;
        @Expose private int numKilledMutants;
        @Expose private int points;
        @Expose private List<String> smells;

        public TCTestDTO(Test test, List<Mutant> mutants) {
            User creator = UserDAO.getUserForPlayer(test.getPlayerId());
            this.id = test.getId();
            this.creatorId = creator.getId();
            this.creatorName = creator.getUsername();
            this.numCoveredMutants = test.getCoveredMutants(mutants).size();
            this.numCoveredMutants = test.getKilledMutants().size(); // TODO: very slow, pass mutants like in getCoveredMutants
            this.points = test.getScore();
            this.smells = (new TestSmellsDAO()).getDetectedTestSmellsForTest(test);
        }
    }
}
