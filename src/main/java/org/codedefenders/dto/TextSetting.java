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
package org.codedefenders.dto;

import org.xnap.commons.i18n.I18n;

public class TextSetting {
    private final SETTING_NAME name;
    private final String language;
    private String value;

    public TextSetting(SETTING_NAME name, String language, String value) {
        this.name = name;
        this.language = language;
        this.value = value;
    }

    public SETTING_NAME name() {
        return name;
    }

    public String language() {
        return language;
    }

    public String value() {
        return value;
    }

    public void value(String value) {
        this.value = value;
    }

    public enum SETTING_NAME {
        SITE_NOTICE(
                I18n.marktr("Site Notice"),
                I18n.marktr("HTML formatted text shown in the site notice. This is mandatory in many regions.")
        ),
        PRIVACY_NOTICE(
                I18n.marktr("Privacy Notice"),
                I18n.marktr("HTML formatted text shown in the privacy notice. This is mandatory for GDPR compliance.")
        ),
        CONTACT_NOTICE(
                I18n.marktr("Contact Notice"),
                I18n.marktr("HTML formatted text shown on the 'Contact Us' page just below heading.")
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
}
