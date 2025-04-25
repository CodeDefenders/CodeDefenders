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
package org.codedefenders.beans.contact;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.util.LinkUtils;
import org.codedefenders.util.URLUtils;

@RequestScoped
@Named("teacherApplicationTemplate")
public class TeacherApplicationTemplate {
    private final CodeDefendersAuth login;
    private final URLUtils url;

    @Inject
    public TeacherApplicationTemplate(CodeDefendersAuth login, URLUtils url) {
        this.login = login;
        this.url = url;
    }

    public String getMailtoLink() {
        String urlEncodedLink = "mailto:%s?subject=%s&body=%s".formatted(
                LinkUtils.urlEncode(getAddress()),
                LinkUtils.urlEncode(getSubject()),
                LinkUtils.urlEncode(getBody())
        );
        return urlEncodedLink.replaceAll("\\+", "%20");
    }

    public String getAddress() {
        return AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.TEACHER_APPLICATIONS_EMAIL).getStringValue();
    }

    public String getSubject() {
        return "Teacher Account Application for " + login.getSimpleUser().getName();
    }

    public String getBody() {
        return """
                Greetings,

                I would like to request a teacher account for %s on %s.

                Regards,
                %s
                """.stripIndent().formatted(
                        login.getSimpleUser().getName(),
                        getShortAbsoluteUri(),
                        login.getSimpleUser().getName());
    }

    private String getShortAbsoluteUri() {
        return url.getAbsoluteURLForPath("/")
                .replaceFirst("^https?://", "")
                .replaceFirst("/$", "");
    }
}
