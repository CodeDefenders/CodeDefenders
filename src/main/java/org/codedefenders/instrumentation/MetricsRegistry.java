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

package org.codedefenders.instrumentation;

import java.util.concurrent.ThreadPoolExecutor;

import jakarta.enterprise.context.ApplicationScoped;

import com.google.common.cache.Cache;
import io.prometheus.client.guava.cache.CacheMetricsCollector;
import io.prometheus.client.hotspot.DefaultExports;

/**
 * Provides additional functionality for Metrics collection.
 */
@ApplicationScoped
public class MetricsRegistry {

    private static final CacheMetricsCollector cacheMetrics = new CacheMetricsCollector().register();
    private static final ThreadPoolMetricsCollector threadPoolExecutorMetrics = new ThreadPoolMetricsCollector().register();


    public void registerDefaultCollectors() {
        DefaultExports.initialize();
    }

    public void registerGuavaCache(String name, Cache<?, ?> cache) {
        cacheMetrics.addCache(name, cache);
    }

    public void registerThreadPoolExecutor(String name, ThreadPoolExecutor executor) {
        threadPoolExecutorMetrics.addThreadPoolExecutor(name, executor);
    }
}
