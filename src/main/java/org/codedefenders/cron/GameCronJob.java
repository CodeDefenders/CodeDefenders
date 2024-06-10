package org.codedefenders.cron;

import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.service.game.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This processor is executed regular to handle tasks asynchronous.
 * Its only task (for now) is to regularly detect and close expired games.
 */
@ApplicationScoped
public class GameCronJob extends FixedDelayCronJob {
    private static final Logger logger = LoggerFactory.getLogger(GameCronJob.class);

    private final GameService gameService;

    @Inject
    GameCronJob(GameService gameService) {
        // Execute task every .5 minutes
        super(10, 30, TimeUnit.SECONDS);
        this.gameService = gameService;
        logger.info("Initialized successfully");
    }

    @Override
    public void run() {
        logger.debug("Started execution");
        try {
            gameService.closeExpiredGames();
            logger.debug("Finished execution");
        } catch (Exception e) {
            logger.error("Encountered error:", e);
        }
    }
}
