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
<%@ page import="org.codedefenders.util.Paths" %>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Login"); %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<%
    if (!login.isLoggedIn()) {
        response.sendRedirect(request.getContextPath() + Paths.GAMES_OVERVIEW);
    }

    String email = request.getParameter("email");
    String reference = request.getParameter("reference");
%>

<jsp:include page="/jsp/header_logout.jsp"/>

<div id="login" class="container">
    <div class="modal-body">
        <div id="create">
            <form action="<%=request.getContextPath()  + Paths.LOGIN%>" method="post">
                <input type="hidden" name="formType" value="resetPassword">
                <input type="hidden" name="reference"
                       value="<%= reference %>">
                <label for="inputEmail" class="sr-only">Email</label>
                <input type="email" id="inputEmail" name="email" class="form-control" placeholder="Email" required autofocus
                value="<%= email %>" />
                <label for="inputPasswordCreate" class="sr-only">Password
                </label>
                <input type="password" id="inputPasswordCreate" name="password" class="form-control" placeholder="Password" required>
                <label for="inputConfirmPassword"
                       class="sr-only">Confirm Password
                </label>
                <input type="password" id="inputConfirmPassword" name="confirm" class="form-control" placeholder="Confirm Password" required>
                <button class="btn btn-lg btn-primary btn-block" type="submit">Reset Password</button>
            </form>
            <span style="margin-right:5px; font-size:small;">Valid password: 3-20 characters.</span>
        </div>
    </div>
</div>
