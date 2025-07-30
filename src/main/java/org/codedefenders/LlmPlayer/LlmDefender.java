/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
package org.codedefenders.LlmPlayer;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.control.RequestContextController;
import jakarta.enterprise.inject.spi.CDI;

import org.codedefenders.game.GameClass;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.Player;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.MultiplayerGameRepository;
import org.codedefenders.service.LlmService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.Constants;
import org.codedefenders.util.LlmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


//TODO Melee-games
public class LlmDefender extends Player {
    private static final Logger logger = LoggerFactory.getLogger(LlmDefender.class);

    MultiplayerGame game;

    MultiplayerGameRepository gameRepo = CDIUtil.getBeanFromCDI(MultiplayerGameRepository.class);
    LlmService llmService = CDIUtil.getBeanFromCDI(LlmService.class);

    GameClass cut;
    String cutSrc;
    List<String> dependencyCode;
    String systemPrompt;
    int secondsBetweenTests = 10; //TODO irgendwo einstellen

    private final RequestContextController requestContextController;
    private final GameManagingUtils gameManagingUtils;

    public LlmDefender(int id, UserEntity user, int gameId, int points, boolean active) {
        super(id, user, gameId, points, Role.DEFENDER, active);

        requestContextController = CDI.current().select(RequestContextController.class).get();
        gameManagingUtils = CDI.current().select(GameManagingUtils.class).get();

        game = gameRepo.getMultiplayerGame(gameId);

        cut = game.getCUT();
        cutSrc = cut.getSourceCode();

        dependencyCode = cut.getDependencyCode();

        systemPrompt = """
                Write a single test for the first class of the following Java code using a maximum of 2 assertions.
                The other classes are dependencies of the first class, you don't need to test them.
                Write only the content of the test method, without including formatting, comments,
                the header or the method declaration. Use JUnit 4.""";//TODO different testing libraries
    }

    public void startRunning() {
        if (!llmService.isThreadActive(this)) {
            llmService.setPlayerActive(this, true);
            new AIDefenderThread().start();
            logger.info("Started a new llm defender thread for player {}.", getId());
        } else {
            logger.warn("Trying to start an llm defender thread for player {}, but a thread was already registed.",
                    getId());
        }
    }

    public void stopRunning() { //TODO currently not working
        llmService.setPlayerActive(this, false);
    }

    private void writeTest() {
        StringBuilder input = new StringBuilder(cutSrc);
        for (String d : dependencyCode) {
            input.append(d);
        }

        String result = llmService.getResponse(input.toString(), systemPrompt);
        String formattedResult = result.replace("```java", "").replace("```", "");//TODO in Utils-Methode
        formattedResult = LlmUtils.extractTestContentFromReply(formattedResult);
        String testTemplate = cut.getTestTemplate();
        String testSrc = testTemplate.replace(Constants.TEST_TEMPLATE_PLACEHOLDER, formattedResult);
        logger.info("AI defender generated test: {}", testSrc);

        requestContextController.activate();
        try {
            game = gameRepo.getMultiplayerGame(game.getId());
            gameManagingUtils.createBattlegroundTest(game, getUser().getId(), testSrc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            requestContextController.deactivate();
        }
    }

    private class AIDefenderThread extends Thread {
        @Override
        public void run() {
            logger.info("Starting AiDefenderThread");
            while (llmService.isThreadActive(LlmDefender.this) && gameRepo.isGameActive(game.getId())) {
                try {
                    writeTest();
                    sleep((long) secondsBetweenTests * 1000);
                } catch (InterruptedException e) {
                    logger.warn("AiDefenderThread interrupted");
                    break;
                }
            }
        }
    }
}
