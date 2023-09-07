package org.codedefenders.servlets.registration;
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

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.util.EmailUtils;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.codedefenders.validation.input.CodeDefendersValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.codedefenders.servlets.admin.AdminUserManagement.DIGITS;
import static org.codedefenders.servlets.admin.AdminUserManagement.LOWER;

@WebServlet(Paths.PASSWORD)
public class PasswordServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(PasswordServlet.class);

    @Inject
    private MessagesBean messages;

    @Inject
    private UserRepository userRepo;

    @Inject
    private URLUtils url;

    @Inject
    private PasswordEncoder passwordEncoder;

    // TODO Move this to Injectable configuration
    private static final int PW_RESET_SECRET_LENGTH = 20;

    private static final String CHANGE_PASSWORD_MSG = """
            Hello %s!

            Change your password here: %s
            This link is only valid for %d hours.

            Greetings, your Code Defenders team""".stripIndent();

    private static final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        CodeDefendersValidator validator = new CodeDefendersValidator();

        String formType = request.getParameter("formType");

        switch (formType) {
            case "resetPassword":
                String email = request.getParameter("accountEmail");
                String username = request.getParameter("accountUsername");
                Optional<UserEntity> u = userRepo.getUserByEmail(email);
                if (u.isEmpty() || !u.get().getUsername().equals(username) || !u.get().getEmail().equalsIgnoreCase(email)) {
                    messages.add("No user was found for this username and email. Please check if the username and email match.");
                } else {
                    String resetPwSecret = generatePasswordResetSecret();
                    userRepo.setPasswordResetSecret(u.get().getId(), resetPwSecret);
                    String resetUrl =  url.getAbsoluteURLForPath(Paths.LOGIN) + "?resetPW=" + resetPwSecret;
                    String msg = String.format(CHANGE_PASSWORD_MSG, u.get().getUsername(), resetUrl,
                            AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.PASSWORD_RESET_SECRET_LIFESPAN)
                                    .getIntValue());
                    if (EmailUtils.sendEmail(u.get().getEmail(), "Code Defenders Password reset", msg)) {
                        messages.add("A password reset link has been sent to " + email);
                    } else {
                        messages.add("Something went wrong. No email could be sent.");
                    }
                }
                response.sendRedirect(url.forPath(Paths.LOGIN));
                break;

            case "changePassword":
                String resetPwSecret = request.getParameter("resetPwSecret");
                String confirm = request.getParameter("inputConfirmPasswordChange");
                String password = request.getParameter("inputPasswordChange");

                String responseURL = url.forPath(Paths.LOGIN) + "?resetPW=" + resetPwSecret;
                Optional<Integer> userId = userRepo.getUserIdForPasswordResetSecret(resetPwSecret);
                if (resetPwSecret != null && userId.isPresent()) {
                    if (!(validator.validPassword(password))) {
                        messages.add("Password not changed. Make sure it is valid.");
                    } else if (password.equals(confirm)) {
                        Optional<UserEntity> user = userRepo.getUserById(userId.get());
                        if (user.isPresent()) {
                            user.get().setEncodedPassword(passwordEncoder.encode(password));
                            if (userRepo.update(user.get())) {
                                userRepo.setPasswordResetSecret(user.get().getId(), null);
                                responseURL = url.forPath(Paths.LOGIN);
                                messages.add("Successfully changed your password.");
                            }
                        }
                    } else {
                        messages.add("Your two password entries did not match");
                    }
                } else {
                    messages.add("Your password reset link is not valid or has expired.");
                    responseURL = url.forPath(Paths.LOGIN);
                }
                response.sendRedirect(responseURL);
                break;
            default:
                // ignored
        }

    }

    /**
     * Password must contain MIN_PASSWORD_LENGTH to 20 alphanumeric characters, with
     * no whitespace or special character.
     */
    private static String generatePasswordResetSecret() {
        StringBuilder sb = new StringBuilder();
        char[] initialSet = LOWER;
        initialSet = ArrayUtils.addAll(initialSet, DIGITS);

        for (int i = 0; i < PW_RESET_SECRET_LENGTH; i++) {
            sb.append(initialSet[secureRandom.nextInt(initialSet.length)]);
        }
        return sb.toString();
    }
}
