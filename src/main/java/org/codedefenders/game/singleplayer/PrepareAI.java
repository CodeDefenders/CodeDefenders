/*
 * Copyright (C) 2016-2019 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.game.singleplayer;

import static org.codedefenders.util.Constants.AI_DIR;
import static org.codedefenders.util.Constants.F_SEP;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.io.FileUtils;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.execution.BackendExecutorService;
import org.codedefenders.execution.ClassCompilerService;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.singleplayer.automated.attacker.AiAttacker;
import org.codedefenders.game.singleplayer.automated.attacker.MajorMaker;
import org.codedefenders.game.singleplayer.automated.defender.AiDefender;
import org.codedefenders.game.singleplayer.automated.defender.EvoSuiteMaker;

public class PrepareAI {
    
//    @Inject
    private static BackendExecutorService backend;

//    @Inject
    private static ClassCompilerService classCompiler;

    static {
        InitialContext initialContext;
        try {
            initialContext = new InitialContext();
            BeanManager bm = (BeanManager) initialContext.lookup("java:comp/env/BeanManager");
            //
            Bean bean = null;
            CreationalContext ctx = null;
            //
            bean = (Bean) bm.getBeans(BackendExecutorService.class, new Annotation[0]).iterator().next();
            ctx = bm.createCreationalContext(bean);
            backend = (BackendExecutorService) bm.getReference(bean, BackendExecutorService.class, ctx);
            //
            bean = (Bean) bm.getBeans(ClassCompilerService.class, new Annotation[0]).iterator().next();
            ctx = bm.createCreationalContext(bean);
            classCompiler = (ClassCompilerService) bm.getReference(bean, ClassCompilerService.class, ctx);
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }
    public PrepareAI() {

    }

    public static boolean createTestsAndMutants(int classId) {

        AIDummyGame dummyGame = new AIDummyGame(classId);
        dummyGame.insert();
        dummyGame.addPlayer(AiAttacker.ID, Role.ATTACKER);
        dummyGame.addPlayer(AiDefender.ID, Role.DEFENDER);

        GameClass cut = GameClassDAO.getClassForId(classId);

        // Generate tests.
        EvoSuiteMaker esMake = new EvoSuiteMaker(classId, dummyGame);
        try {
            esMake.makeSuite();
        } catch (Exception e) {
            e.printStackTrace();
            killAi(cut);
            return false;
        }

        // Make the suite include timeouts, to prevent infinite looping
        // when checking equivalence against a player's mutants.
        if (esMake.addTimeoutsToSuite(4000)) {
            // Compile the modified suite.
            if (! classCompiler.compileGenTestSuite(cut)) {
                killAi(cut);
                return false; // Failed
            }
        } else {
            killAi(cut);
            return false; // Failed
        }

        // Generate mutants.
        MajorMaker mMake = new MajorMaker(classId, dummyGame);
        try {
            mMake.createMutants();
        } catch (Exception e) {
            e.printStackTrace();
            killAi(cut);
            return false;
        }

        ArrayList<Test> tests = esMake.getValidTests();
        ArrayList<Mutant> mutants = mMake.getValidMutants();

        for (Test t : tests) {
            for (Mutant m : mutants) {
                // Find if mutant killed by test.
                if (backend.testKillsMutant(m, t)) {
                    m.incrementTimesKilledAi();
                    t.incrementAiMutantsKilled();
                }
            }
        }

        // Store kill counts to SQL.
        for (Test t : tests) {
            t.update();
        }
        for (Mutant m : mutants) {
            m.update();
        }

        DatabaseAccess.setAiPrepared(cut); // Mark class as being AI prepared.
        dummyGame.update();
        DatabaseAccess.setGameAsAIDummy(dummyGame.getId()); // Mark dummy game
                                                            // as a dummy game.

        if (!isPrepared(cut)) {
            // SQL has not been updated correctly, should discard everything and
            // fail.
            killAi(cut);
            return false;
        }
        return true; // Succeeded
    }

    /**
     * Delete all generated AI files for a CUT. Should be used if generation
     * fails. Mutants and tests are deleted separately, as one can be created
     * without the other.
     */
    private static void killAi(GameClass c) {
        try {
            File tDir = new File(AI_DIR + F_SEP + "tests" + F_SEP + c.getAlias());
            FileUtils.deleteDirectory(tDir);
        } catch (IOException e) {
            // Deleting mutants failed.
            e.printStackTrace();
        }
        try {
            File mDir = new File(AI_DIR + F_SEP + "mutants" + F_SEP + c.getAlias());
            FileUtils.deleteDirectory(mDir);
        } catch (IOException e) {
            // Deleting mutants failed.
            e.printStackTrace();
        }
    }

    /**
     * Select an index of an arraylist, with a bias to earlier or later values.
     * 
     * @param length
     *            Number of indexes in the arraylist.
     * @param bias
     *            The bias power to use. >1 biases towards earlier indexes, <1
     *            biases towards later indexes.
     * @return the resulting index.
     */
    public static int biasedSelection(int length, double bias) {

        // Generate a random number biased towards smaller or larger values.
        double r = Math.pow(Math.random(), bias);

        return (int) Math.floor(r * length);
    }

    public static boolean isPrepared(GameClass cut) {
        if (DatabaseAccess.isAiPrepared(cut)) {
            return true;
        }
        return false;
    }
}
