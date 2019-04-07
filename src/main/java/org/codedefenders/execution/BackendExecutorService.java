package org.codedefenders.execution;

import java.io.File;

import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;

/**
 * @author gambi
 *
 */
public interface BackendExecutorService{

    /**
     * 
     * @param cut
     * @param testDir
     * @param testClassName
     * @throws Exception
     */
    public void testOriginal(GameClass cut, String testDir, String testClassName) throws Exception;
    
    /**
     * 
     * @param dir
     * @param t
     * @return
     */
    public TargetExecution testOriginal(File dir, Test t);
    
    /**
     * Executes a test against a mutant
     * 
     * @param m
     *            A {@link Mutant} object
     * @param t
     *            A {@link Test} object
     * @return A {@link TargetExecution} object
     */
    @SuppressWarnings("Duplicates")
    public TargetExecution testMutant(Mutant m, Test t);

    /**
     * 
     * @param m
     * @return
     */
    public boolean potentialEquivalent(Mutant m);

    /**
     * @param m
     * @param t
     * @return
     */
    public boolean testKillsMutant(Mutant m, Test t);
}