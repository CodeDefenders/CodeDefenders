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

import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.ConnectionPool;
import org.codedefenders.util.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AdminSystemSettings extends HttpServlet {

    public enum SETTING_NAME {
        SHOW_PLAYER_FEEDBACK {
            @Override
            public String toString() {
                return "Show other player's feedback to all players in the game.";
            }
        },
        REGISTRATION {
            @Override
            public String toString() {
                return "Show or hide the link for user registration.";
            }
        },
        CLASS_UPLOAD {
            @Override
            public String toString() {
                return "Show or hide the link for CUT upload.";
            }
        },
        GAME_CREATION {
            @Override
            public String toString() {
                return "Show or hide the link for Game Creation.";
            }
        },
        GAME_JOINING {
            @Override
            public String toString() {
                return "Show or hide links to join/leave games.";
            }
        },
        REQUIRE_MAIL_VALIDATION {
            @Override
            public String toString() {
                return "Require a validated email address for login.";
            }
        },
        MIN_PASSWORD_LENGTH {
            @Override
            public String toString() {
                return "Minimum password length, also length of generated passwords. Recommended: >7";
            }
        },
        CONNECTION_POOL_CONNECTIONS {
            @Override
            public String toString() {
                return "Number of permanently open connections. Recommended: >20 \nLowering this number closes the delta in connections!";
            }
        },
        CONNECTION_WAITING_TIME {
            @Override
            public String toString() {
                return "Amount of time in ms a thread waits to be notified of newly available connections. Recommended: ~5000ms";
            }
        },
        SITE_NOTICE {
            @Override
            public String toString() {
                return "HTML formatted text shown in the site notice. This is mandatory in many regions.";
            }
        },
        PRIVACY_NOTICE {
            @Override
            public String toString() {
                return "HTML formatted text shown in the provacy notice. This is mandatory for GDPR compliance.";
            }
        },
        EMAIL_SMTP_HOST {
            @Override
            public String toString() {
                return "SMTP host";
            }
        },
        EMAIL_SMTP_PORT {
            @Override
            public String toString() {
                return "SMTP port";
            }
        },
        EMAIL_ADDRESS {
            @Override
            public String toString() {
                return "System mail account";
            }
        },
        EMAIL_PASSWORD {
            @Override
            public String toString() {
                return "Password for the system mail account";
            }
        },
        EMAILS_ENABLED {
            @Override
            public String toString() {
                return "Send emails from the specified account for verification, resetting passwords and when the admin changes user info";
            }
        },
        PASSWORD_RESET_SECRET_LIFESPAN {
            @Override
            public String toString() {
                return "How long (in hours) a password reset secret is valid";
            }
        },
        DEBUG_MODE {
            @Override
            public String toString() {
                return "Turn on certain debugging features such as detailed debug prints for javax.mail";
            }
        },
        AUTOMATIC_KILLMAP_COMPUTATION {
            @Override
            public String toString() {
                return "Turn on the automatic killmaps computation";
            }
        },
        ALLOW_USER_PROFILE {
            @Override
            public String toString() {
                return "Let users view and edit their profile.";
            }
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
        HttpSession session = request.getSession();
        // Get their user id from the session.
        int currentUserID = (Integer) session.getAttribute("uid");
        ArrayList<String> messages = new ArrayList<String>();
        session.setAttribute("messages", messages);
        String responsePath = request.getContextPath() + "/admin/settings";

        switch (request.getParameter("formType")) {
            case "saveSettings":
                updateSystemSettings(request, messages);
                break;
            default:
                System.err.println("Action not recognised");
                break;
        }

        response.sendRedirect(responsePath);
    }

    // TODO Those methods should be factored into a class and exposed for reuse
    public void updateSystemSettings(HttpServletRequest request, List<String> messages) {
        List<SettingsDTO> settings = AdminDAO.getSystemSettings();

        boolean success = true;
        for (SettingsDTO setting : settings) {
            String valueString = request.getParameter(setting.getName().name());
            if (setting.getType().equals(SETTING_TYPE.BOOL_VALUE)
                    || (valueString != null && (!valueString.equals(""))
                    || setting.getType().equals(SETTING_TYPE.STRING_VALUE))) {
                switch (setting.getType()) {
                    case STRING_VALUE:
                        setting.setStringValue(valueString);
                        break;
                    case INT_VALUE:
                        setting.setIntValue(Integer.parseInt(valueString));
                        if (setting.getName().equals(SETTING_NAME.CONNECTION_POOL_CONNECTIONS))
                            ConnectionPool.instance().updateSize(Integer.parseInt(valueString));
                        if (setting.getName().equals(SETTING_NAME.CONNECTION_WAITING_TIME))
                            ConnectionPool.instance().updateWaitingTime(Integer.parseInt(valueString));
                        break;
                    case BOOL_VALUE:
                        setting.setBoolValue(valueString != null);
                        break;
                }
                success = success && AdminDAO.updateSystemSetting(setting);
            }
        }
        messages.add(success ? "Updated Settings." : "There was a problem. Please consult the logs");
    }

}
