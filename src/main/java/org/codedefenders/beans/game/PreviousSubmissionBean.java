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
