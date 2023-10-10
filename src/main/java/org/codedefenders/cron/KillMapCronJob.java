/*
 * Copyright (C) 2023 Code Defenders contributors
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

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.execution.KillMap.KillMapJob;
import org.codedefenders.execution.KillMapService;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("killMapCronJob")
@ApplicationScoped
public class KillMapCronJob extends FixedDelayCronJob {
    private static final Logger logger = LoggerFactory.getLogger(KillMapCronJob.class);

    private final KillMapService killMapService;


    private boolean isEnabled;
    private KillMapJob currentJob;


    @Inject
    public KillMapCronJob(KillMapService killMapService) {
        super(20, 10, TimeUnit.SECONDS);
        this.killMapService = killMapService;

        AdminSystemSettings.SettingsDTO setting = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.AUTOMATIC_KILLMAP_COMPUTATION);
        if (setting.getType() != AdminSystemSettings.SETTING_TYPE.BOOL_VALUE) {
            throw new RuntimeException("This setting should be a boolean!!");
        }
        isEnabled = setting.getBoolValue();
        logger.debug("Set enabled={} from database.", isEnabled);
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
     *
     * @return The currently processed job.
     */
    @Nullable
    public KillMapJob getCurrentJob() {
        return currentJob;
    }

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
                        AbstractGame game = GameDAO.getGame(theJob.getId());

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
                case CLASSROOM:
                    try {
                        killMapService.forClassroom(theJob.getId());
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
