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

<div class="container" style=" max-width: 50%; min-width: 25%; ">
	<h2 style="text-align: center">Contact Us</h2>
	<p style="text-align: center">
		Code Defenders is an open source project; you can find details on the
		<a href="https://github.com/CodeDefenders/CodeDefenders">GitHub</a> project page.
	</p>
</div>
	<%
	final boolean emailEnabled = AdminDAO.getSystemSetting(EMAILS_ENABLED).getBoolValue();
	if(emailEnabled) {
%>
<div class="container">
	<form action="<%=request.getContextPath() + Paths.API_SEND_EMAIL%>" method="post" class="form-signin">
		<input type="hidden" name="formType" value="login">
		<label for="inputName" class="sr-only">Name</label>
		<input type="text" id="inputName" name="name" class="form-control" placeholder="Name" required autofocus>
		<label for="inputEmail" class="sr-only">Email</label>
		<input type="email" id="inputEmail" name="email" class="form-control" placeholder="Email" required>
		<label for="inputSubject" class="sr-only">Subject</label>
		<input type="text" id="inputSubject" name="subject" class="form-control" placeholder="Subject" required autofocus>
		<label for="inputMessage" class="sr-only">Message</label>
		<textarea id="inputMessage" name="message" class="form-control" placeholder="Message" rows="8" required
            style="resize: none;"></textarea>
		<button class="btn btn-lg btn-primary btn-block" type="submit">Send</button>
	</form>
<%
	}
%>
</div>

<%@ include file="/jsp/footer.jsp" %>
