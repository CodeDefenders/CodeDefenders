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
     * The line numbers of the errors reported by the compiler.
     */
    private List<Integer> errorLines;

    public ErrorHighlightingBean() {
        errorLines = null;
    }

    public void setErrorLines(List<Integer> errorLines) {
        this.errorLines = errorLines;
    }

    // --------------------------------------------------------------------------------

    public boolean hasErrorLines() {
        return errorLines != null;
    }

    public String getErrorLinesJSON() {
        if (errorLines == null) {
            return "[]";
        } else {
            return new Gson().toJson(errorLines);
        }
    }
}
