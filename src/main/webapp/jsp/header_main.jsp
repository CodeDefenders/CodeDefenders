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

<jsp:include page="/jsp/header.jsp"/>

<div class="nest">
    <div class="full-width">
        <div class="bg-plus-2" style="padding:2px 0;">
        </div>
        <% if (pageInfo.hasPageTitle()) { %>
            <h2 class="full-width page-title" style="text-align: center;">${pageInfo.pageTitle}</h2>
        <% } %>
        <div>
            <div>
                <div>
