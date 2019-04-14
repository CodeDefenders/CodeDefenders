package org.codedefenders.execution;

import org.codedefenders.game.GameClass;

public interface MutantGeneratorService {

    /**
     * Generates mutant classes using Major
     * @param cut game class
     */
    public void generateMutantsFromCUT(final GameClass cut);
}
