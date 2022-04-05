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
            <span class="d-inline-block px-2 ms-2 border"><%=login.getUser().getEmail()%></span>
        </p>
        <p>
            Change your account information, password or delete your account in the
            <a href="<%=request.getContextPath() + Paths.USER_SETTINGS%>"
               title="Edit or delete your CodeDefenders account.">account settings</a>.
        </p>
    </section>

</div>

<%@ include file="/jsp/footer.jsp" %>
