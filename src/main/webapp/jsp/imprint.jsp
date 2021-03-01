<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<%@ page import="org.codedefenders.database.AdminDAO" %><%--

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
<% pageInfo.setPageTitle("Contact Us"); %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<% if (login.isLoggedIn()) { %>
<jsp:include page="/jsp/header.jsp"/>
<% } else { %>
<jsp:include page="/jsp/header_logout.jsp"/>
<% } %>

<div class="container" style=" max-width: 50%; min-width: 25%; ">

    <h2 style="text-align: left">Imprint</h2>

    <%  String siteNotice = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.SITE_NOTICE).getStringValue();
        if (siteNotice.isEmpty()) {
    %>
            <p>Please add an imprint in the system settings</p>
    <%  } else { %>
            <%=siteNotice%>
    <%  } %>

    <br/>

    <h3 style="text-align: left">Privacy Policy</h3>

    <%  String privacyNotice = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.PRIVACY_NOTICE).getStringValue();
        if (privacyNotice.isEmpty()) {
    %>
            <p>Please add a privacy policy in the system settings</p>
    <%  } else { %>
            <%=privacyNotice%>
    <%  } %>
    
</div>

<%@ include file="/jsp/footer.jsp" %>
