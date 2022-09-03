package org.codedefenders.execution;

import org.codedefenders.service.game.GameService;
import org.codedefenders.util.CDIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameCronJobProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(GameCronJobProcessor.class);

    private final GameService gameService;

    GameCronJobProcessor() {
        gameService = CDIUtil.getBeanFromCDI(GameService.class);
        logger.info("GameCronJobProcessor initialized ");
    }

    @Override
    public void run() {
        logger.info("execute GameCronJobProcessor");
        gameService.closeExpiredGames();
    }

}
