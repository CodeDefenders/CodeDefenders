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

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.service.I18nService;
import org.codedefenders.service.TextSettingsService;
import org.codedefenders.util.Constants;
import org.codedefenders.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;

import static org.codedefenders.util.Paths.ADMIN_TEXT_SETTINGS;

@WebServlet(ADMIN_TEXT_SETTINGS)
public class AdminTextSettings extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(AdminTextSettings.class);

    @Inject
    private MessagesBean messages;

    @Inject
    private URLUtils url;

    @Inject
    private I18nService i18nService;

    @Inject
    private TextSettingsService textSettingsService;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        var language = request.getParameter("language");
        var locale = i18nService.toSupportedLocale(language);
        request.setAttribute("language", locale.getLanguage());

        var settings = textSettingsService.getAllTextSettings(locale.getLanguage());
        request.setAttribute("settings", settings);

        request.getRequestDispatcher(Constants.ADMIN_TEXT_SETTINGS_JSP).forward(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getParameter("formType").equals("saveTextSettings")) {
            updateTextSettings(request);
        } else {
            logger.error("Action not recognised");
        }

        response.sendRedirect(url.forPath(ADMIN_TEXT_SETTINGS));
    }

    private void updateTextSettings(HttpServletRequest request) {
        var language = request.getParameter("language");
        var locale = i18nService.toSupportedLocale(language);
        if (!locale.getLanguage().equals(language)) {
            messages.addFormatted(I18n.marktr("Unsupported language: {0}"), language);
            return;
        }

        var success = true;
        var settings = textSettingsService.getAllTextSettings(language);
        for (var setting : settings) {
            setting.value(request.getParameter(setting.name().name()));
            success &= textSettingsService.updateTextSetting(setting);
        }

        if (success) {
            messages.addFormatted(
                    I18n.marktr("Text settings for language {0} updated successfully."),
                    locale.getDisplayLanguage()
            );
        } else {
            messages.addFormatted(
                    I18n.marktr("Failed to update text settings for language {0}."),
                    locale.getDisplayLanguage()
            );
        }
    }
}
