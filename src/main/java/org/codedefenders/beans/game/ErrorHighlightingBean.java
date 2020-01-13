package org.codedefenders.beans.game;

import com.google.gson.Gson;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * <p>Provides data for the error highlighting game component.</p>
 * <p>Bean Name: {@code errorHighlighting}</p>
 */
@ManagedBean
@RequestScoped
public class ErrorHighlightingBean {
    private String codeDivSelector;
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

    public boolean hasCodeDivSelector() {
        return codeDivSelector != null;
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
