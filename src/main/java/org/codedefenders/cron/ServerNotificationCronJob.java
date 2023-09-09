package org.codedefenders.cron;


import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.service.AchievementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This processor is executed regular to handle tasks asynchronous.
 * Its only task (for now) is to regularly detect and close expired games.
 */
@ApplicationScoped
public class ServerNotificationCronJob extends FixedDelayCronJob {
    private static final Logger logger = LoggerFactory.getLogger(ServerNotificationCronJob.class);

    private final AchievementService achievementService;

    @Inject
    ServerNotificationCronJob(AchievementService achievementService) {
        // Execute task every 10 seconds
        super(10, 10, TimeUnit.SECONDS);
        this.achievementService = achievementService;
        logger.info("Initialized successfully");
    }

    @Override
    public void run() {
        logger.debug("send achievement notifications");
        achievementService.sendAchievementNotifications();
    }
}
