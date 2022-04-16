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
<%@ page import="org.codedefenders.model.UserEntity" %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>
<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>

<%
    final UserEntity user = (UserEntity) request.getAttribute("user");
    final boolean isSelf = (boolean) request.getAttribute("self");

    pageInfo.setPageTitle(isSelf ? "My Profile" : "Profile of " + user.getUsername());
%>

<jsp:include page="/jsp/header.jsp"/>

<div class="container form-width">
    <h1>${pageInfo.pageTitle}</h1>

    <% if (isSelf) { %>
        <section class="mt-5" aria-labelledby="played-games">
            <h2 class="mb-3" id="played-games">Played games</h2>
            <p>
                You can find a list of your past games in the
                <a href="<%=request.getContextPath() + Paths.GAMES_HISTORY%>">games history</a>.
            </p>
        </section>

        <section class="mt-5" aria-labelledby="account-information">
            <h2 class="mb-3" id="account-information">Account Information</h2>
            <p>
                Your current email:
                <span class="d-inline-block px-2 ms-2 border"><%=user.getEmail()%></span>
            </p>
            <p>
                Change your account information, password or delete your account in the
                <a href="<%=request.getContextPath() + Paths.USER_SETTINGS%>"
                   title="Edit or delete your CodeDefenders account.">account settings</a>.
            </p>
        </section>
    <% } %>

</div>

<%@ include file="/jsp/footer.jsp" %>
