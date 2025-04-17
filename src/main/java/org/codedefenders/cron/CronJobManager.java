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
package org.codedefenders.cron;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.codedefenders.util.concurrent.ExecutorServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This classes manages {@link FixedDelayCronJob}s. It provides an interface to start and stop its execution.
 * The cron jobs will be retrieved once via CDI and executed in a regular time interval.
 */
@ApplicationScoped
public class CronJobManager {
    private static final Logger logger = LoggerFactory.getLogger(CronJobManager.class);

    private static final int AWAIT_SHUTDOWN_TIME = 20; // wait 20 seconds for tasks to complete before shutting down
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    private final ScheduledExecutorService executor;

    private final Instance<FixedDelayCronJob> cronJobs;

    @Inject
    public CronJobManager(ExecutorServiceProvider executorServiceProvider, @Any Instance<FixedDelayCronJob> cronJobs) {
        executor = executorServiceProvider.createScheduledExecutorService("cronJobManager", 2);
        this.cronJobs = cronJobs;
        logger.info("Started successfully");
    }

    public void startup() {
        for (FixedDelayCronJob job : cronJobs) {
            executor.scheduleWithFixedDelay(job, job.getInitialDelay(), job.getExecutionDelay(), job.getTimeUnit());
            logger.debug("Scheduled CronJob {}", job);
        }
        logger.info("CronJobs scheduled");
    }

    @PreDestroy
    public void shutdown() {
        try {
            logger.info("Shutting down");
            executor.shutdownNow();
            boolean shutdownSuccessful = executor.awaitTermination(AWAIT_SHUTDOWN_TIME, TIME_UNIT);
            if (shutdownSuccessful) {
                logger.info("Shutdown successfully");
            } else {
                logger.warn("Shutdown timed out");
            }
        } catch (InterruptedException e) {
            logger.warn("Shutdown interrupted", e);
        }
    }
}
