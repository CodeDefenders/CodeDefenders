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
package org.codedefenders;

import org.codedefenders.execution.MutationTester;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.User;
import org.codedefenders.servlets.games.GameManagingUtils;
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
                    Mutant mutant = GameManagingUtils.createMutant(activeGame.getId(), activeGame.getClassId(), mutantText,
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
                    org.codedefenders.game.Test newTest = GameManagingUtils.createTest(activeGame.getId(), activeGame.getClassId(),
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
