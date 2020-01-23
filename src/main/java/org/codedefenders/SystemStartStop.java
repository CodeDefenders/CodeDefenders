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
package org.codedefenders;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import org.codedefenders.database.ConnectionPool;
import org.codedefenders.execution.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

@WebListener
public class SystemStartStop implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(SystemStartStop.class);

    @Inject
    private ThreadPoolManager mgr;

    /**
     * This method is called when the servlet context is initialized(when
     * the Web application is deployed). You can initialize servlet context
     * related data here.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Java version: " + System.getProperty("java.version"));
        if (getJavaMajorVersion() > 9) {
            logger.error("Unsupported java version! CodeDefenders needs at most Java 9");
            throw new Error("");
        } else {
            try {
                ConnectionPool.instance();
                logger.info("Code Defenders started successfully.");
            } catch (Exception e) {
                // Fail Deployment
                throw new RuntimeException("Deployment failed. Reason: ", e);
            }
            mgr.register("test-executor").withMax(4).withCore(2).add();
        }
    }

    /**
     * This method is invoked when the Servlet Context (the Web application)
     * is undeployed or Application Server shuts down.
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            ConnectionPool.instance().closeDBConnections();
        } catch (Throwable e) {
            logger.error("Error in closing database connections", e);
        }

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

        // The ThreadPoolManager should be able to automatically stop the instances
    }

    private static int getJavaMajorVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        /* Allow these formats:
         * 1.8.0_72-ea
         * 9-ea
         * 9
         * 9.0.1
         */
        int dotPos = version.indexOf('.');
        int dashPos = version.indexOf('-');
        return Integer.parseInt(version.substring(0,
                dotPos > -1 ? dotPos : dashPos > -1 ? dashPos : 1));
    }
}
