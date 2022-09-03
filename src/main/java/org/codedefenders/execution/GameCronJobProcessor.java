package org.codedefenders.execution;

import org.codedefenders.service.game.GameService;
import org.codedefenders.util.CDIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameCronJobProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(GameCronJobProcessor.class);

    private final GameService gameService;

    GameCronJobProcessor(GameService gameService) {
        this.gameService = gameService;
        logger.info("GameCronJobProcessor initialized ");
    }

    @Override
    public void run() {
        logger.info("execute GameCronJobProcessor");
        try {
            closeExpiredGames();
        } catch (Exception e) {
            logger.error("GameCronJobProcessor encountered error:", e);
        }
    }

    private void closeExpiredGames() {
        gameService.closeExpiredGames();
    }

}
