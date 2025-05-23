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
package org.codedefenders;

import java.io.IOException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import org.codedefenders.auth.SystemSubject;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.configuration.ConfigurationValidationException;
import org.codedefenders.cron.CronJobManager;
import org.codedefenders.dependencies.DependencyProvider;
import org.codedefenders.dependencies.MavenDependencyResolver;
import org.codedefenders.game.GameStoppedEventHandlerContainer;
import org.codedefenders.instrumentation.MetricsRegistry;
import org.codedefenders.service.AchievementService;
import org.codedefenders.service.RoleService;
import org.codedefenders.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import io.prometheus.client.servlet.jakarta.exporter.MetricsServlet;
import net.bull.javamelody.ReportServlet;

@WebListener
public class SystemStartStop implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(SystemStartStop.class);

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private Configuration config;

    @Inject
    private CronJobManager cronJobManager;

    @Inject
    private AchievementService achievementService;

    @Inject
    private GameStoppedEventHandlerContainer gameStoppedEventHandlerContainer;

    @Inject
    private MetricsRegistry metricsRegistry;

    @Inject
    private RoleService roleService;

    @Inject
    private SystemSubject systemSubject;

    @Inject
    private DependencyProvider dependencyProvider;


    /**
     * This method is called when the servlet context is initialized(when
     * the Web application is deployed). You can initialize servlet context
     * related data here.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            config.validate();
        } catch (ConfigurationValidationException e) {
            logger.error(e.getMessage());
            throw new RuntimeException("Invalid configuration! Reason: " + e.getMessage(), e);
        }

        if (config.isMetricsCollectionEnabled()) {
            metricsRegistry.registerDefaultCollectors();
            sce.getServletContext().addServlet("prom", new MetricsServlet()).addMapping("/metrics");
        }
        if (config.isJavaMelodyEnabled()) {
            sce.getServletContext().addServlet("javamelody", new ReportServlet()).addMapping("/monitoring");
        }

        systemSubject.execute(
                () -> roleService.migrateAdminUsers(config.getAuthAdminRole()));
        systemSubject.execute(
                () -> roleService.doInitialAdminSetup(config.getAuthAdminUsers()));

        sce.getServletContext().setRequestCharacterEncoding("UTF-8");
        sce.getServletContext().setResponseCharacterEncoding("UTF-8");

        cronJobManager.startup();
        achievementService.registerEventHandler();
        gameStoppedEventHandlerContainer.registerEventHandler();

        try {
            FileUtils.cleanLeftoverDependencies();
        } catch (IOException e) {
            logger.error("Could not remove leftover dependencies.", e);
        }
        try {
            dependencyProvider.installDependencies();
        } catch (MavenDependencyResolver.MavenDependencyResolverException e) {
            logger.error("Could not install dependencies.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * This method is invoked when the Servlet Context (the Web application)
     * is undeployed or Application Server shuts down.
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // IMPORTANT: Do not place calls to cleanup/shutdown methods of CDI Beans here.
        // It is likely that the CDI container does not exist anymore at this point, so the cleanup is not executed
        // Instead use a @PreDestroy annotated method in the CDI Bean itself.

        // https://stackoverflow.com/questions/11872316/tomcat-guice-jdbc-memory-leak
        AbandonedConnectionCleanupThread.checkedShutdown();
        logger.info("AbandonedConnectionCleanupThread shut down.");

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        Driver d = null;
        while (drivers.hasMoreElements()) {
            try {
                d = drivers.nextElement();
                DriverManager.deregisterDriver(d);
                logger.info(String.format("Driver %s deregistered", d));
            } catch (SQLException ex) {
                logger.warn(String.format("Error deregistering driver %s", d), ex);
            }
        }

        // The ExecutorServiceProvider should be able to automatically stop the instances
    }
}
