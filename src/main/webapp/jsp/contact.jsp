<%--

    Copyright (C) 2016-2019 Code Defenders contributors

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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.*" %>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Contact Us"); %>

<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>

<% if (login.isLoggedIn()) { %>
	<jsp:include page="/jsp/header.jsp"/>
<% } else { %>
	<jsp:include page="/jsp/header_logout.jsp"/>
<% } %>

<%
    boolean emailsEnabled = AdminDAO.getSystemSetting(EMAILS_ENABLED).getBoolValue();
    String contactNotice = AdminDAO.getSystemSetting(CONTACT_NOTICE).getStringValue();
    pageContext.setAttribute("emailsEnabled", emailsEnabled);
    pageContext.setAttribute("contactNotice", contactNotice);
%>

<div class="container form-width">

	<h2>${pageInfo.pageTitle}</h2>
    <p>
        Code Defenders is an open source project; you can find details on the
        <a href="https://github.com/CodeDefenders/CodeDefenders">GitHub</a> project page.
    </p>

    <c:if test="${emailsEnabled}">
        <form action="${url.forPath(Paths.API_SEND_EMAIL)}" method="post" class="needs-validation">
            <input type="hidden" name="formType" value="login">

            <div class="row g-3">
                <div class="col-12">
                    <label class="form-label" for="name-input">Name</label>
                    <input type="text" id="name-input" name="name" class="form-control" placeholder="Name" required autofocus>
                </div>

                <div class="col-12">
                    <label class="form-label" for="email-input">Email</label>
                    <input type="email" id="email-input" name="email" class="form-control" placeholder="Email" required>
                </div>

                <div class="col-12">
                    <label class="form-label" for="subject-input">Subject</label>
                    <input type="text" id="subject-input" name="subject" class="form-control" placeholder="Subject" required>
                </div>

                <div class="col-12">
                    <label class="form-label" for="message-input">Message</label>
                    <textarea id="message-input" name="message" class="form-control" placeholder="Message" rows="8" required></textarea>
                </div>

                <div class="col-12">
                    <button class="btn btn-primary" type="submit">Send</button>
                </div>
            </div>
        </form>
    </c:if>

    <c:if test="${!contactNotice.blank}">
        <div class="mt-5">
            <c:out value="${contactNotice}" escapeXml="false"/>
        </div>
    </c:if>
</div>

<%@ include file="/jsp/footer.jsp" %>
