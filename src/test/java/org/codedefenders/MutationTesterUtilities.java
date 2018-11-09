package org.codedefenders;

import org.codedefenders.execution.MutationTester;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.User;
import org.codedefenders.servlets.games.GameManager;
import org.codedefenders.util.Constants;
import org.codedefenders.validation.code.CodeValidatorException;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;

/**
 * This class supports the testing of MutationTester
 *
 * @author gambi
 */
public class MutationTesterUtilities {

    public static Runnable attack(MultiplayerGame activeGame, String mutantFile, User attacker,
                                  ArrayList<String> messages, Logger logger) throws IOException {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    String mutantText = new String(Files.readAllBytes(new File(mutantFile).toPath()),
                            Charset.defaultCharset());
                    Mutant mutant = GameManager.createMutant(activeGame.getId(), activeGame.getClassId(), mutantText,
                            attacker.getId(), Constants.MODE_BATTLEGROUND_DIR);
                    System.out.println(new Date() + " MutationTesterTest.attack() " + attacker.getId() + " with "
                            + mutant.getId());
                    MutationTester.runAllTestsOnMutant(activeGame, mutant, messages);
                    activeGame.update();
                    System.out.println(new Date() + " MutationTesterTest.attack() " + attacker.getId() + ": "
                            + messages.get(messages.size() - 1));
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }

            }
        };
    }

    public static Runnable defend(MultiplayerGame activeGame, String testFile, User defender,
                                  ArrayList<String> messages, Logger logger) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    // Compile and test original
                    String testText;
                    testText = new String(Files.readAllBytes(new File(testFile).toPath()), Charset.defaultCharset());
                    org.codedefenders.game.Test newTest = GameManager.createTest(activeGame.getId(), activeGame.getClassId(),
                            testText, defender.getId(), Constants.MODE_BATTLEGROUND_DIR);

                    System.out.println(new Date() + " MutationTesterTest.defend() " + defender.getId() + " with "
                            + newTest.getId());
                    MutationTester.runTestOnAllMultiplayerMutants(activeGame, newTest, messages);
                    activeGame.update();
                    System.out.println(new Date() + " MutationTesterTest.defend() " + defender.getId() + ": "
                            + messages.get(messages.size() - 1));
                } catch (IOException | CodeValidatorException e) {
                    logger.error(e.getMessage());
                }
            }
        };
    }

}
