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

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("My Profile"); %>

<jsp:include page="/jsp/header.jsp"/>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<div class="container form-width">
    <h1>${pageInfo.pageTitle}</h1>

    <h2 class="mt-4 mb-3">Played games</h2>
    <p>
        You can find a list of your past games in the
        <a href="<%=request.getContextPath() + Paths.GAMES_HISTORY%>">games history</a>.
    </p>

    <h2 class="mt-4 mb-3">Account Information</h2>

    <p>Your current email: <%=login.getUser().getEmail()%></p>

    <p>Change your account information, password or delete your account:</p>
    <a href="<%=request.getContextPath() + Paths.USER_SETTINGS%>" class="btn btn-outline-primary">Account Settings</a>

</div>

<%@ include file="/jsp/footer.jsp" %>
