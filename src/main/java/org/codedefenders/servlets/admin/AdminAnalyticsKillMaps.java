/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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
package org.codedefenders.servlets.admin;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.execution.KillMapProcessor;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminAnalyticsKillMaps extends HttpServlet {
    private static final long serialVersionUID = 4237104046949774958L;

    private static final Logger logger = LoggerFactory.getLogger(AdminAnalyticsKillMaps.class);

    private final static String DEFAULT_MESSAGE = "Settings saved.";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.getRequestDispatcher(Constants.ADMIN_ANALYTICS_KILLMAP_JSP).forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Validate user requests
        // TODO This shall be a call done by an admin user.
        // But I guess there's no way to check this...
        HttpSession session = request.getSession();
        ArrayList<String> messages = new ArrayList<String>();
        // Get their user id from the session.
        int currentUserID = (Integer) session.getAttribute("uid");

        String valueString = request
                .getParameter(AdminSystemSettings.SETTING_NAME.AUTOMATIC_KILLMAP_COMPUTATION.name());

        ServletContext context = getServletContext();
        KillMapProcessor killMapProcessor = (KillMapProcessor) context.getAttribute(KillMapProcessor.NAME);
        if (valueString != null && !killMapProcessor.isEnabled()) {
            // This means enable
            logger.info("User {} enabled Killmap Processor", currentUserID);
            killMapProcessor.setEnabled(true);
            /*
             * Store the setting into the DB. We use the code which is already
             * in place under the assumption that we do not need to pass the
             * entire set of configurations to update one entry
             */
            (new AdminSystemSettings()).updateSystemSettings(request, messages);

        } else if (valueString == null && killMapProcessor.isEnabled()) {
            // This means disable
            logger.info("User {} disabled Killmap Processor", currentUserID);
            killMapProcessor.setEnabled(false);
            // Store the setting into the DB. We use the code which is already
            // in place
            (new AdminSystemSettings()).updateSystemSettings(request, messages);

        } else {
            logger.debug("Invalid request from user {}", currentUserID);
        }
        session.setAttribute("messages", messages);

        // Show again the same page as rendered by the JSP
        request.getRequestDispatcher(Constants.ADMIN_ANALYTICS_KILLMAP_JSP).forward(request, response);

    }
}