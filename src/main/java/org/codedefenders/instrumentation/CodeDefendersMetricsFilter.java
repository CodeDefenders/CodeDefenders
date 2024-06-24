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

import java.io.IOException;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.util.Paths;

import io.prometheus.client.Counter;
import io.prometheus.client.servlet.jakarta.filter.MetricsFilter;

@WebFilter(filterName = "metricsFilter")
public class CodeDefendersMetricsFilter extends MetricsFilter {
    private static final Counter jsessionidIgnoredRequestsCounter = Counter.build()
            .name("codedefenders_metrics_filter_jsessionid_ignored_requests")
            .help("The amount of requests our MetricsFilter did not sample because it contained a 'jsessionid' in the URL")
            .register();

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    Configuration config;

    public CodeDefendersMetricsFilter() {
        super("http_request_duration_seconds", "help", 0, null, true);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest) || !(servletResponse instanceof HttpServletResponse)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        String servletPath = ((HttpServletRequest) servletRequest).getServletPath();
        if (!config.isMetricsCollectionEnabled()
                // We do not need metrics for the static resources and the notification endpoint has to high cardinality.
                || Stream.of(Paths.STATIC_RESOURCE_PREFIXES).anyMatch(servletPath::startsWith)
                || servletPath.startsWith("/notifications/")
        ) {
            filterChain.doFilter(servletRequest, servletResponse);
            // Normally this should not happen (esp. not with setting session-config.tracking-mode=COOKIE in web.xml)
            // In the case it occurs anyway, we do not want it to show up in the metrics, but we track the number of times it happens.
        } else if (((HttpServletRequest) servletRequest).getRequestURI().matches(".*;?(jsessionid|JSESSIONID)=[0-9A-Fa-f]*")) {
            jsessionidIgnoredRequestsCounter.inc();
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            super.doFilter(servletRequest, servletResponse, filterChain);
        }
    }
}
