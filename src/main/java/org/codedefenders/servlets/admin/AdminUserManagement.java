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
package org.codedefenders.servlets.admin;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.dto.User;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.util.EmailUtils;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.codedefenders.validation.input.CodeDefendersValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED;

/**
 * This {@link HttpServlet} handles admin requests for managing {@link UserEntity Users}.
 *
 * <p>Serves on path: {@code /admin/users}.
 */
@WebServlet(org.codedefenders.util.Paths.ADMIN_USERS)
public class AdminUserManagement extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminUserManagement.class);

    static final String USER_NAME_LIST_DELIMITER = "[\\r\\n]+";

    @Inject
    private MessagesBean messages;

    @Inject
    private UserRepository userRepo;

    @Inject
    private UserService userService;

    @Inject
    private URLUtils url;

    @Inject
    private PasswordEncoder passwordEncoder;

    public static final char[] LOWER = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    public static final char[] DIGITS = "0123456789".toCharArray();
    private static final char[] PUNCTUATION = "!@#$%&*()_+-=[]|,./?><".toCharArray();
    private static final String NEW_ACCOUNT_MSG = """
            Welcome to Code Defenders!

            An account has been created for you with Username %s and Password %s.
            You can log in at %s.

            Happy coding!""".stripIndent();
    private static final String EMAIL_NOT_SPECIFIED_DOMAIN = "@NOT.SPECIFIED";
    private static final String PASSWORD_RESET_MSG = """
            %s,

            your password has been reset to %s
            Please change it at your next convenience.""".stripIndent();

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        User user = null;
        String editUser = request.getParameter("editUser");
        if (editUser != null && editUser.length() > 0 && StringUtils.isNumeric(editUser)) {
            user = userService.getUserById(Integer.parseInt(editUser)).orElse(null);
        }

        request.setAttribute("editedUser", user);

        request.getRequestDispatcher(Constants.ADMIN_USER_JSP).forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String responsePath = url.forPath(Paths.ADMIN_USERS);

        final String formType = ServletUtils.formType(request);
        switch (formType) {
            case "manageUsers": {
                // TODO(Alex): "resetPasswordButton" is not set anywhere?!
                /*
                final Optional<Integer> userToReset = ServletUtils.getIntParameter(request, "resetPasswordButton");
                if (userToReset.isPresent()) {
                    messages.add(resetUserPW(userToReset.get()));
                    break;
                }
                 */
                final Optional<Integer> userId = ServletUtils.getIntParameter(request, "setUserInactive");
                if (userId.isPresent()) {
                    final boolean success = setUserInactive(userId.get());
                    if (success) {
                        messages.add("Successfully set user with id " + userId.get() + " as inactive.");
                    } else {
                        logger.warn("Setting user as inactive failed.");
                        messages.add("Failed to set user as inactive.");
                    }
                }
                final Optional<Integer> userToEdit = ServletUtils.getIntParameter(request, "editUserInfo");
                if (userToEdit.isPresent()) {
                    responsePath = url.forPath(Paths.ADMIN_USERS)
                            + "?editUser=" + userToEdit.get();
                }
                break;
            }
            case "createUsers": {
                final Optional<String> userList = ServletUtils.getStringParameter(request, "user_name_list");
                if (userList.isEmpty()) {
                    logger.error("Creating users failed. Missing parameter 'user_name_list'");
                } else {
                    logger.info("Creating users....");
                    createUserAccounts(request, userList.get());
                    logger.info("Creating users succeeded.");
                }
                break;
            }
            case "editUser": {
                // TODO Phil 23/06/19: update 'uid' request parameter as it is the same as the
                //  'userid' from the session attributes
                final Optional<Integer> userId = ServletUtils.getIntParameter(request, "uid");
                if (userId.isEmpty()) {
                    logger.error("Editing user failed. Missing request parameter 'uid'");
                } else {
                    String newUsername = request.getParameter("name");
                    String newEmail = request.getParameter("email");
                    String password = request.getParameter("password");
                    String confirmPassword = request.getParameter("confirm_password");

                    String msg;

                    if (!password.equals(confirmPassword)) {
                        msg = "Passwords don't match";
                    } else {
                        String newPassword = null;
                        if (!password.equals("")) {
                            newPassword = password;
                        }

                        Optional<String> result = userService.updateUser(userId.get(), newUsername, newEmail, newPassword);

                        if (result.isPresent()) { // There was an error
                            responsePath = url.forPath(Paths.ADMIN_USERS)
                                    + "?editUser=" + userId.get();
                            msg = result.get();
                        } else {
                            msg = "Successfully updated info for User " + userId.get();
                        }
                    }

                    messages.add(msg);
                }
                break;
            }
            default:
                logger.error("Action {" + formType + "} not recognised.");
                break;
        }

        response.sendRedirect(responsePath);
    }

    private boolean setUserInactive(int userId) {
        final Optional<UserEntity> user = userRepo.getUserById(userId);
        if (user.isEmpty()) {
            return false;
        }
        user.get().setActive(false);
        return userRepo.update(user.get());

    }

    private void createUserAccounts(HttpServletRequest request, String userNameListString) {
        final String[] lines = userNameListString.split(USER_NAME_LIST_DELIMITER);

        final boolean sendMail = AdminDAO.getSystemSetting(EMAILS_ENABLED).getBoolValue();
        final String hostAddress = url.getAbsoluteURLForPath("/");

        for (String credentials : lines) {
            createUserAccount(credentials.trim(), sendMail, hostAddress);
        }
    }

    /**
     * Creates a user for a given string, which has to be formatted like:
     *
     * <p>{@code username,password}
     *
     * <p>{@code username,password,email}
     *
     * <p>Values can be separated by either ',' or ';'.
     */
    private void createUserAccount(String userCredentials, boolean sendMail, String hostAddress) {
        CodeDefendersValidator validator = new CodeDefendersValidator();
        // credentials have the following form: username, password, email (optional)
        final String[] credentials = userCredentials.split("[,;]+");
        if (credentials.length < 2) {
            logger.info("Failed to create user due to not enough arguments:" + credentials.length);
            messages.add("Please provide at least username and password");
            return;
        } else if (credentials.length > 3) {
            logger.info("Failed to create user due to too many arguments:" + credentials.length);
            messages.add("Please provide at maximum username,password and email");
            return;
        }

        final String username = credentials[0].trim();
        if (userRepo.getUserByName(username).isPresent()) {
            logger.info("Failed to create user. Username already in use:" + username);
            messages.add("Username '" + username + "' already in use.");
            return;
        }
        if (!validator.validUsername(username)) {
            logger.info("Failed to create user. Username invalid:" + username);
            messages.add("Username '" + username + "' invalid, user not created");
            return;
        }

        final String password = credentials[1].trim();
        if (!validator.validPassword(password)) {
            logger.info("Failed to create user. Password invalid:" + password);
            messages.add("Password for user " + username + " invalid, user not created. Please notice that only >= "
                    + AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue()
                    + " alphanumeric characters (a-z, A-Z, 0-9) without whitespaces are allowed.");
            return;
        }

        final String email;

        final boolean hasMail = credentials.length == 3;
        if (hasMail) {
            email = credentials[2].trim();
            if (userRepo.getUserByEmail(email).isPresent()) {
                logger.info("Failed to create user. Email address already in use:" + email);
                messages.add("Email '" + email + "' already in use.");
                return;
            }
            if (!validator.validEmailAddress(email)) {
                logger.info("Failed to create user. Email invalid:" + email);
                messages.add("Email for user " + username + " invalid, user not created.");
                return;
            }
        } else {
            email = username + EMAIL_NOT_SPECIFIED_DOMAIN;
        }

        final UserEntity user = new UserEntity(username, passwordEncoder.encode(password), email);
        final boolean createSuccess = userRepo.insert(user).isPresent();

        if (!createSuccess) {
            final String errorMsg = "Failed to create account for user '" + username + "'";
            logger.error(errorMsg);
            messages.add(errorMsg);
        } else {
            messages.add("Created user " + username + (hasMail ? " (" + email + ")" : ""));
            if (hasMail && sendMail && hostAddress != null) {
                final boolean mailSuccess = sendNewAccountMsg(email, username, password, hostAddress);
                if (!mailSuccess) {
                    messages.add("Could not send email to user " + username + " with email " + email);
                    logger.error("Failed to send account creation mail to user " + username + "<" + email + ">");
                } else {
                    logger.info("Successfully sent account creation mail to user " + username + "<" + email + ">");
                }
            }
        }
    }

    private boolean sendNewAccountMsg(String email, String name, String password, String hostAddr) {
        String message = String.format(NEW_ACCOUNT_MSG, name, password, hostAddr);
        return EmailUtils.sendEmail(email, "Your Code Defenders Account", message);
    }

    /*
    private String deleteUser(int uid) {
        return "Currently disabled!";
        // return (AdminDAO.deleteUser(uid) ? "Successfully deleted user " : "Error
        // trying to delete user ") + uid + "!";
    }

    private String resetUserPW(int uid) {

        String newPassword = generatePW();

        if (AdminDAO.setUserPassword(uid, UserEntity.encodePassword(newPassword))) {
            if (AdminDAO.getSystemSetting(EMAILS_ENABLED).getBoolValue()) {
                userRepo.getUserById(uid).ifPresent(user -> {
                    String msg = String.format(PASSWORD_RESET_MSG, user.getUsername(), newPassword);
                    EmailUtils.sendEmail(user.getEmail(), "Code Defenders Password reset", msg);
                });
            }
            return "User " + uid + "'s password set to: " + newPassword;
        }
        return "Could not reset password for user " + uid;
    }

    private static String generatePW() {
        int length = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue();

        StringBuilder sb = new StringBuilder();
        char[] initialSet = LOWER;

        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(initialSet[random.nextInt(initialSet.length)]);
        }
        char[] resultChars = sb.toString().toCharArray();

        List<Integer> randomInts = IntStream.range(0, length).boxed()
                .collect(Collectors.toList());
        Collections.shuffle(randomInts);

        int c = 0;
        resultChars[randomInts.get(c)] = Character.toUpperCase(resultChars[randomInts.get(c)]);
        resultChars[randomInts.get(++c)] = PUNCTUATION[random.nextInt(PUNCTUATION.length)];
        resultChars[randomInts.get(++c)] = DIGITS[random.nextInt(DIGITS.length)];

        return new String(resultChars);
    }
     */
}
