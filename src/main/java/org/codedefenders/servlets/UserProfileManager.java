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
package org.codedefenders.servlets;

import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This {@link HttpServlet} handles requests for viewing the currently logged
 * in {@link UserEntity}. This functionality may be disabled, e.g. in a class room
 * setting. See {@link #checkEnabled()}.
 *
 * <p>Serves on path: {@code /profile}.
 *
 * @author <a href="https://github.com/timlg07">Tim Greller</a>
 */
@WebServlet(org.codedefenders.util.Paths.USER_PROFILE)
public class UserProfileManager extends HttpServlet {

    @Inject
    private UserRepository userRepo;

    @Inject
    private LoginBean login;

    /**
     * Checks whether users can view and update their profile information.
     *
     * @return {@code true} when users can access their profile, {@code false} otherwise.
     */
    public static boolean checkEnabled() {
        return AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.ALLOW_USER_PROFILE).getBoolValue();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!checkEnabled()) {
            // Send users to the home page
            response.sendRedirect(ServletUtils.getBaseURL(request));
            return;
        }

        if (!userRepo.getUserById(login.getUserId()).isPresent()) {
            response.sendRedirect(request.getContextPath());
            return;
        }

        request.getRequestDispatcher(Constants.USER_PROFILE_JSP).forward(request, response);
    }

}
