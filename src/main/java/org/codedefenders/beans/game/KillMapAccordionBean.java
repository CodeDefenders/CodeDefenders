package org.codedefenders.beans.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.execution.KillMap;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named(value = "killMapAccordion")
@ManagedBean
@RequestScoped
public class KillMapAccordionBean {

    private final MutantAccordionBean mutantAccordionBean;
    private final TestAccordionBean testAccordionBean;
    private final GameService gameService;
    private final GameProducer gameProducer;
    private final AbstractGame game;

    private Map<Integer, Map<Integer, KillMap.KillMapEntry.Status>> killMapForTests;
    private Map<Integer, Map<Integer, KillMap.KillMapEntry.Status>> killMapForMutants;

    private final List<KillMapAccordionCategory> categories = new ArrayList<>();
    private final Map<Integer, MutantDTO> mutantsById;

    private static final Logger logger = LoggerFactory.getLogger(KillMapAccordionBean.class);

    @Inject
    public KillMapAccordionBean(MutantAccordionBean mutantAccordionBean,
                                TestAccordionBean testAccordionBean,
                                GameService gameService, GameProducer gameProducer) {
        this.mutantAccordionBean = mutantAccordionBean;
        this.testAccordionBean = testAccordionBean;
        this.gameService = gameService;
        this.gameProducer = gameProducer;
        this.game = gameProducer.getGame();

        this.killMapForTests = getKillMapForTests();
        this.killMapForMutants = getKillMapForMutants();

        mutantsById = this.mutantAccordionBean.getMutants().stream()
                .collect(Collectors.toMap(MutantDTO::getId, Function.identity()));

        initCategories();
    }

    private void initCategories() {
        int categoryId = 0;
        for (MutantAccordionBean.MutantAccordionCategory mc : mutantAccordionBean.getCategories()) {
            for (TestAccordionBean.TestAccordionCategory tc : testAccordionBean.getCategories()) {
                boolean isSameMethod = mc.getDescription().equals(tc.getDescription());
                boolean isMutantCategoryAll = mc.getDescription().equals("All Mutants");
                boolean isTestCategoryAll = tc.getDescription().equals("All Tests");
                boolean isCategoryOutsideMethods = mc.getDescription().equals("Mutants outside methods");
                if (isSameMethod || isMutantCategoryAll && isTestCategoryAll ||
                        isCategoryOutsideMethods && isTestCategoryAll) {
                    KillMapAccordionCategory category = new KillMapAccordionCategory(
                            mc.getDescription(),
                            String.valueOf(categoryId++)
                    );
                    category.addTestIds(tc.getTestIds());
                    category.addMutantIds(mc.getMutantIds());
                    categories.add(category);
                }
            }
        }
    }

    List<KillMap.KillMapEntry> getKillMap() {
        return KillmapDAO.getKillMapEntriesForGame(game.getId());
    }

    private Map<Integer, Map<Integer, KillMap.KillMapEntry.Status>> getKillMapForTests() {
        Map<Integer, Map<Integer, KillMap.KillMapEntry.Status>> killMapForTests = new HashMap<>();
        for (KillMap.KillMapEntry entry : getKillMap()) {
            if (!killMapForTests.containsKey(entry.test.getId())) {
                killMapForTests.put(entry.test.getId(), new HashMap<>());
            }
            killMapForTests.get(entry.test.getId()).put(entry.mutant.getId(), entry.status);
        }
        return killMapForTests;
    }

    private Map<Integer, Map<Integer, KillMap.KillMapEntry.Status>> getKillMapForMutants() {
        Map<Integer, Map<Integer, KillMap.KillMapEntry.Status>> killMapForMutants = new HashMap<>();
        for (KillMap.KillMapEntry entry : getKillMap()) {
            if (!killMapForMutants.containsKey(entry.mutant.getId())) {
                killMapForMutants.put(entry.mutant.getId(), new HashMap<>());
            }
            killMapForMutants.get(entry.mutant.getId()).put(entry.test.getId(), entry.status);
        }
        return killMapForMutants;
    }

    public String getKillMapForTestsAsJSON() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(killMapForTests);
    }

    public String getKillMapForMutantsAsJSON() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(killMapForMutants);
    }

    public String getCategoriesAsJSON() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(categories);
    }

    public String getTestsAsJSON() {
        return testAccordionBean.getTestsAsJSON();
    }

    public String getMutantsAsJSON() {
        return mutantAccordionBean.jsonMutants();
    }

    public int getGameId() {
        return game.getId();
    }

    public List<KillMapAccordionCategory> getCategories() {
        return categories;
    }

    public List<MutantDTO> getMutantsByCategory(KillMapAccordionCategory category) {
        return category.getMutantIds().stream()
                .map(mutantsById::get)
                .collect(Collectors.toList());
    }

    /**
     * Represents a category (accordion section) of the test accordion.
     */
    public static class KillMapAccordionCategory {
        @Expose
        private String description;
        @Expose
        private Set<Integer> testIds;
        @Expose
        private Set<Integer> mutantIds;
        @Expose
        private String id;

        public KillMapAccordionCategory(String description, String id) {
            this.description = description;
            this.testIds = new HashSet<>();
            this.mutantIds = new HashSet<>();
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public void addTestId(int testId) {
            this.testIds.add(testId);
        }

        public void addTestIds(Collection<Integer> testIds) {
            this.testIds.addAll(testIds);
        }

        public Set<Integer> getTestIds() {
            return Collections.unmodifiableSet(testIds);
        }

        public void addMutantId(int mutantId) {
            this.mutantIds.add(mutantId);
        }

        public void addMutantIds(Collection<Integer> mutantIds) {
            this.mutantIds.addAll(mutantIds);
        }

        public Set<Integer> getMutantIds() {
            return Collections.unmodifiableSet(mutantIds);
        }
    }


}
