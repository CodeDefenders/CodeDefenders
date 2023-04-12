/*
 * Copyright (C) 2022 Code Defenders contributors
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
package org.codedefenders.util.concurrent;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.GuardedBy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.instrumentation.MetricsRegistry;
import org.codedefenders.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

@ApplicationScoped
public class ExecutorServiceProvider {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorServiceProvider.class);

    private static final int AWAIT_SHUTDOWN_TIME = 20; // wait 20 seconds for tasks to complete before shutting down
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    private final MetricsRegistry metricsRegistry;
    private final TransactionManager transactionManager;

    @GuardedBy("this")
    private final Map<String, ExecutorService> executorServices = new HashMap<>();


    @Inject
    public ExecutorServiceProvider(MetricsRegistry metricsRegistry, TransactionManager transactionManager) {
        this.metricsRegistry = metricsRegistry;
        this.transactionManager = transactionManager;
    }


    /**
     * @implNote <ul>
     *         <li>This uses multiple for loops, so ThreadPools can shut down in parallel and we do not try to wait for
     *         one thread-pool to finish shutting down before we start shutting down the next one.</li>
     *         <li>Implementation is based on the example given in the JavaDoc of {@link ExecutorService}</li>
     *         </ul>
     */
    @PreDestroy
    synchronized void shutdown() {
        logger.info("Shutting down ExecutorServices");

        for (Map.Entry<String, ExecutorService> entry : executorServices.entrySet()) {
            String name = entry.getKey();
            ExecutorService executor = entry.getValue();
            logger.debug("Shutting down {}", name);
            executor.shutdown(); // Disable new tasks from being submitted
        }

        try {
            // TODO(Alex): This might potential wait for 2 * AWAIT_SHUTDOWN_TIME * <Number of ExecutorServices>
            for (Map.Entry<String, ExecutorService> entry : executorServices.entrySet()) {
                String name = entry.getKey();
                ExecutorService executor = entry.getValue();
                // Wait a while for existing tasks to terminate
                if (!executor.awaitTermination(AWAIT_SHUTDOWN_TIME, TIME_UNIT)) {
                    logger.debug("Tasks from {} did not finish within time period.", name);
                    executor.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!executor.awaitTermination(AWAIT_SHUTDOWN_TIME, TIME_UNIT)) {
                        logger.error("Pool {} did not terminate", name);
                    }
                }
            }
        } catch (InterruptedException ex) {
            logger.debug("Thread got interrupted, shutdownNow all pools");
            // (Re-)Cancel if current thread also interrupted
            for (Map.Entry<String, ExecutorService> entry : executorServices.entrySet()) {
                ExecutorService executor = entry.getValue();
                executor.shutdownNow();
            }
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }


    public ScheduledExecutorService createScheduledExecutorService(final String name, int count) {
        return (ScheduledExecutorService) createExecutorService(name, count, true);
    }

    public ExecutorService createExecutorService(final String name, int count) {
        return createExecutorService(name, count, false);
    }


    private synchronized ExecutorService createExecutorService(final String name, int count, boolean scheduled) {
        if (executorServices.containsKey(name)) {
            throw new IllegalStateException("There already exists an (Scheduled)ExecutorService with the name " + name);
        }


        ThreadPoolExecutor executorService;
        if (scheduled) {
            executorService = new CodeDefendersScheduledThreadPoolExecutor(name, count, transactionManager);
        } else {
            executorService = new CodeDefendersThreadPoolExecutor(name, count, transactionManager);
        }

        metricsRegistry.registerThreadPoolExecutor(name, executorService);
        executorServices.put(name, executorService);
        return executorService;
    }

    private static class CodeDefendersThreadPoolExecutor extends ThreadPoolExecutor {
        private static final Logger logger = LoggerFactory.getLogger(CodeDefendersThreadPoolExecutor.class);

        private final TransactionManager transactionManager;

        public CodeDefendersThreadPoolExecutor(String name, int poolSize,
                                               @Nonnull TransactionManager transactionManager) {
            super(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());
            this.transactionManager = transactionManager;
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);

            try {
                transactionManager.terminateTransaction();
            } catch (SQLException e) {
                logger.error("SQLException while terminating transactions", e);
            } catch (IllegalStateException e) {
                logger.error("Terminated still running transactions", e);
            }
        }
    }

    private static class CodeDefendersScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {
        private static final Logger logger = LoggerFactory.getLogger(CodeDefendersScheduledThreadPoolExecutor.class);

        private final TransactionManager transactionManager;

        public CodeDefendersScheduledThreadPoolExecutor(String name, int corePoolSize,
                                                        @Nonnull TransactionManager transactionManager) {
            super(corePoolSize, new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());
            this.transactionManager = transactionManager;
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);

            try {
                transactionManager.terminateTransaction();
            } catch (SQLException e) {
                logger.error("SQLException while terminating transactions", e);
            } catch (IllegalStateException e) {
                logger.error("Terminated still running transactions", e);
            }
        }
    }
}
