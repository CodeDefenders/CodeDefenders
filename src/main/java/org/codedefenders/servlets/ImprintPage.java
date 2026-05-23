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
import jakarta.inject.Named;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.service.I18nService;
import org.codedefenders.service.TextSettingsService;

import static org.codedefenders.dto.TextSetting.SETTING_NAME.PRIVACY_NOTICE;
import static org.codedefenders.dto.TextSetting.SETTING_NAME.SITE_NOTICE;

@WebServlet("/imprint")
public class ImprintPage extends HttpServlet {

    @Inject
    private TextSettingsService textSettingsService;
    @Named
    @Inject
    private I18nService i18nService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        var language = i18nService.getI18n(req).getLocale().getLanguage();
        var siteNotice = textSettingsService.getTextSetting(language, SITE_NOTICE);
        var privacyNotice = textSettingsService.getTextSetting(language, PRIVACY_NOTICE);
        req.setAttribute("siteNotice", siteNotice.value());
        req.setAttribute("privacyNotice", privacyNotice.value());
        req.getRequestDispatcher("/jsp/imprint.jsp").forward(req, resp);
    }
}
