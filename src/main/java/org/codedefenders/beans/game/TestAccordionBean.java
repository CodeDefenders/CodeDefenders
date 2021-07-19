package org.codedefenders.beans.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.dto.TestDTO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * <p>Provides data for the test accordion game component.</p>
 * <p>Bean Name: {@code testAccordion}</p>
 */
@Named(value = "testAccordion")
@RequestScoped
public class TestAccordionBean {

    private static final Logger logger = LoggerFactory.getLogger(TestAccordionBean.class);

    /**
     * The categories of the test accordion, in sorted order.
     * One category containing all tests and one category for each method of the class,
     * containing the tests that cover the method.
     */
    private final List<TestAccordionCategory> categories;

    /**
     * Maps test ids to tests.
     */
    private final Map<Integer, TestDTO> tests;

    @Inject
    public TestAccordionBean(LoginBean login, GameService gameService, GameProducer gameProducer) {
        AbstractGame game = gameProducer.getGame();

        GameClass cut = game.getCUT();
        List<TestDTO> testsList = gameService.getTests(login.getUser(), game);

        tests = new HashMap<>();
        categories = new ArrayList<>();

        for (TestDTO test : testsList) {
            tests.put(test.getId(), test);
        }

        TestAccordionCategory allTests = new TestAccordionCategory("All Tests", "all");
        allTests.addTestIds(tests.keySet());

        List<GameClass.MethodDescription> methodDescriptions = cut.getMethodDescriptions();
        List<TestAccordionCategory> methodCategories = new ArrayList<>();
        for (int i = 0; i < methodDescriptions.size(); i++) {
            methodCategories.add(new TestAccordionCategory(methodDescriptions.get(i), String.valueOf(i)));
        }

        categories.add(allTests);
        categories.addAll(methodCategories);

        if (methodCategories.isEmpty()) {
            return;
        }

        /* Map ranges of methods to their test accordion infos. */
        @SuppressWarnings("UnstableApiUsage")
        RangeMap<Integer, TestAccordionCategory> methodRanges = TreeRangeMap.create();
        for (TestAccordionCategory method : methodCategories) {
            methodRanges.put(Range.closed(method.startLine, method.endLine), method);
        }

        Range<Integer> beforeFirst = Range.closedOpen(0, methodRanges.span().lowerEndpoint());

        /* For every test, go through all covered lines and find the methods that are covered by it. */
        for (TestDTO test : testsList) {

            /* Save the last range a line number fell into to avoid checking a line number in the same method twice. */
            Range<Integer> lastRange = null;

            for (Integer line : test.getLinesCovered()) {

                /* Skip if line falls into a method that was already considered. */
                if (lastRange != null && lastRange.contains(line)) {
                    continue;
                }

                Map.Entry<Range<Integer>, TestAccordionCategory> entry = methodRanges.getEntry(line);

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

    // --------------------------------------------------------------------------------

    /**
     * Returns the categories of the test accordion.
     *
     * @return The categories of the test accordion.
     */
    public List<TestAccordionCategory> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    public Map<Integer, TestDTO> getTests() {
        return tests;
    }

    public String getCategoriesAsJSON() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(categories);
    }

    public String getTestsAsJSON() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapterFactory(new JSONUtils.MapTypeAdapterFactory())
                .create();
        return gson.toJson(tests);
    }


    /**
     * Represents a category (accordion section) of the test accordion.
     */
    public static class TestAccordionCategory {
        @Expose
        private String description;
        private Integer startLine;
        private Integer endLine;
        @Expose
        private Set<Integer> testIds;
        @Expose
        private String id;

        public TestAccordionCategory(String description, String id) {
            this.description = description;
            this.testIds = new HashSet<>();
            this.id = id;
        }

        public TestAccordionCategory(GameClass.MethodDescription methodDescription, String id) {
            this(methodDescription.getDescription(), id);
            this.startLine = methodDescription.getStartLine();
            this.endLine = methodDescription.getEndLine();
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
}
