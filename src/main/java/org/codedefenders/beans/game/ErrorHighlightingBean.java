package org.codedefenders.beans.game;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;

import com.google.gson.Gson;

/**
 * <p>Provides data for the error highlighting game component.</p>
 * <p>Bean Name: {@code errorHighlighting}</p>
 */
@RequestScoped
public class ErrorHighlightingBean {
    /**
     * Selector for the div the CodeMirror container is in. Should only contain one CodeMirror instance.
     */
    private String codeDivSelector;

    /**
     * The line numbers of the errors reported by the compiler.
     */
    private List<Integer> errorLines;

    public ErrorHighlightingBean() {
        codeDivSelector = null;
        errorLines = null;
    }

    public void setCodeDivSelector(String codeDivSelector) {
        this.codeDivSelector = codeDivSelector;
    }

    public void setErrorLines(List<Integer> errorLines) {
        this.errorLines = errorLines;
    }

    // --------------------------------------------------------------------------------

    public boolean hasErrorLines() {
        return errorLines != null;
    }

    public String getCodeDivSelector() {
        return codeDivSelector;
    }

    public String getErrorLinesJSON() {
        if (errorLines == null) {
            return "[]";
        } else {
            return new Gson().toJson(errorLines);
        }
    }
}
