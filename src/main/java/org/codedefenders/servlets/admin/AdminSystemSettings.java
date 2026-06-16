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
package org.codedefenders.servlets.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Level;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.logging.LoggingConfig;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;

import static org.codedefenders.util.Paths.ADMIN_SETTINGS;

// TODO Does this enable CDI using @Property@Inject ?
@WebServlet(Paths.ADMIN_SETTINGS)
public class AdminSystemSettings extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminSystemSettings.class);

    @Inject
    private MessagesBean messages;

    @Inject
    private URLUtils url;

    @Inject
    private LoggingConfig loggingConfig;

    public static List<Level> sortedLogLevels = new ArrayList<>();
    static {
        Arrays.stream(Level.values())
                .sorted(Comparator.comparing(Level::intLevel))
                .forEach(sortedLogLevels::add);
    }

    public enum SETTING_NAME {
        SHOW_PLAYER_FEEDBACK(
                I18n.marktr("Show Player Feedback"),
                I18n.marktr("Show other player's feedback to all players in the game.")
        ),
        REGISTRATION(
                I18n.marktr("Registration"),
                I18n.marktr("Show or hide the link for user registration.")
        ),
        CLASS_UPLOAD(
                I18n.marktr("Class Upload"),
                I18n.marktr("Show or hide the link for CUT upload.")
        ),
        GAME_CREATION(
                I18n.marktr("Game Creation"),
                I18n.marktr("Show or hide the link for Game Creation.")
        ),
        GAME_JOINING(
                I18n.marktr("Game Joining"),
                I18n.marktr("Show or hide links to join/leave games.")
        ),
        REQUIRE_MAIL_VALIDATION(
                I18n.marktr("Require Mail Validation"),
                I18n.marktr("Require a validated email address for login.")
        ),
        MIN_PASSWORD_LENGTH(
                I18n.marktr("Min Password Length"),
                I18n.marktr("Minimum password length, also length of generated passwords. Recommended: >7")
        ),
        CONNECTION_POOL_CONNECTIONS(
                I18n.marktr("Connection Pool Connections"),
                I18n.marktr(
                    """
                    NOT USED, SET THIS VIA THE CONFIGURATION.
                    Number of permanently open connections. Recommended: >20
                    Lowering this number closes the delta in connections!
                    """
                )
        ),
        CONNECTION_WAITING_TIME(
                I18n.marktr("Connection Waiting Time"),
                I18n.marktr(
                    """
                    NOT USED, SET THIS VIA THE CONFIGURATION.
                    Amount of time in ms a thread waits to be notified of newly available connections.
                    Recommended: ~5000ms
                    """
                )
        ),
        SUPPORTED_LANGUAGES(
                I18n.marktr("Supported Languages"),
                I18n.marktr(
                    """
                    Comma separated list of supported locales where full translations exist.
                    The first locale in the list will be used by default.
                    Language codes like en, de, fr are sufficient.
                    """
                )
        ),
        EMAIL_SMTP_HOST(
                I18n.marktr("Email Smtp Host"),
                I18n.marktr("SMTP host")
        ),
        EMAIL_SMTP_PORT(
                I18n.marktr("Email Smtp Port"),
                I18n.marktr("SMTP port")
        ),
        EMAIL_ADDRESS(
                I18n.marktr("Email Address"),
                I18n.marktr("System mail account")
        ),
        EMAIL_PASSWORD(
                I18n.marktr("Email Password"),
                I18n.marktr("Password for the system mail account")
        ),
        EMAIL_USERNAME(
                I18n.marktr("Email Username"),
                I18n.marktr(
                    """
                    Username for the system mail account.
                    Should usually be the same as the address. If left blank, will default to the address.
                    """
                )
        ),
        EMAILS_ENABLED(
                I18n.marktr("Emails Enabled"),
                I18n.marktr(
                """
                    Send emails from the specified account for verification,
                    resetting passwords and when the admin changes user info
                    """
                )
        ),
        PASSWORD_RESET_SECRET_LIFESPAN(
                I18n.marktr("Password Reset Secret Lifespan"),
                I18n.marktr("How long (in hours) a password reset secret is valid")
        ),
        DEBUG_MODE(
                I18n.marktr("Debug Mode"),
                I18n.marktr("Turn on certain debugging features such as detailed debug prints for javax.mail")
        ),
        AUTOMATIC_KILLMAP_COMPUTATION(
                I18n.marktr("Automatic Killmap Computation"),
                I18n.marktr("Turn on the automatic killmaps computation")
        ),
        PUBLIC_USER_PROFILE(
                I18n.marktr("Public User Profile"),
                I18n.marktr("Let users visit the profile pages of others.")
        ),
        ALLOW_PUZZLE_SECTION(
                I18n.marktr("Allow Puzzle Section"),
                I18n.marktr("Let users play the puzzles in the puzzle section.")
        ),
        FAILED_DUEL_VALIDATION_THRESHOLD(
                I18n.marktr("Failed Duel Validation Threshold"),
                I18n.marktr("The maximum number of tests that will run to validate a lost equivalence duel.")
        ),
        GAME_DURATION_MINUTES_MAX(
                I18n.marktr("Game Duration Minutes Max"),
                I18n.marktr("The maximum duration that can be set for multiplayer/melee games (in minutes).")
        ),
        GAME_DURATION_MINUTES_DEFAULT(
                I18n.marktr("Game Duration Minutes Default"),
                I18n.marktr("The default duration a multiplayer/melee game is open (in minutes).")
        ),
        TEACHER_APPLICATIONS_ENABLED(
                I18n.marktr("Teacher Applications Enabled"),
                I18n.marktr(
                    """
                    Enable teacher account applications using email on the 'Contact Us' page.
                    Configuring a valid email address is required.
                    """
                )
        ),
        TEACHER_APPLICATIONS_EMAIL(
                I18n.marktr("Teacher Applications Email"),
                I18n.marktr("The email address to use for teacher account applications.")
        ),
        LOG_LEVEL(
                I18n.marktr("Log Level"),
                I18n.marktr("Controls the granularity of log messages.")
        );

        private final String readableName;
        private final String description;

        SETTING_NAME(String readableName, String description) {
            this.readableName = readableName;
            this.description = description;
        }

        public String getReadableName() {
            return readableName;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum SETTING_TYPE {
        STRING_VALUE,
        INT_VALUE,
        BOOL_VALUE
    }

    public static class SettingsDTO {

        public SettingsDTO(SETTING_NAME name, String value) {
            this.stringValue = value;
            this.type = SETTING_TYPE.STRING_VALUE;
            this.name = name;
        }

        public SettingsDTO(SETTING_NAME name, int value) {
            this.intValue = value;
            this.type = SETTING_TYPE.INT_VALUE;
            this.name = name;
        }

        public SettingsDTO(SETTING_NAME name, boolean value) {
            this.boolValue = value;
            this.type = SETTING_TYPE.BOOL_VALUE;
            this.name = name;
        }

        String stringValue;
        Integer intValue;
        Boolean boolValue;
        private SETTING_TYPE type;
        private SETTING_NAME name;

        public String getStringValue() {
            return stringValue;
        }

        public int getIntValue() {
            return intValue;
        }

        public boolean getBoolValue() {
            return boolValue;
        }

        public SETTING_NAME getName() {
            return name;
        }

        public SETTING_TYPE getType() {
            return type;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }

        public void setIntValue(int intValue) {
            this.intValue = intValue;
        }

        public void setBoolValue(boolean boolValue) {
            this.boolValue = boolValue;
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.getRequestDispatcher(Constants.ADMIN_SETTINGS_JSP).forward(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String responsePath = url.forPath(ADMIN_SETTINGS);

        switch (request.getParameter("formType")) {
            case "saveSettings":
                updateSystemSettings(request);
                break;
            default:
                logger.error("Action not recognised");
                break;
        }

        response.sendRedirect(responsePath);
    }

    // TODO Those methods should be factored into a class and exposed for reuse
    public void updateSystemSettings(HttpServletRequest request) {
        List<SettingsDTO> settings = AdminDAO.getSystemSettings();

        boolean success = true;
        for (SettingsDTO setting : settings) {
            String valueString = request.getParameter(setting.getName().name());
            if (setting.getType().equals(SETTING_TYPE.BOOL_VALUE)
                    || (valueString != null && (!valueString.isEmpty())
                    || setting.getType().equals(SETTING_TYPE.STRING_VALUE))) {
                switch (setting.getType()) {
                    case STRING_VALUE:
                        setting.setStringValue(valueString);
                        if (setting.getName().equals(SETTING_NAME.LOG_LEVEL)) {
                            try {
                                Level.valueOf(setting.getStringValue());
                            } catch (IllegalArgumentException e) {
                                logger.warn("Invalid log level: '{}'. Falling back to INFO.");
                                setting.setStringValue(Level.INFO.name());
                            }
                            // connectionFactory.updateSize(Integer.parseInt(valueString));
                        }
                        break;
                    case INT_VALUE:
                        setting.setIntValue(Integer.parseInt(valueString));
                        break;
                    case BOOL_VALUE:
                        setting.setBoolValue(valueString != null);
                        break;
                    default:
                        // ignored
                }
                success = success && AdminDAO.updateSystemSetting(setting);
            }
        }
        messages.add(success
                ? I18n.marktr("Updated Settings.")
                : I18n.marktr("There was a problem. Please consult the logs")
        );

        loggingConfig.reconfigure();
        messages.add(success ? "Updated Settings." : "There was a problem. Please consult the logs");
    }

}
