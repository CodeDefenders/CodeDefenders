package org.codedefenders.beans.game;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.Mutant;
import org.codedefenders.model.User;
import org.codedefenders.util.JSONUtils;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;
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
@ManagedBean
@RequestScoped
public class MutantAccordionBean {
    /**
     * Show a button to flag a selected mutant as equivalent.
     */
    private Boolean enableFlagging;

    /**
     * The mode of the currently played game.
     * Used to determine how to flag mutants.
     */
    private GameMode gameMode;

    /**
     * The game id of the currently played game.
     * Used for URL parameters.
     */
    private Integer gameId;

    /**
     * Enable viewing of the mutant diffs.
     */
    private Boolean viewDiff;

    private GameClass cut;
    private List<Mutant> mutantList;
    private List<MutantAccordionCategory> categories;

    private User user;

    private boolean playerCoverToClaim;

    public MutantAccordionBean() {
        enableFlagging = null;
        gameMode = null;
        gameId = null;
        viewDiff = null;
        cut = null;
        mutantList = null;
        categories = null;
        user = null;
    }

    public void setMutantAccordionData(GameClass cut,
                                       User user,
                                       List<Mutant> mutants) {
        this.cut = cut;
        this.user = user;
        this.mutantList = Collections.unmodifiableList(mutants);
        this.playerCoverToClaim = false;

        categories = new ArrayList<>();

        MutantAccordionCategory allMutants = new MutantAccordionCategory("All Mutants", "all");
        allMutants.addMutantIds(getMutants().stream().map(MutantDTO::getId).collect(Collectors.toList()));
        categories.add(allMutants);

        MutantAccordionCategory mutantsWithoutMethod =
                new MutantAccordionCategory("Mutants outside methods", "noMethod");
        categories.add(mutantsWithoutMethod);

        List<GameClass.MethodDescription> methodDescriptions = cut.getMethodDescriptions();
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
        for (Mutant mutant : getAllMutants()) {

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

    public void setPlayerCoverToClaim(boolean playerCoverToClaim) {
        this.playerCoverToClaim = playerCoverToClaim;
    }

    public void setEnableFlagging(boolean enableFlagging) {
        this.enableFlagging = enableFlagging;
    }

    public void setFlaggingData(GameMode gameMode, int gameId) {
        this.gameMode = gameMode;
        this.gameId = gameId;
    }

    public void setViewDiff(boolean viewDiff) {
        this.viewDiff = viewDiff;
    }

    // --------------------------------------------------------------------------------

    public GameMode getGameMode() {
        return gameMode;
    }

    public Boolean getEnableFlagging() {
        return enableFlagging;
    }

    public Boolean canFlag() {
        return enableFlagging && (gameMode == GameMode.PARTY || gameMode == GameMode.MELEE);
    }

    public Integer getGameId() {
        return gameId;
    }

    public boolean getViewDiff() {
        return viewDiff;
    }

    public List<MutantDTO> getMutants() {
        return mutantList.stream().map(m -> new MutantDTO(m, user, playerCoverToClaim))
                .collect(Collectors.toList());
    }

    public List<MutantAccordionCategory> getCategories() {
        return categories;
    }

    public List<Mutant> getAllMutants() {
        return mutantList;
    }

    public String jsonFromCategories() {
        Gson gson = new GsonBuilder()
                .create();
        return gson.toJson(categories);
    }

    public String jsonMutants() {
        Map<Integer, MutantDTO> mutants =
                mutantList.stream().collect(Collectors.toMap(Mutant::getId,
                        m -> new MutantDTO(m, user, playerCoverToClaim)));
        // TODO If we try to sort the mutants according to the order they appear in the
        //  class we need to sort the Ids in the MutantAccordionCategory.
        Gson gson = new GsonBuilder()
                // It is important that its LinkedHashMap.class, it doesn't work if I change it to Map.class ...
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
