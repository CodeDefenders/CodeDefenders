<%--

    Copyright (C) 2016-2025 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<%@ page import="org.codedefenders.database.AdminDAO" %>

<%
    String siteNotice = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.SITE_NOTICE).getStringValue();
    String privacyNotice = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.PRIVACY_NOTICE).getStringValue();
    pageContext.setAttribute("siteNotice", siteNotice);
    pageContext.setAttribute("privacyNotice", privacyNotice);
%>

<p:main_page title="Imprint & Privacy Policy">
    <div class="container">

        <h3 class="mb-3">Imprint</h3>

        <div class="bg-light rounded-3 p-3 mb-3">
            <c:choose>
                <c:when test="${empty siteNotice}">
                    <p class="mb-0">Please add an imprint in the system settings.</p>
                </c:when>
                <c:otherwise>
                    <p style="white-space: pre-wrap" class="mb-0"><c:out value="${siteNotice}" escapeXml="false"/></p>
                </c:otherwise>
            </c:choose>
        </div>

        <h3 class="mt-4 mb-3">Privacy Policy</h3>

        <div class="bg-light rounded-3 p-3 mb-3">
            <c:choose>
                <c:when test="${empty privacyNotice}">
                    <p class="mb-0">Please add a privacy policy in the system settings.</p>
                </c:when>
                <c:otherwise>
                    <p style="white-space: pre-wrap" class="mb-0"><c:out value="${privacyNotice}" escapeXml="false"/></p>
                </c:otherwise>
            </c:choose>
        </div>

    </div>
</p:main_page>
