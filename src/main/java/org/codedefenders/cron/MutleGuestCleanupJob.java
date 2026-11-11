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

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.codedefenders.arcade.mutle.persistence.MutleRepository;
import org.codedefenders.arcade.mutle.service.MutleGuestLimits;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Retains guest games for seven days; account games and shared fixture files are never deleted. */
@ApplicationScoped
public class MutleGuestCleanupJob extends FixedDelayCronJob {
    private static final Logger logger = LoggerFactory.getLogger(MutleGuestCleanupJob.class);
    private final MutleRepository repository;
    private final TransactionManager transactions;
    private final Configuration config;

    @Inject
    public MutleGuestCleanupJob(MutleRepository repository, TransactionManager transactions, Configuration config) {
        super(1, 10, TimeUnit.MINUTES);
        this.repository = repository;
        this.transactions = transactions;
        this.config = config;
    }

    @Override
    public void run() {
        Instant cutoff = Instant.now().minus(MutleGuestLimits.RETENTION);
        try {
            for (int gameId : repository.expiredGuestGames(cutoff)) {
                try {
                    transactions.executeInTransaction(transaction -> {
                        if (!repository.deleteExpiredGuestGame(gameId, cutoff)) {
                            return;
                        }
                        // Delete before commit so a filesystem failure keeps the DB row available for retry.
                        deleteTests(config.getTestsDir().toPath(), gameId);
                        transaction.commit();
                    });
                } catch (Exception e) {
                    logger.error("Could not clean up expired Mutle guest game {}", gameId, e);
                }
            }
        } catch (Exception e) {
            logger.error("Could not select expired Mutle guest games", e);
        }
    }

    static void deleteTests(Path testsRoot, int gameId) throws IOException {
        if (gameId < 1) {
            throw new IllegalArgumentException("Invalid guest game ID");
        }
        FileUtils.deleteDirectory(testsRoot.resolve("mutle").resolve(String.valueOf(gameId)).toFile());
    }
}
