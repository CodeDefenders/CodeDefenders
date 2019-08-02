package org.codedefenders.execution;

import java.io.File;

import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;

public interface ClassCompilerService {

    /**
     * Compiles mutant
     * @param dir Mutant directory
     * @param jFile Java source file
     * @param gameID Game identifier
     * @param cut Class under test
     * @param ownerId User who submitted mutant
     * @return A {@link Mutant} object
     */
    Mutant compileMutant(File dir, String jFile, int gameID, GameClass cut, int ownerId);

    /**
     * Compiles test
     * @param dir Test directory
     * @param jFile Java source file
     * @param gameID Game identifier
     * @param cut Class under test
     * @param ownerId Player who submitted test
     * @return A {@link Test} object
     */
    Test compileTest(File dir, String jFile, int gameID, GameClass cut, int ownerId);

    /**
     * Compiles CUT
     *
     * @param cut Class under test
     * @return The path to the compiled CUT
     */
    String compileCUT(GameClass cut) throws CompileException;

    /**
     * Compiles generated test suite
     * @param cut Class under test
     */
    boolean compileGenTestSuite(final GameClass cut);

}
