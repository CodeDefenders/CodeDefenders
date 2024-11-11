package org.codedefenders.beans.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.codedefenders.analysis.gameclass.MethodDescription;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.dto.TestDTO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameAccordionMapping;
import org.codedefenders.game.GameClass;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private List<TestAccordionCategory> categories;

    /**
     * Maps test ids to tests.
     */
    private Map<Integer, TestDTO> tests;

    @Inject
    public TestAccordionBean(CodeDefendersAuth login, GameService gameService, GameProducer gameProducer) {
        var game = gameProducer.getGame();
        if (game != null) {
            GameClass cut = game.getCUT();
            List<TestDTO> testsList = gameService.getTests(login.getUserId(), game.getId());
            init(cut, testsList);
        }
    }

    public void init(GameClass cut, List<TestDTO> testsList) {
        List<MethodDescription> methodDescriptions = cut.getMethodDescriptions();
        GameAccordionMapping mapping = GameAccordionMapping.computeForTests(methodDescriptions, testsList);

        categories = new ArrayList<>();
        categories.add(
                new TestAccordionCategory(
                        "All Tests",
                        GameAccordionMapping.ALL_CATEGORY_ID,
                        mapping.allElements));

        for (int i = 0; i < methodDescriptions.size(); i++) {
            MethodDescription method = methodDescriptions.get(i);

            String description = method.getDescription();
            String id = Integer.toString(i);
            SortedSet<Integer> testIds = mapping.elementsPerMethod.get(method);

            categories.add(new TestAccordionCategory(description, id, testIds));
        }

        tests = testsList.stream()
                .collect(Collectors.toMap(TestDTO::getId, Function.identity()));
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
        @Expose
        private String id;
        @Expose
        private SortedSet<Integer> testIds;

        public TestAccordionCategory(String description, String id, SortedSet<Integer> testIds) {
            this.description = description;
            this.id = id;
            this.testIds = testIds;
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
