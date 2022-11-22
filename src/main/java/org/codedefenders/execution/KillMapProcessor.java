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
package org.codedefenders.execution;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class setups the thread pool to process games killmaps asynchronously.
 * It reads from the DB KillMapJobs the id of the games waiting for their
 * killmap to be computed, and processes them one at the time. Results are then
 * stored to killmap, and the job is removed from the database
 *
 * @author gambi
 *
 */
@ApplicationScoped
public class KillMapProcessor {
    private static final Logger logger = LoggerFactory.getLogger(KillMapProcessor.class);

    @Inject
    private KillMapService killMapService;

    private ScheduledExecutorService executor;

    // Do we need those to be configurable ? Not until further notice !
    private static final int INITIAL_DELAY_VALUE = 20;
    private static final int EXECUTION_DELAY_VALUE = 10;
    private static final TimeUnit EXECUTION_DELAY_UNIT = TimeUnit.SECONDS;


    // This is reset every time we re-deploy the app.
    // We make it easy: instead of stopping and restarting the executor, we
    // simply skip the job if the processor is disabled
    private boolean isEnabled = true;

    private KillMapJob currentJob = null;

    private class Processor implements Runnable {

        @Override
        public void run() {
            if (!isEnabled) {
                return;
            }
            // Retrieve all of them to have a partial count, but execute only
            // the first
            List<KillMapJob> gamesToProcess = KillmapDAO.getPendingJobs();
            if (gamesToProcess.isEmpty()) {
                logger.debug("No killmap computation to process");
            } else {

                KillMapJob theJob = gamesToProcess.get(0);
                currentJob = theJob;

                switch (theJob.getType()) {
                    case CLASS:
                        try {
                            killMapService.forClass(theJob.getId());
                        } catch (Throwable e) {
                            logger.warn("Killmap computation failed!", e);
                        } finally {
                            // If the job fails and we leave it in the database,
                            // we risk to create an infinite loop. So we remove it every time !
                            KillmapDAO.removeJob(theJob);
                        }
                        break;
                    case GAME:
                        try {
                            MultiplayerGame game = MultiplayerGameDAO.getMultiplayerGame(theJob.getId());

                            assert game.getId() == theJob.getId();

                            logger.info("Computing killmap for game " + game.getId());
                            killMapService.forGame(game);
                            logger.info("Killmap for game " + game.getId() + ". Remove job from DB");
                            // At this point we can remove the job from the DB
                        } catch (Throwable e) {
                            logger.warn("Killmap computation failed!", e);
                        } finally {
                            // If the job fails and we leave it in the database,
                            // we risk to create an infinite loop. So we remove it every time !
                            KillmapDAO.removeJob(theJob);
                        }
                        break;
                    default:
                        // ignored
                }

                currentJob = null;
            }
        }
    }

    public KillMapProcessor() {
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * Returns the job that is currently processed.
     * Can be {@code null} even when killmap processing is enabled.
     * @return The currently processed job.
     */
    public KillMapJob getCurrentJob() {
        return currentJob;
    }

    /**
     * Return the ID of the games or classes for which there's a pending killmap
     * computation.
     */
    public List<KillMapJob> getPendingJobs() {
        return KillmapDAO.getPendingJobs();
    }

    public void start() {
        /*
         * Note: the size of the thread pool for the moment is fixed to 1. If
         * this thread dies for whatever reason the killmap computation will die
         * as well
         */

        // Read the setting for this service from the DB
        for (AdminSystemSettings.SettingsDTO setting : AdminDAO.getSystemSettings()) {
            if (!AdminSystemSettings.SETTING_NAME.AUTOMATIC_KILLMAP_COMPUTATION.equals(setting.getName())) {
                continue;
            }
            // Avoid future problems
            if (AdminSystemSettings.SETTING_TYPE.BOOL_VALUE.equals(setting.getType())) {
                logger.debug("Initial conf from the DB:" + setting.getBoolValue());
                this.setEnabled(setting.getBoolValue());
                break;
            }
        }

        executor = Executors.newScheduledThreadPool(1);
        logger.debug("KillMapProcessor Started ");
        executor.scheduleWithFixedDelay(new Processor(), INITIAL_DELAY_VALUE, EXECUTION_DELAY_VALUE,
                EXECUTION_DELAY_UNIT);
    }

    @PreDestroy
    public void shutdown() {
        try {
            logger.info("KillMapProcessor Shutting down");
            // Cancel pending jobs
            executor.shutdownNow();
            executor.awaitTermination(20, TimeUnit.SECONDS);
            logger.info("KillMapProcessor Shut down");
        } catch (InterruptedException e) {
            logger.warn("KillMapProcessor Shutdown interrupted", e);
        }
    }

    /**
     * Represents a job for computing a killmap.
     */
    public static class KillMapJob {
        private KillMap.KillMapType type;
        private Integer id;

        public KillMapJob(KillMap.KillMapType type, Integer id) {
            this.type = type;
            this.id = id;
        }

        public KillMap.KillMapType getType() {
            return type;
        }

        public Integer getId() {
            return id;
        }
    }
}
