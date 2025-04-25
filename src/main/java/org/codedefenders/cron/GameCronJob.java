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
