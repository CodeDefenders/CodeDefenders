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

import java.io.Serializable;
import java.util.List;

import jakarta.enterprise.context.SessionScoped;

/**
 * <p>Saves the previous mutant / test submission so the code of the previous submission can be displayed. </p>
 * <p>Bean Name: {@code previousSubmission}</p>
 */
@SessionScoped
// TODO: Put error message here and show it somewhere different than the messages?
public class PreviousSubmissionBean implements Serializable {
    private String mutantCode;
    private String testCode;
    private List<Integer> errorLines;

    /**
     * The selected line for the defender intention collection.
     * (The server-side implementation seems to be able to deal with multiple selected lines, but this is unused.)
     */
    private Integer selectedLine;

    public PreviousSubmissionBean() {
        mutantCode = null;
        testCode = null;
        errorLines = null;
    }

    public void setMutantCode(String mutantCode) {
        this.mutantCode = mutantCode;
    }

    public void setTestCode(String testCode) {
        this.testCode = testCode;
    }

    public void setErrorLines(List<Integer> errorLines) {
        this.errorLines = errorLines;
    }

    public void setSelectedLine(int selectedLine) {
        this.selectedLine = selectedLine;
    }

    public void clear() {
        mutantCode = null;
        testCode = null;
        errorLines = null;
        selectedLine = null;
    }

    public void clearButKeepMutant() {
        testCode = null;
        errorLines = null;
        selectedLine = null;
    }

    // --------------------------------------------------------------------------------

    public boolean hasMutant() {
        return mutantCode != null;
    }

    public boolean hasTest() {
        return testCode != null;
    }

    public boolean hasErrorLines() {
        return errorLines != null;
    }

    public boolean hasSelectedLine() {
        return selectedLine != null;
    }

    public String getMutantCode() {
        return mutantCode;
    }

    public String getTestCode() {
        return testCode;
    }

    public List<Integer> getErrorLines() {
        return errorLines;
    }

    public Integer getSelectedLine() {
        return selectedLine;
    }
}
