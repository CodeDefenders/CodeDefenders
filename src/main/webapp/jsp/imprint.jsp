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

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>
<%--@elvariable id="siteNotice" type="java.lang.String"--%>
<%--@elvariable id="privacyNotice" type="java.lang.String"--%>

<p:main_page title="${i18n.tr('Imprint & Privacy Policy')}">
    <div class="container">

        <h1 class="mb-3">${i18n.tr('Site Notice')}</h1>

        <div class="bg-light rounded-3 p-3 mb-3">
            <c:choose>
                <c:when test="${empty siteNotice}">
                    <p class="mb-0">${i18n.tr('Please add an imprint in the system settings.')}</p>
                </c:when>
                <c:otherwise>
                    <p style="white-space: pre-wrap" class="mb-0"><c:out value="${siteNotice}" escapeXml="false"/></p>
                </c:otherwise>
            </c:choose>
        </div>

        <h2 class="mt-4 mb-3 h1">${i18n.tr('Privacy Policy')}</h2>

        <div class="bg-light rounded-3 p-3 mb-3">
            <c:choose>
                <c:when test="${empty privacyNotice}">
                    <p class="mb-0">${i18n.tr('Please add a privacy policy in the system settings.')}</p>
                </c:when>
                <c:otherwise>
                    <p style="white-space: pre-wrap" class="mb-0"><c:out value="${privacyNotice}" escapeXml="false"/></p>
                </c:otherwise>
            </c:choose>
        </div>

    </div>
</p:main_page>
