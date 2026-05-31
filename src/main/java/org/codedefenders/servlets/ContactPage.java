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
package org.codedefenders.servlets;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.dto.TextSetting;
import org.codedefenders.service.I18nService;
import org.codedefenders.service.TextSettingsService;

import static org.codedefenders.dto.TextSetting.SETTING_NAME.CONTACT_NOTICE;
import static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED;
import static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.TEACHER_APPLICATIONS_EMAIL;
import static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.TEACHER_APPLICATIONS_ENABLED;

@WebServlet("/contact")
public class ContactPage extends HttpServlet {

    @Inject
    private I18nService i18nService;
    @Inject
    private TextSettingsService textSettingsService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        var language = i18nService.getI18n(req).getLocale().getLanguage();
        var contactNotice = textSettingsService.getTextSettingOrDefault(language, CONTACT_NOTICE);

        var emailsEnabled = AdminDAO.getSystemSetting(EMAILS_ENABLED).getBoolValue();
        var teacherApplicationsEnabled = AdminDAO.getSystemSetting(TEACHER_APPLICATIONS_ENABLED).getBoolValue();
        var teacherApplicationsEmail = AdminDAO.getSystemSetting(TEACHER_APPLICATIONS_EMAIL).getStringValue();

        req.setAttribute("contactNotice", contactNotice.map(TextSetting::value).orElse(null));
        req.setAttribute("emailsEnabled", emailsEnabled);
        req.setAttribute("teacherApplicationsEnabled", teacherApplicationsEnabled);
        req.setAttribute("teacherApplicationsEmail", teacherApplicationsEmail);

        req.getRequestDispatcher("/jsp/contact.jsp").forward(req, resp);
    }
}
