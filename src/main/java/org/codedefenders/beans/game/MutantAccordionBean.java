package org.codedefenders.beans.game;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import org.apache.commons.lang.StringEscapeUtils;
import org.codedefenders.database.UserDAO;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.Mutant;
import org.codedefenders.util.Constants;
import org.codedefenders.util.JSONUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;

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
    private List<Mutant> aliveMutants;
    private List<Mutant> killedMutants;
    private List<Mutant> equivalentMutants;
    private List<Mutant> flaggedMutants;
    private List<MutantAccordionCategory> categories;

    public MutantAccordionBean() {
        enableFlagging = null;
        gameMode = null;
        gameId = null;
        viewDiff = null;
        cut = null;
        aliveMutants = null;
        killedMutants = null;
        equivalentMutants = null;
        flaggedMutants = null;
        categories = null;
    }

    public void setMutantAccordionData(GameClass cut,
                                       List<Mutant> aliveMutants,
                                       List<Mutant> killedMutants,
                                       List<Mutant> equivalentMutants,
                                       List<Mutant> flaggedMutants) {
        this.cut = cut;
        this.aliveMutants = Collections.unmodifiableList(aliveMutants);
        this.killedMutants = Collections.unmodifiableList(killedMutants);
        this.equivalentMutants = Collections.unmodifiableList(equivalentMutants);
        this.flaggedMutants = Collections.unmodifiableList(flaggedMutants);

        categories = new ArrayList<>();

        MutantAccordionCategory allMutants = new MutantAccordionCategory("All Mutants", "all");
        allMutants.addMutantIds(getMutants().stream().map(x -> x.id).collect(Collectors.toList()));
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
        return enableFlagging && gameMode == GameMode.PARTY;
    }

    public Integer getGameId() {
        return gameId;
    }

    public boolean getViewDiff() {
        return viewDiff;
    }

    public List<MutantAccordionMutantDTO> getMutants() {
        // TODO Why do mutants don't have this simple attribute?
        List<MutantAccordionMutantDTO> mutants = new ArrayList<>();
        mutants.addAll(aliveMutants
                .stream()
                .map(x -> new MutantAccordionMutantDTO(x, MutantState.ALIVE))
                .collect(Collectors.toList()));
        mutants.addAll(killedMutants
                .stream()
                .map(x -> new MutantAccordionMutantDTO(x, MutantState.KILLED))
                .collect(Collectors.toList()));
        mutants.addAll(equivalentMutants
                .stream()
                .map(x -> new MutantAccordionMutantDTO(x, MutantState.EQUIVALENT))
                .collect(Collectors.toList()));
        mutants.addAll(flaggedMutants
                .stream()
                .map(x -> new MutantAccordionMutantDTO(x, MutantState.FLAGGED))
                .collect(Collectors.toList()));
        return mutants;
    }

    public List<MutantAccordionCategory> getCategories() {
        return categories;
    }

    public List<Mutant> getAllMutants() {
        List<Mutant> mutants = new ArrayList<>();
        mutants.addAll(aliveMutants);
        mutants.addAll(killedMutants);
        mutants.addAll(equivalentMutants);
        mutants.addAll(flaggedMutants);
        return mutants;
    }

    public List<Mutant> getAliveMutants() {
        return aliveMutants;
    }

    public List<Mutant> getKilledMutants() {
        return killedMutants;
    }

    public List<Mutant> getEquivalentMutants() {
        return equivalentMutants;
    }

    public List<Mutant> getFlaggedMutants() {
        return flaggedMutants;
    }

    public boolean hasAliveMutants() {
        return !aliveMutants.isEmpty();
    }

    public boolean hasKilledMutants() {
        return !killedMutants.isEmpty();
    }

    public boolean hasEquivalentMutants() {
        return !equivalentMutants.isEmpty();
    }

    public boolean hasFlaggedMutants() {
        return !flaggedMutants.isEmpty();
    }

    public String jsonFromCategories() {
        Gson gson = new GsonBuilder()
                .create();
        return gson.toJson(categories);
    }

    public String jsonMutants() {
        Map<Integer, MutantAccordionMutantDTO> mutants = new HashMap<>();

        for (Mutant mutant : aliveMutants) {
            mutants.put(mutant.getId(), new MutantAccordionMutantDTO(mutant, MutantState.ALIVE));
        }
        for (Mutant mutant : killedMutants) {
            mutants.put(mutant.getId(), new MutantAccordionMutantDTO(mutant, MutantState.KILLED));
        }
        for (Mutant mutant : flaggedMutants) {
            mutants.put(mutant.getId(), new MutantAccordionMutantDTO(mutant, MutantState.FLAGGED));
        }
        for (Mutant mutant : equivalentMutants) {
            mutants.put(mutant.getId(), new MutantAccordionMutantDTO(mutant, MutantState.EQUIVALENT));
        }
        // TODO If we try to sort the mutants according to the order they appear in the
        //  class we need to sort the Ids in the MutantAccordionCategory.
        Gson gson = new GsonBuilder()
                // It is important that its LinkedHashMap.class, it doesn't work if I change it to Map.class ...
                .registerTypeAdapter(HashMap.class, new JSONUtils.MapSerializer())
                .create();
        return gson.toJson(mutants);
    }

    public static Comparator<MutantAccordionMutantDTO> sortedByLineNumberAscending() {
        return (o1, o2) -> {
            List<Integer> lines1 = o1.lines;
            List<Integer> lines2 = o2.lines;

            if (lines1.isEmpty()) {
                if (lines2.isEmpty()) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (lines2.isEmpty()) {
                return 1;
            }

            return Collections.min(lines1) - Collections.min(lines2);
        };
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

    public static class MutantAccordionMutantDTO {
        @Expose
        private final int id;
        @Expose
        private final String creatorName;
        @Expose
        private final MutantState state;
        @Expose
        private final int points;
        @Expose
        private final String lineString;
        @Expose
        private final Boolean covered;
        @Expose
        private final String killedByName;
        @Expose
        private final boolean canMarkEquivalent;
        @Expose
        private final boolean canView;
        private final List<Integer> lines;
        @Expose
        private final int killedByTestId;
        @Expose
        private final String killMessage;
        @Expose
        private final String description;

        public MutantAccordionMutantDTO(Mutant mutant, MutantState state) {
            id = mutant.getId();
            creatorName = mutant.getCreatorName();
            points = mutant.getScore();
            this.state = state;
            covered = mutant.isCovered();
            description = StringEscapeUtils.escapeJavaScript(mutant.getHTMLReadout()
                    .stream()
                    .filter(Objects::nonNull).collect(Collectors.joining("<br>")));
            if (mutant.getKillingTest() != null) {
                killedByName = UserDAO.getUserForPlayer(mutant.getKillingTest().getPlayerId()).getUsername();
                killedByTestId = mutant.getKillingTest().getId();
                killMessage = StringEscapeUtils.escapeJavaScript(mutant.getKillMessage());
            } else {
                killedByName = null;
                killedByTestId = -1;
                killMessage = null;
            }


            lines = mutant.getLines();
            lineString = lines.stream().map(String::valueOf).collect(Collectors.joining(","));
            canMarkEquivalent = mutant.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO)
                    && mutant.getCreatorId() != Constants.DUMMY_ATTACKER_USER_ID
                    && mutant.getLines().size() >= 1;
            canView = state == MutantState.KILLED || state == MutantState.EQUIVALENT;
        }
    }

    // TODO: Do we have this already somwhere?
    public enum MutantState {
        KILLED,
        ALIVE,
        EQUIVALENT,
        FLAGGED
    }
}
