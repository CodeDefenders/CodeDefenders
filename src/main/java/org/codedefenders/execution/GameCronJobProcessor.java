package org.codedefenders.execution;

import org.codedefenders.database.ConnectionFactory;
import org.codedefenders.util.CDIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameCronJobProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(GameCronJobProcessor.class);

    private final ConnectionFactory connectionFactory;

    GameCronJobProcessor() {
        connectionFactory = CDIUtil.getBeanFromCDI(ConnectionFactory.class);
        logger.info("GameCronJobProcessor initialized ");
    }

    @Override
    public void run() {
        process();
    }

    public static void process() {
        logger.info("GameCronJobProcessor executed ");
    }
}
