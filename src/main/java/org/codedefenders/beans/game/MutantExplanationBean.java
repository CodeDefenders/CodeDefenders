package org.codedefenders.beans.game;

import org.codedefenders.validation.code.CodeValidatorLevel;

import jakarta.enterprise.context.RequestScoped;

/**
 * <p>Provides data for the mutant explanation game component.</p>
 * <p>Bean Name: {@code mutantExplanation}</p>
 */
@RequestScoped
public class MutantExplanationBean {
    /**
     * The validation level to display.
     */
    private CodeValidatorLevel codeValidatorLevel;

    public MutantExplanationBean() {
        codeValidatorLevel = null;
    }

    public void setCodeValidatorLevel(CodeValidatorLevel codeValidatorLevel) {
        this.codeValidatorLevel = codeValidatorLevel;
    }

    // --------------------------------------------------------------------------------

    public CodeValidatorLevel getCodeValidatorLevel() {
        return codeValidatorLevel;
    }
}
