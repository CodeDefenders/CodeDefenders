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

import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.model.KeyMap;
import org.codedefenders.model.User;
import org.codedefenders.model.UserInfo;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.auth.LoginManager;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This {@link HttpServlet} handles requests for managing the currently logged in {@link User}.
 *
 * This functionality may be disabled, e.g. in a class room setting. See {@link #checkEnabled()}.
 * <p>
 * Serves on path: {@code /profile}.
 * @see org.codedefenders.util.Paths#USER_PROFILE
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
public class UserProfileManager extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(UserProfileManager.class);

    private static final String DELETED_USER_NAME = "DELETED";
    private static final String DELETED_USER_EMAIL = "%s@deleted-code-defenders";
    private static final String DELETED_USER_PASSWORD = "DELETED";

    /**
     * Checks whether users can view and update their profile information.
     *
     * @return {@code true} when users can access their profile, {@code false} otherwise.
     */
    public static boolean checkEnabled() {
        // please, in the name of the lord, can we change the way system settings are implemented?
        return AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.ALLOW_USER_PROFILE).getBoolValue();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!checkEnabled()) {
            // Send users to the home page
            response.sendRedirect(ServletUtils.getBaseURL(request));
            return;
        }
        final int userId = ServletUtils.userId(request);
        final Optional<UserInfo> requestingUserInfo = Optional.ofNullable(AdminDAO.getUsersInfo(userId));
        if (!requestingUserInfo.isPresent()) {
            response.sendRedirect(request.getContextPath());
            return;
        }
        request.setAttribute("userProfileInfo", requestingUserInfo.get());

        request.getRequestDispatcher(Constants.USER_PROFILE_JSP).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!checkEnabled()) {
            // Send users to the home page
            response.sendRedirect(request.getContextPath());
            return;
        }

        final HttpSession session = request.getSession();
        final ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);
        final int userId = ServletUtils.userId(request);

        String responsePath = request.getContextPath() + Paths.USER_PROFILE;

        final String formType = ServletUtils.formType(request);
        switch (formType) {
            case "updateKeyMap": {
                final String parameter = ServletUtils.getStringParameter(request, "editorKeyMap").orElse(null);
                final KeyMap editorKeyMap = KeyMap.valueOrDefault(parameter);
                if (updateUserKeyMap(userId, editorKeyMap)) {
                    session.setAttribute("user-keymap", editorKeyMap);
                    messages.add("Successfully updated editor preference.");
                } else {
                    logger.info("Failed to update editor preference for user {}.", userId);
                    messages.add("Failed to update editor preference.");
                }
                Redirect.redirectBack(request, response);
                return;
            }
            case "updateProfile": {
                final Optional<String> email = ServletUtils.getStringParameter(request, "updatedEmail");
                final Optional<String> password = ServletUtils.getStringParameter(request, "updatedPassword");
                boolean allowContact = ServletUtils.parameterThenOrOther(request, "allowContact", true, false);
                final boolean success = updateUserInformation(userId, email, password, allowContact);
                if (success) {
                    messages.add("Successfully updated profile information.");
                } else {
                    logger.info("Failed to update profile information for user {}.", userId);
                    messages.add("Failed to update profile information. Please contact the page administrator.");
                }
                response.sendRedirect(responsePath);
                return;
            }
            case "deleteAccount": {
                // Does not actually delete the account but pseudomizes it
                final boolean success = removeUserInformation(userId);
                if (success) {
                    logger.info("User {} successfully set themselves as inactive.", userId);
                    messages.add("You successfully deleted your account. Sad to see you go. :(");
                } else {
                    logger.info("Failed to set user {} as inactive.", userId);
                    messages.add("Failed to set your account as inactive. Please contact the page administrator.");
                }
                response.sendRedirect(responsePath);
                return;
            }
            default:
                logger.error("Action {" + formType + "} not recognised.");
                response.sendRedirect(responsePath);
        }
    }

    private boolean updateUserKeyMap(int userId, KeyMap editorKeyMap) {
        final User user = UserDAO.getUserById(userId);
        if (user == null) {
            return false;
        }
        user.setKeyMap(editorKeyMap);
        return user.update();
    }

    private boolean updateUserInformation(int userId, Optional<String> email, Optional<String> password, boolean allowContact) {
        final User user = UserDAO.getUserById(userId);
        if (user == null) {
            return false;
        }
        if (password.isPresent()) {
            if (!LoginManager.validPassword(password.get())) {
                return false;
            }
            user.setEncodedPassword(User.encodePassword(password.get()));
        }
        email.ifPresent(user::setEmail);
        user.setAllowContact(allowContact);

        return user.update();
    }

    private boolean removeUserInformation(int userId) {
        final User user = UserDAO.getUserById(userId);
        if (user == null) {
            return false;
        }
        user.setActive(false);
        user.setUsername(DELETED_USER_NAME);
        user.setEmail(String.format(DELETED_USER_EMAIL, UUID.randomUUID()));
        user.setEncodedPassword(DELETED_USER_PASSWORD);
        return user.update();
    }
}
