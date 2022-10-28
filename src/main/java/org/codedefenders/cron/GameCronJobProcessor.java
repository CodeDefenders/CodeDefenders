package org.codedefenders.cron;

import org.codedefenders.service.game.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This processor is executed regular to handle tasks asynchronous.
 * Its only task (for now) is to regularly detect and close expired games.
 */
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
