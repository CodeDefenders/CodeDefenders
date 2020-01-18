package org.codedefenders.beans.game;

import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.Mutant;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>Provides data for the mutant accordion game component.</p>
 * <p>Bean Name: {@code mutantAccordion}</p>
 */
@ManagedBean
@RequestScoped
// TODO: For now this just has setters for the attributes from the mutant list (+ the CUT). Change this as needed.
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

    public Integer getGameId() {
        return gameId;
    }

    public boolean getViewDiff() {
        return viewDiff;
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
}
