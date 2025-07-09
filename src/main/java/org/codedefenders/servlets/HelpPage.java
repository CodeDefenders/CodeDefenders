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

import org.codedefenders.beans.page.PageInfoBean;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import static org.xnap.commons.i18n.I18nFactory.FALLBACK;
import static org.xnap.commons.i18n.I18nFactory.READ_PROPERTIES;

@WebServlet("/help")
public class HelpPage extends HttpServlet {

    @Inject
    PageInfoBean pageInfo;
    final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HelpPage.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        I18n i18n = I18nFactory.getI18n(getClass(), req.getLocale(), FALLBACK | READ_PROPERTIES);
        pageInfo.setPageTitle(i18n.tr("Help"));
        logger.info(i18n.tr("Help"));
        req.setAttribute("i18n", i18n);
        req.getRequestDispatcher("/jsp/help.jsp").forward(req, resp);
    }
}
