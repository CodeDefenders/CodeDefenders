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
<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.*" %>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Contact Us"); %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<% if (login.isLoggedIn()) { %>
	<jsp:include page="/jsp/header.jsp"/>
<% } else { %>
	<jsp:include page="/jsp/header_logout.jsp"/>
<% } %>

<div class="container" style=" max-width: 50%; min-width: 25%;">

	<h2>Contact Us</h2>
	<p>
		Code Defenders is an open source project; you can find details on the
		<a href="https://github.com/CodeDefenders/CodeDefenders">GitHub</a> project page.
	</p>
<%
	if (AdminDAO.getSystemSetting(EMAILS_ENABLED).getBoolValue()) {
%>
	<form action="<%=request.getContextPath() + Paths.API_SEND_EMAIL%>" method="post" class="needs-validation">
		<input type="hidden" name="formType" value="login">

        <div class="row">
            <div class="col-sm-12 mb-3">
                <label class="form-label" for="name-input">Name</label>
                <input type="text" id="name-input" name="name" class="form-control" placeholder="Name" required autofocus>
            </div>
        </div>

        <div class="row">
            <div class="col-sm-12 mb-3">
                <label class="form-label" for="email-input">Email</label>
                <input type="email" id="email-input" name="email" class="form-control" placeholder="Email" required>
            </div>
        </div>

        <div class="row">
            <div class="col-sm-12 mb-3">
                <label class="form-label" for="subject-input">Subject</label>
                <input type="text" id="subject-input" name="subject" class="form-control" placeholder="Subject" required>
            </div>
        </div>

        <div class="row">
            <div class="col-sm-12 mb-3">
                <label class="form-label" for="message-input">Message</label>
                <textarea id="message-input" name="message" class="form-control" placeholder="Message" rows="8" required></textarea>
            </div>
        </div>

        <div class="row">
            <div class="col-sm-12 mb-3">
                <button class="btn btn-primary" type="submit">Send</button>
            </div>
        </div>
	</form>
<%
	}
%>

</div>

<%@ include file="/jsp/footer.jsp" %>
