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
package org.codedefenders.persistence.database;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.servlets.admin.AdminSystemSettings;

@Named
@ApplicationScoped
public class SettingsRepository {

    public boolean isMailValidationRequired() {
        return AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.REQUIRE_MAIL_VALIDATION).getBoolValue();
    }

    public int getMinPasswordLength() {
        return AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue();
    }
}
