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
import org.codedefenders.dto.MutantDTO;
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

/**
 * <p>Provides data for the mutant accordion game component.</p>
 * <p>Bean Name: {@code mutantAccordion}</p>
 */
@Named(value = "mutantAccordion")
@RequestScoped
public class MutantAccordionBean {
    private static final Logger logger = LoggerFactory.getLogger(MutantAccordionBean.class);

    private final AbstractGame game;

    private final List<MutantDTO> mutantList;
    private final List<MutantAccordionCategory> categories;

    @Inject
    public MutantAccordionBean(GameService gameService, CodeDefendersAuth login, GameProducer gameProducer) {
        game = gameProducer.getGame();
        mutantList = gameService.getMutants(login.getUserId(), game.getId());

        GameClass cut = game.getCUT();
        List<MethodDescription> methodDescriptions = cut.getMethodDescriptions();
        GameAccordionMapping mapping = GameAccordionMapping.computeForMutants(methodDescriptions, mutantList);

        categories = new ArrayList<>();
        categories.add(
                new MutantAccordionCategory(
                        "All Mutants",
                        GameAccordionMapping.ALL_CATEGORY_ID,
                        mapping.allElements));
        categories.add(
                new MutantAccordionCategory(
                        "Mutants outside methods",
                        GameAccordionMapping.OUTSIDE_METHODS_CATEGORY_ID,
                        mapping.elementsOutsideMethods));

        for (int i = 0; i < methodDescriptions.size(); i++) {
            MethodDescription method = methodDescriptions.get(i);

            String description = method.getDescription();
            String id = Integer.toString(i);
            SortedSet<Integer> testIds = mapping.elementsPerMethod.get(method);

            categories.add(new MutantAccordionCategory(description, id, testIds));
        }
    }

    public Integer getGameId() {
        return game.getId();
    }

    public List<MutantDTO> getMutants() {
        return mutantList;
    }

    public List<MutantAccordionCategory> getCategories() {
        return categories;
    }

    public String jsonFromCategories() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(categories);
    }

    public String jsonMutants() {
        Map<Integer, MutantDTO> mutants = this.mutantList.stream()
                .collect(Collectors.toMap(MutantDTO::getId, Function.identity()));
        // TODO If we try to sort the mutants according to the order they appear in the
        //  class we need to sort the Ids in the MutantAccordionCategory.
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapterFactory(new JSONUtils.MapTypeAdapterFactory())
                .create();
        // We need to do the JavaScript escaping in the end, since otherwise {@code '} character don't get escaped
        // properly
        return StringEscapeUtils.escapeEcmaScript(gson.toJson(mutants));
    }

    public static class MutantAccordionCategory {
        @Expose
        private String description;
        @Expose
        private String id;
        @Expose
        private SortedSet<Integer> mutantIds;

        public MutantAccordionCategory(String description, String id, SortedSet<Integer> mutantIds) {
            this.description = description;
            this.id = id;
            this.mutantIds = mutantIds;
        }

        public String getDescription() {
            return description;
        }

        public Set<Integer> getMutantIds() {
            return Collections.unmodifiableSet(mutantIds);
        }

        public String getId() {
            return id;
        }

    }
}
