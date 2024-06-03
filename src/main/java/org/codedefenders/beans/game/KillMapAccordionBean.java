package org.codedefenders.beans.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.analysis.gameclass.MethodDescription;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.dto.KillMapDTO;
import org.codedefenders.dto.MutantDTO;
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
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named(value = "killMapAccordion")
@RequestScoped
public class KillMapAccordionBean {
    private final AbstractGame game;
    private final KillMapDTO killMap;
    private final List<KillMapAccordionCategory> categories = new ArrayList<>();
    private final Map<Integer, MutantDTO> mutantsById;
    private final Map<Integer, TestDTO> testsById;
    private static final Logger logger = LoggerFactory.getLogger(KillMapAccordionBean.class);

    /**
     * The bean providing data for the kill-map test and mutant accordions.
     *
     * @param login         The current login.
     * @param gameService   The GameService to get tests and mutants from.
     * @param gameProducer  The GameProducer for the current game.
     */
    @Inject
    public KillMapAccordionBean(CodeDefendersAuth login, GameService gameService, GameProducer gameProducer) {
        game = gameProducer.getGame();
        assert game != null;
        killMap = new KillMapDTO(game.getId());

        mutantsById = gameService.getMutants(login.getUserId(), game.getId()).stream()
                .collect(Collectors.toMap(MutantDTO::getId, Function.identity()));
        testsById = gameService.getTests(login.getUserId(), game.getId()).stream()
                .collect(Collectors.toMap(TestDTO::getId, Function.identity()));

        initCategories();
    }

    private void initCategories() {
        GameClass cut = game.getCUT();
        List<MethodDescription> methods = cut.getMethodDescriptions();
        GameAccordionMapping testsMapping = GameAccordionMapping.computeForTests(methods, testsById.values());
        GameAccordionMapping mutantsMapping = GameAccordionMapping.computeForMutants(methods, mutantsById.values());

        categories.add(
                new KillMapAccordionCategory(
                        "All Mutants and Tests",
                        GameAccordionMapping.ALL_CATEGORY_ID,
                        testsMapping.allElements,
                        mutantsMapping.allElements));
        categories.add(
                new KillMapAccordionCategory(
                        "Mutants outside methods",
                        GameAccordionMapping.OUTSIDE_METHODS_CATEGORY_ID,
                        testsMapping.allElements,
                        mutantsMapping.elementsOutsideMethods));

        for (int i = 0; i < methods.size(); i++) {
            MethodDescription method = methods.get(i);

            String description = method.getDescription();
            String id = Integer.toString(i);
            SortedSet<Integer> testIds = testsMapping.elementsPerMethod.get(method);
            SortedSet<Integer> mutantIds = mutantsMapping.elementsPerMethod.get(method);

            categories.add(new KillMapAccordionCategory(description, id, testIds, mutantIds));
        }
    }

    public String getKillMapForMutantsAsJSON() {
        return killMap.getKillMapForMutantsAsJSON();
    }

    public String getCategoriesAsJSON() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(getCategories());
    }

    public String getCategoriesForTestsAsJSON() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(getCategoriesForTests());
    }

    // TODO(kreismar): this replicates TestAccordionBean#getTestsAsJSON(), we should extract this sometime
    public String getTestsAsJSON() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapterFactory(new JSONUtils.MapTypeAdapterFactory())
                .create();
        return gson.toJson(testsById);
    }

    // TODO(kreismar): this replicates MutantAccordionBean#jsonMutants(), we should extract this sometime
    public String getMutantsAsJSON() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapterFactory(new JSONUtils.MapTypeAdapterFactory())
                .create();
        return StringEscapeUtils.escapeEcmaScript(gson.toJson(mutantsById));
    }

    public int getGameId() {
        return game.getId();
    }

    public List<KillMapAccordionCategory> getCategories() {
        return categories;
    }

    public List<KillMapAccordionCategory> getCategoriesForTests() {
        return categories.stream()
                .filter(c -> !c.getId().equals(GameAccordionMapping.OUTSIDE_METHODS_CATEGORY_ID))
                .collect(Collectors.toList());
    }

    public List<MutantDTO> getMutantsByCategory(KillMapAccordionCategory category) {
        return category.getMutantIds().stream()
                .map(mutantsById::get)
                .collect(Collectors.toList());
    }

    public List<TestDTO> getTestsByCategory(KillMapAccordionCategory category) {
        return category.getTestIds().stream()
                .map(testsById::get)
                .collect(Collectors.toList());
    }


    /**
     * Represents a category (accordion section) of the test accordion.
     */
    public static class KillMapAccordionCategory {
        @Expose
        private String description;
        @Expose
        private String id;
        @Expose
        private SortedSet<Integer> testIds;
        @Expose
        private SortedSet<Integer> mutantIds;

        public KillMapAccordionCategory(String description, String id,
                                        SortedSet<Integer> testIds, SortedSet<Integer> mutantIds) {
            this.description = description;
            this.id = id;
            this.testIds = testIds;
            this.mutantIds = mutantIds;
        }

        public String getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public Set<Integer> getTestIds() {
            return Collections.unmodifiableSet(testIds);
        }

        public Set<Integer> getMutantIds() {
            return Collections.unmodifiableSet(mutantIds);
        }
    }


}
