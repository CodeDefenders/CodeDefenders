package org.codedefenders.beans.game;

import org.codedefenders.validation.code.CodeValidatorLevel;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;

/**
 * <p>Provides data for the mutant explanation game component.</p>
 * <p>Bean Name: {@code mutantExplanation}</p>
 */
@ManagedBean
@RequestScoped
public class MutantExplanationBean {
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
