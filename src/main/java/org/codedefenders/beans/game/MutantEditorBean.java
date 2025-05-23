/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.beans.game;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameState;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.model.Dependency;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.util.FileUtils;
import org.codedefenders.util.concurrent.EditorUtils;

/**
 * <p>Provides data for the mutant editor game component.</p>
 * <p>Bean Name: {@code mutantEditor}</p>
 */
@Named("mutantEditor")
@RequestScoped
public class MutantEditorBean {

    private final GameClassRepository gameClassRepo;

    /**
     * The class name of the mutant.
     */
    private String className;

    /**
     * The mutant code to display.
     */
    private String mutantCode;

    /**
     * The dependencies of the mutant to display, mapped by their names.
     * Can be empty, but must not be {@code null}.
     */
    private Map<String, String> dependencies;

    /**
     * Start of editable lines in the mutant.
     * If {@code null}, the code can be modified from the start.
     */
    private Integer editableLinesStart;

    /**
     * End of editable lines in the mutant.
     * If {@code null}, the code can be modified until the end.
     */
    private Integer editableLinesEnd;

    /**
     * Whether the editor is readonly.
     */
    private boolean readonly;

    @Inject
    public MutantEditorBean(GameClassRepository gameClassRepo, GameProducer gameProducer,
                            PreviousSubmissionBean previousSubmission) {
        AbstractGame game = gameProducer.getGame();
        GameClass clazz = game.getCUT();

        this.gameClassRepo = gameClassRepo;
        mutantCode = null;
        editableLinesStart = null;
        editableLinesEnd = null;

        setClassName(clazz.getName());
        setDependenciesForClass(clazz);
        if (previousSubmission.hasMutant()) {
            setPreviousMutantCode(previousSubmission.getMutantCode());
        } else {
            setMutantCodeForClass(clazz);
        }

        if (game instanceof PuzzleGame p) {
            setEditableLinesForPuzzle(p.getPuzzle());
        }
        setReadonly(game.getState() == GameState.SOLVED || game.getState() == GameState.FINISHED);
    }

    /**
     * Sets the className. This method is called upon the information of a {@link GameClass}. Since
     * {@link GameClass#getName()} contains the fully qualified name, the package information must be removed.
     * @param className The fully qualified name of the class
     */
    public void setClassName(String className) {
        String[] split = className.split("\\.");
        this.className = split[split.length - 1];
    }

    public void setMutantCodeForClass(GameClass clazz) {
        mutantCode = StringEscapeUtils.escapeHtml4(clazz.getSourceCode());
    }

    /**
     * Sets the code for the mutant editor from the previous submission of the player.
     * @param previousMutantCode The code from the previous submission, not HTML-escaped.
     */
    public void setPreviousMutantCode(String previousMutantCode) {
        mutantCode = StringEscapeUtils.escapeHtml4(previousMutantCode);
    }

    public void setDependenciesForClass(GameClass clazz) {
        dependencies = EditorUtils.getDependencyHashMap(clazz.getId(), gameClassRepo);
    }

    public void setEditableLinesForPuzzle(Puzzle puzzle) {
        this.editableLinesStart = puzzle.getEditableLinesStart();
        this.editableLinesEnd = puzzle.getEditableLinesEnd();
    }

    // --------------------------------------------------------------------------------

    public String getClassName() {
        return className;
    }

    /**
     * Returns the HTML-escaped code of the mutant.
     * @return The HTML-escaped code of the mutant.
     */
    public String getMutantCode() {
        return mutantCode;
    }

    public Map<String, String> getDependencies() {
        return dependencies;
    }

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    public int getEditableLinesStart() {
        return editableLinesStart == null ? 1 : editableLinesStart;
    }

    public int getEditableLinesEnd() {
        return editableLinesEnd;
    }

    public boolean hasEditableLinesStart() {
        return editableLinesStart != null;
    }

    public boolean hasEditableLinesEnd() {
        return editableLinesEnd != null;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }
}
