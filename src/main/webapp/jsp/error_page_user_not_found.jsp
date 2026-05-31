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
<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<%@ page import="org.codedefenders.util.Paths" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<t:error_page
        title="${i18n.tr('User not found (404)')}"
        statusCode="404"
        shortDescription="${i18n.tr('The user with the name \"{0}\" does not exist.', fn:escapeXml(param.user))}">
    <jsp:attribute name="message">
        <p>${i18n.tr("Make sure the address and the username are correct.")}</p>
        <p>${i18n.tr("Please contact your administrator if you think this is a mistake.")}</p>
        <p>
            ${i18n.tr("Looking for your own profile?")}
            <a href="${url.forPath(Paths.USER_PROFILE)}" title="${i18n.tr('your profile')}">${i18n.tr("Click here.")}</a>
        </p>
    </jsp:attribute>
</t:error_page>
