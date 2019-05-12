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

import org.codedefenders.database.ConnectionPool;
import org.codedefenders.execution.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemStartStop implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(SystemStartStop.class);

    @Inject
    private ThreadPoolManager mgr;

    // Public constructor is required by servlet spec
    public SystemStartStop() {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        /*
         * This method is called when the servlet context is initialized(when
         * the Web application is deployed). You can initialize servlet context
         * related data here.
         */
        try {
            ConnectionPool.instance();
            logger.info("Code Defenders started successfully.");
        } catch (Exception e) {
            // Fail Deployment
            throw new RuntimeException("Deployment failed. Reason: ", e);
        }

        mgr.register("test-executor").withMax(4).withCore(2).add();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        /*
         * This method is invoked when the Servlet Context (the Web application)
         * is undeployed or Application Server shuts down.
         */
        try {
            ConnectionPool.instance().closeDBConnections();
            logger.info("Code Defenders shut down successfully.");
        } catch (Throwable e) {
            logger.error("Error in closing connections", e);
        }
        // The ThreadPoolManager should be able to automatically stop the instances  
    }
}
