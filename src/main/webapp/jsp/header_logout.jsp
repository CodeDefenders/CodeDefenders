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
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<jsp:include page="/jsp/header_base.jsp"/>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>

<nav class="navbar navbar-expand-md navbar-cd" id="header">
    <div class="container-fluid">

        <a class="navbar-brand" href="${url.forPath("/")}">
            <img src="${url.forPath("/images/logo.png")}" alt="" class="d-inline-block"
            <%-- Negative margin to prevent the navbar from getting tall from the tall image. --%>
                 style="height: 2.5rem; margin: -2rem .25rem -1.7rem .2rem;" />
            Code Defenders
        </a>

        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#header-navbar-controls"
                aria-controls="header-navbar-controls" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse" id="header-navbar-controls">

            <ul class="navbar-nav me-auto">
                <li class="nav-item">
                    <a class="nav-link" href="#research">Research</a>
                </li>
            </ul>
            <c:if test="${!pageContext.request.requestURI.contains(\"login\")}">
                <ul class="navbar-nav ms-auto">
                    <li class="nav-item">
                        <a class="nav-link" href="${url.forPath("/login")}">Login</a>
                    </li>
                </ul>
            </c:if>

        </div>

    </div>
</nav>

<jsp:include page="/jsp/messages.jsp"/>

<div id="content"> <%-- closed in footer --%>
