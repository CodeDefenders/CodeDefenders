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

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

/**
 * This is taken from:
 *
 * https://rmannibucau.wordpress.com/2016/02/29/cdi-replace-the-configuration-by-a-register-pattern/
 *
 * TODO Maybe we can replace this with a ServletListener?
 */
@ApplicationScoped
public class ThreadPoolManager {
    private final AtomicBoolean running = new AtomicBoolean();
    private final Map<String, ThreadPoolModel> models = new HashMap<>();
    private final ConcurrentMap<String, ThreadPoolExecutor> pools = new ConcurrentHashMap<>();

    @Inject
    private Event<ThreadPoolManager> registrationEvent;

    void init(@Observes @Initialized(ApplicationScoped.class) final Object start) {
        registrationEvent.fire(this); // note: doesn't prevent to support some default file configuration if desired
        running.set(true);
    }

    @PreDestroy
    void destroy() {
        running.set(false);
        pools.forEach((n, p) -> models.get(n).destroy(p));
    }

    @Produces
    @ThreadPool("")
    public ThreadPoolExecutor getOrCreatePool(final InjectionPoint ip) {
        if (!running.get()) {
            throw new IllegalStateException("Pool not available");
        }
        return pools.computeIfAbsent(ip.getAnnotated().getAnnotation(ThreadPool.class).value(), name ->
            ofNullable(models.get(name)).map(ThreadPoolModel::create).orElseThrow(() -> new IllegalArgumentException("No pool '" + name + "' defined.")));
    }

    /**
     * @param name name of the pool (only mandatory configuration).
     * @return a pool builder to customize defaults of the pool. Think to call add() to ensure it is registered.
     */
    public ThreadPoolBuilder register(final String name) {
        System.out.println("ThreadPoolManager.register() " + name);
        return new ThreadPoolBuilder(this, name);
    }

    public static class ThreadPoolBuilder {
        private final String name;
        private final ThreadPoolManager registration;

        private int core = 3;
        private int max = 10;
        private long shutdownTime = 0;
        private TimeUnit shutdownTimeUnit = MILLISECONDS;
        private long keepAliveTime = 0;
        private TimeUnit keepAliveTimeUnit = MILLISECONDS;
        private BlockingQueue<Runnable> workQueue;
        private ThreadFactory threadFactory;
        private RejectedExecutionHandler handler;

        private ThreadPoolBuilder(final ThreadPoolManager registration, final String name) {
            this.registration = registration;
            this.name = name;
        }

        public ThreadPoolBuilder withCore(final int core) {
            this.core = core;
            return this;
        }

        public ThreadPoolBuilder withMax(final int max) {
            this.max = max;
            return this;
        }

        public ThreadPoolBuilder withKeepAliveTime(final long keepAliveTime, final TimeUnit unit) {
            this.keepAliveTime = keepAliveTime;
            this.keepAliveTimeUnit = unit;
            return this;
        }

        public ThreadPoolBuilder withShutdownTime(final long time, final TimeUnit unit) {
            this.shutdownTime = keepAliveTime;
            this.shutdownTimeUnit = unit;
            return this;
        }

        public ThreadPoolBuilder withWorkQueue(final BlockingQueue<Runnable> workQueue) {
            this.workQueue = workQueue;
            return this;
        }

        public ThreadPoolBuilder withThreadFactory(final ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        public ThreadPoolBuilder withRejectedExecutionHandler(final RejectedExecutionHandler handler) {
            this.handler = handler;
            return this;
        }

        public ThreadPoolManager add() {
            this.registration.models.put(name, new ThreadPoolModel(core, max, keepAliveTime, keepAliveTimeUnit, workQueue, threadFactory, handler, shutdownTime, shutdownTimeUnit));
            return registration;
        }
    }

    private static class ThreadPoolModel {
        private final int core;
        private final int max;
        private final long keepAliveTime;
        private final TimeUnit keepAliveTimeUnit;
        private final BlockingQueue<Runnable> workQueue;
        private final ThreadFactory threadFactory;
        private final RejectedExecutionHandler handler;
        private final long shutdownTime;
        private final TimeUnit shutdownTimeUnit;

        private ThreadPoolModel(final int core, final int max,
                                final long keepAliveTime, final TimeUnit keepAliveTimeUnit,
                                final BlockingQueue<Runnable> workQueue,
                                final ThreadFactory threadFactory,
                                final RejectedExecutionHandler handler,
                                final long shutdownTime, final TimeUnit shutdownTimeUnit) {
            this.core = core;
            this.max = max;
            this.keepAliveTime = keepAliveTime;
            this.keepAliveTimeUnit = keepAliveTimeUnit == null ? MILLISECONDS : keepAliveTimeUnit;
            this.shutdownTime = shutdownTime;
            this.shutdownTimeUnit = shutdownTimeUnit == null ? MILLISECONDS : shutdownTimeUnit;
            this.workQueue = workQueue == null ? new LinkedBlockingDeque<>() : workQueue;
            this.threadFactory = threadFactory == null ? Executors.defaultThreadFactory() : threadFactory;
            this.handler = handler == null ? new ThreadPoolExecutor.AbortPolicy() : handler;
        }

        private ThreadPoolExecutor create() {
            return new ThreadPoolExecutor(core, max, keepAliveTime, keepAliveTimeUnit, workQueue, threadFactory, handler);
        }

        private void destroy(final ThreadPoolExecutor executor) {
            executor.shutdown();
            if (shutdownTime > 0) {
                try {
                    executor.awaitTermination(shutdownTime, shutdownTimeUnit);
                } catch (final InterruptedException e) {
                    Thread.interrupted();
                }
            }
        }
    }
}
