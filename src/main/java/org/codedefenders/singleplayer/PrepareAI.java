package org.codedefenders.singleplayer;

import org.apache.commons.io.FileUtils;
import org.codedefenders.*;
import org.codedefenders.singleplayer.automated.attacker.AiAttacker;
import org.codedefenders.singleplayer.automated.attacker.MajorMaker;
import org.codedefenders.singleplayer.automated.defender.AiDefender;
import org.codedefenders.singleplayer.automated.defender.EvoSuiteMaker;
import org.codedefenders.util.DatabaseAccess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.codedefenders.Constants.AI_DIR;
import static org.codedefenders.Constants.F_SEP;

public class PrepareAI {

    public PrepareAI() {

    }

    public static boolean createTestsAndMutants(int classId) {

        AIDummyGame dummyGame = new AIDummyGame(classId);
        dummyGame.insert();
        dummyGame.addPlayer(AiAttacker.ID, Role.ATTACKER);
        dummyGame.addPlayer(AiDefender.ID, Role.DEFENDER);

        GameClass cut = DatabaseAccess.getClassForKey("Class_ID", classId);

        //Generate tests.
        EvoSuiteMaker esMake = new EvoSuiteMaker(classId, dummyGame);
        try {
            esMake.makeSuite();
        } catch (Exception e) {
            e.printStackTrace();
            killAi(cut);
            return false;
        }

        //Make the suite include timeouts, to prevent infinite looping
        //when checking equivalence against a player's mutants.
        if (esMake.addTimeoutsToSuite(4000)) {
            //Compile the modified suite.
            if (!AntRunner.compileGenTestSuite(cut)) {
                killAi(cut);
                return false; //Failed
            }
        } else {
            killAi(cut);
            return false; //Failed
        }

        //Generate mutants.
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
                //Find if mutant killed by test.
                if (AntRunner.testKillsMutant(m, t)) {
                    m.incrementTimesKilledAi();
                    t.incrementAiMutantsKilled();
                }
            }
        }

        //Store kill counts to SQL.
        for (Test t : tests) {
            t.update();
        }
        for (Mutant m : mutants) {
            m.update();
        }

        DatabaseAccess.setAiPrepared(cut); //Mark class as being AI prepared.
        dummyGame.update();
        DatabaseAccess.setGameAsAIDummy(dummyGame.getId()); //Mark dummy game as a dummy game.

        if (!isPrepared(cut)) {
            //SQL has not been updated correctly, should discard everything and fail.
            killAi(cut);
            return false;
        }
        return true; //Succeeded
    }

    /**
     * Delete all generated AI files for a CUT.
     * Should be used if generation fails.
     * Mutants and tests are deleted separately, as one can be created without the other.
     */
    private static void killAi(GameClass c) {
        try {
            File tDir = new File(AI_DIR + F_SEP + "tests" + F_SEP + c.getAlias());
            FileUtils.deleteDirectory(tDir);
        } catch (IOException e) {
            //Deleting mutants failed.
            e.printStackTrace();
        }
        try {
            File mDir = new File(AI_DIR + F_SEP + "mutants" + F_SEP + c.getAlias());
            FileUtils.deleteDirectory(mDir);
        } catch (IOException e) {
            //Deleting mutants failed.
            e.printStackTrace();
        }
    }

    /**
     * Select an index of an arraylist, with a bias to earlier or later values.
     *
     * @param length Number of indexes in the arraylist.
     * @param bias   The bias power to use. >1 biases towards earlier indexes, <1 biases towards later indexes.
     * @return the resulting index.
     */
    public static int biasedSelection(int length, double bias) {

        //Generate a random number biased towards smaller or larger values.
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
