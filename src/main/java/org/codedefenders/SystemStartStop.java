/**
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

import org.codedefenders.database.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SystemStartStop implements ServletContextListener,
        HttpSessionListener, HttpSessionAttributeListener {
    private static final Logger logger = LoggerFactory.getLogger(SystemStartStop.class);

    // Public constructor is required by servlet spec
    public SystemStartStop() {
    }

    // -------------------------------------------------------
    // ServletContextListener implementation
    // -------------------------------------------------------
    public void contextInitialized(ServletContextEvent sce) {
        /* This method is called when the servlet context is
         * initialized(when the Web application is deployed).
         * You can initialize servlet context related data here.
         */
        ConnectionPool.instance();
        logger.info("Code Defenders started successfully.");
    }

    public void contextDestroyed(ServletContextEvent sce) {
        /* This method is invoked when the Servlet Context
         * (the Web application) is undeployed or
         * Application Server shuts down.
         */
        ConnectionPool.instance().closeDBConnections();
        logger.info("Code Defenders shut down successfully.");
    }

    // -------------------------------------------------------
    // HttpSessionListener implementation
    // -------------------------------------------------------
    public void sessionCreated(HttpSessionEvent se) {
        // Session is created. */
    }

    public void sessionDestroyed(HttpSessionEvent se) {
        // Session is destroyed.
    }

    // -------------------------------------------------------
    // HttpSessionAttributeListener implementation
    // -------------------------------------------------------
    public void attributeAdded(HttpSessionBindingEvent sbe) {
        // This method is called when an attribute is added to a session.
    }

    public void attributeRemoved(HttpSessionBindingEvent sbe) {
        // This method is called when an attribute is removed from a session.
    }

    public void attributeReplaced(HttpSessionBindingEvent sbe) {
        // This method is invoked when an attribute is replaced in a session.
    }
}
