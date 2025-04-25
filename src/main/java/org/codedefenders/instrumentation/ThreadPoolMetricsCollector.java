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
package org.codedefenders.instrumentation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.Gauge;
import io.prometheus.client.GaugeMetricFamily;

// Package private by design
class ThreadPoolMetricsCollector extends Collector {
    private static final Gauge.Child collectDuration = new Gauge.Builder()
            .name("codedefenders_collector_duration")
            .unit("seconds")
            .help("How long it took to collect metrics")
            .labelNames("collector")
            .register()
            .labels("thread_pool");

    protected final ConcurrentMap<String, ThreadPoolExecutor> children = new ConcurrentHashMap<>();


    public void addThreadPoolExecutor(String executorName, ThreadPoolExecutor executor) {
        this.children.put(executorName, executor);
    }

    public ThreadPoolExecutor removeThreadPoolExecutor(String executorName) {
        return this.children.remove(executorName);
    }


    public List<MetricFamilySamples> collect() {
        try (Gauge.Timer ignored = collectDuration.startTimer()) {
            List<String> labelNames = Arrays.asList("threadPool");

            GaugeMetricFamily corePoolSize = new GaugeMetricFamily("codedefenders_thread_pool_threads_core", "Maximum amount of threads", labelNames);
            GaugeMetricFamily poolSize = new GaugeMetricFamily("codedefenders_thread_pool_threads_active", "Number of currently active threads", labelNames);
            GaugeMetricFamily tasksActive = new GaugeMetricFamily("codedefenders_thread_pool_tasks_active", "Number of currently executing tasks", labelNames);
            CounterMetricFamily tasksSubmitted = new CounterMetricFamily("codedefenders_thread_pool_tasks_submitted_total", "Total number of submitted tasks", labelNames);
            CounterMetricFamily tasksCompleted = new CounterMetricFamily("codedefenders_thread_pool_tasks_completed_total", "Total number of completed tasks", labelNames);
            GaugeMetricFamily tasksQueued = new GaugeMetricFamily("codedefenders_thread_pool_queue_size", "Number of queued tasks", labelNames);

            List<MetricFamilySamples> mfs = Arrays.asList(corePoolSize, poolSize, tasksActive, tasksSubmitted, tasksCompleted, tasksQueued);

            for (Map.Entry<String, ThreadPoolExecutor> entry : children.entrySet()) {
                List<String> labelValues = Arrays.asList(entry.getKey());
                ThreadPoolExecutor executor = entry.getValue();

                corePoolSize.addMetric(labelValues, executor.getCorePoolSize());
                poolSize.addMetric(labelValues, executor.getPoolSize());
                tasksActive.addMetric(labelValues, executor.getActiveCount());
                tasksSubmitted.addMetric(labelValues, executor.getTaskCount());
                tasksCompleted.addMetric(labelValues, executor.getCompletedTaskCount());
                tasksQueued.addMetric(labelValues, executor.getQueue().size());
            }

            return mfs;
        }
    }
}
