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
package org.codedefenders.servlets.registration;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.I18nService;
import org.codedefenders.util.EmailUtils;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.codedefenders.validation.input.CodeDefendersValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.xnap.commons.i18n.I18n;

import static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.PASSWORD_RESET_SECRET_LIFESPAN;
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
    private I18nService i18nService;

    @Inject
    private PasswordEncoder passwordEncoder;

    // TODO Move this to Injectable configuration
    private static final int PW_RESET_SECRET_LENGTH = 20;

    private static final String CHANGE_PASSWORD_MSG = I18n.marktr("""
            Hello {0}!

            Change your password here: {1}
            This link is only valid for {2} hours.

            Greetings, your Code Defenders team""").stripIndent();

    private static final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        CodeDefendersValidator validator = new CodeDefendersValidator();

        String formType = request.getParameter("formType");

        switch (formType) {
            case "resetPassword":
                String email = request.getParameter("accountEmail");
                String username = request.getParameter("accountUsername");
                Optional<UserEntity> userOpt = userRepo.getUserByEmail(email);
                if (userOpt.isEmpty()
                        || !userOpt.get().getUsername().equals(username)
                        || !userOpt.get().getEmail().equalsIgnoreCase(email)) {
                    messages.add(I18n.marktr("No user was found for this username and email. Please check if the username and email match."));
                } else {
                    var user = userOpt.get();
                    var resetPwSecret = generatePasswordResetSecret();
                    userRepo.setPasswordResetSecret(user.getId(), resetPwSecret);

                    var resetUrl =  url.getAbsoluteURLForPath(Paths.LOGIN) + "?resetPW=" + resetPwSecret;
                    var lifespan = AdminDAO.getSystemSetting(PASSWORD_RESET_SECRET_LIFESPAN).getIntValue();

                    var locale = user.getLocale() != null ? user.getLocale() : i18nService.getDefaultLocale();
                    var i18n = I18nService.getI18n(locale);
                    var message = i18n.tr(CHANGE_PASSWORD_MSG, user.getUsername(), resetUrl, lifespan);
                    var subject = i18n.tr("Code Defenders Password reset");

                    if (EmailUtils.sendEmail(user.getEmail(), subject, message)) {
                        messages.addFormatted(I18n.marktr("A password reset link has been sent to {0}"), email);
                    } else {
                        messages.add(I18n.marktr("Something went wrong. No email could be sent."));
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
                        messages.add(I18n.marktr("Password not changed. Make sure it is valid."));
                    } else if (password.equals(confirm)) {
                        Optional<UserEntity> user = userRepo.getUserById(userId.get());
                        if (user.isPresent()) {
                            user.get().setEncodedPassword(passwordEncoder.encode(password));
                            if (userRepo.update(user.get())) {
                                userRepo.setPasswordResetSecret(user.get().getId(), null);
                                responseURL = url.forPath(Paths.LOGIN);
                                messages.add(I18n.marktr("Successfully changed your password."));
                            }
                        }
                    } else {
                        messages.add(I18n.marktr("Your two password entries did not match"));
                    }
                } else {
                    messages.add(I18n.marktr("Your password reset link is not valid or has expired."));
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
