package org.codedefenders.execution;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class GameCronJobManager {

    private static final Logger logger = LoggerFactory.getLogger(GameCronJobManager.class);
    private static final int INITIAL_DELAY_VALUE = 10;
    private static final int EXECUTION_DELAY_VALUE = 60 * 5; // execute task every 5 minutes
    private static final int AWAIT_SHUTDOWN_TIME = 20; // wait 20 seconds for tasks to complete before shutting down
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    private static ScheduledExecutorService executor;

    public void startup() {
        executor = Executors.newScheduledThreadPool(1);
        logger.debug("GameCronJobManager Started ");
        executor.scheduleWithFixedDelay(
                new GameCronJobProcessor(),
                INITIAL_DELAY_VALUE,
                EXECUTION_DELAY_VALUE,
                TIME_UNIT
        );
    }

    public void shutdown() {
        try {
            logger.info("GameCronJobManager shutting down");
            executor.shutdownNow();
            boolean shutdownSuccessful = executor.awaitTermination(AWAIT_SHUTDOWN_TIME, TIME_UNIT);
            if (shutdownSuccessful) {
                logger.info("GameCronJobManager shut down");
            } else {
                logger.warn("GameCronJobManager shutdown timed out");
            }
        } catch (InterruptedException e) {
            logger.warn("GameCronJobManager Shutdown interrupted", e);
        }
    }
}
