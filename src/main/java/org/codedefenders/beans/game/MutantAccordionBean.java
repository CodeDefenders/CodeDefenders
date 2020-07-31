package org.codedefenders.beans.game;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.service.game.GameService;
import org.codedefenders.util.JSONUtils;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Provides data for the mutant accordion game component.</p>
 * <p>Bean Name: {@code mutantAccordion}</p>
 */
@Named(value = "mutantAccordion")
@RequestScoped
public class MutantAccordionBean {

    @Inject
    GameService gameService;

    @Inject
    LoginBean loginBean;

    @Inject
    AbstractGame game;

    private List<MutantDTO> mutantList;
    private List<MutantAccordionCategory> categories;

    @PostConstruct
    public void setup() {
        mutantList = gameService.getMutants(loginBean.getUser(), game);

        categories = new ArrayList<>();

        MutantAccordionCategory allMutants = new MutantAccordionCategory("All Mutants", "all");
        allMutants.addMutantIds(getMutants().stream().map(MutantDTO::getId).collect(Collectors.toList()));
        categories.add(allMutants);

        MutantAccordionCategory mutantsWithoutMethod =
                new MutantAccordionCategory("Mutants outside methods", "noMethod");
        categories.add(mutantsWithoutMethod);

        List<GameClass.MethodDescription> methodDescriptions = game.getCUT().getMethodDescriptions();
        List<MutantAccordionCategory> methodCategories = new ArrayList<>();
        for (int i = 0; i < methodDescriptions.size(); i++) {
            methodCategories.add(new MutantAccordionCategory(methodDescriptions.get(i), String.valueOf(i)));
        }
        categories.addAll(methodCategories);

        if (methodCategories.isEmpty()) {
            return;
        }

        /* Map ranges of methods to their test accordion infos. */
        @SuppressWarnings("UnstableApiUsage")
        RangeMap<Integer, MutantAccordionCategory> methodRanges = TreeRangeMap.create();
        for (MutantAccordionCategory method : methodCategories) {
            methodRanges.put(Range.closed(method.startLine, method.endLine), method);
        }

        Range<Integer> beforeFirst = Range.closedOpen(0, methodRanges.span().lowerEndpoint());


        /* For every test, go through all covered lines and find the methods that are covered by it. */
        for (MutantDTO mutant : mutantList) {

            /* Save the last range a line number fell into to avoid checking a line number in the same method twice. */
            Range<Integer> lastRange = null;

            boolean belongsMethod = false;

            for (Integer line : mutant.getLines()) {

                /* Skip if line falls into a method that was already considered. */
                if (lastRange != null && lastRange.contains(line)) {
                    continue;
                }

                Map.Entry<Range<Integer>, MutantAccordionCategory> entry = methodRanges.getEntry(line);

                /* Line does not belong to any method. */
                if (entry == null) {
                    lastRange = beforeFirst;

                    /* Line belongs to a method. */
                } else {
                    lastRange = entry.getKey();
                    belongsMethod = true;
                    entry.getValue().addMutantId(mutant.getId());
                }
            }

            if (!belongsMethod) {
                mutantsWithoutMethod.addMutantId(mutant.getId());
            }
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
                .create();
        return gson.toJson(categories);
    }

    public String jsonMutants() {
        Map<Integer, MutantDTO> mutants = this.mutantList.stream().collect(Collectors.toMap(MutantDTO::getId, m -> m));
        // TODO If we try to sort the mutants according to the order they appear in the
        //  class we need to sort the Ids in the MutantAccordionCategory.
        Gson gson = new GsonBuilder()
                // It is important that its HashMap.class, it doesn't work if I change it to Map.class …
                .registerTypeAdapter(HashMap.class, new JSONUtils.MapSerializer())
                .create();
        return gson.toJson(mutants);
    }

    public static class MutantAccordionCategory {
        @Expose
        private String description;
        private Integer startLine;
        private Integer endLine;
        @Expose
        private Set<Integer> mutantIds;
        @Expose
        private String id;

        public MutantAccordionCategory(String description, String id) {
            this.description = description;
            this.mutantIds = new HashSet<>();
            this.id = id;
        }

        public MutantAccordionCategory(GameClass.MethodDescription methodDescription, String id) {
            this(methodDescription.getDescription(), id);
            this.startLine = methodDescription.getStartLine();
            this.endLine = methodDescription.getEndLine();
        }

        public void addMutantId(int mutantId) {
            this.mutantIds.add(mutantId);
        }

        public void addMutantIds(Collection<Integer> mutantIds) {
            this.mutantIds.addAll(mutantIds);
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
