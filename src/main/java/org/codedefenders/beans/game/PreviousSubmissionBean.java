package org.codedefenders.beans.game;

import javax.annotation.ManagedBean;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.List;

/**
 * <p>Saves the previous mutant / test submission so the code of the previous submission can be displayed. </p>
 * <p>Bean Name: {@code previousSubmission}</p>
 */
@ManagedBean
@SessionScoped
// TODO: Put error message here and show it somewhere different than the messages?
public class PreviousSubmissionBean implements Serializable {
    /**
     * The
     */
    private String mutantCode;
    private String testCode;
    private List<Integer> errorLines;

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

    // public void clearMutant() {
    //     mutantCode = null;
    // }

    // public void clearTest() {
    //     testCode = null;
    // }

    // public void clearErrorLines() {
    //     errorLines = null;
    // }

    public void clear() {
        mutantCode = null;
        testCode = null;
        errorLines = null;
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

    public String getMutantCode() {
        return mutantCode;
    }

    public String getTestCode() {
        return testCode;
    }

    public List<Integer> getErrorLines() {
        return errorLines;
    }
}
