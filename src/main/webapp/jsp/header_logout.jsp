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

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/jsp/header_base.jsp"/>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>

<nav class="navbar navbar-cd" id="header">
    <div class="navbar-header">

        <%-- The style attributes here are a workaround to make Logo + Text work in the navbar brand. --%>
        <a class="navbar-brand" href="${pageContext.request.contextPath}" style="position: relative;">
            <img src="images/logo.png" style="width: 2em; position: absolute; top: .1em; left: .4em;"/>
            <span style="margin-left: 2em;">Code Defenders</span>
        </a>

        <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#header-navbar-controls" aria-expanded="true">
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
        </button>

    </div>
    <div class="collapse navbar-collapse" id="header-navbar-controls">

        <ul class="nav navbar-nav">
            <c:if test="${!pageContext.request.requestURI.contains(\"login\")}">
                <li><a href="login">Login</a></li>
            </c:if>
            <li><a href="#research" onclick="openResearchBox()">Research</a></li>
            <li><a href="help">Help</a></li>
        </ul>

    </div>
</nav>

<div id="content"> <%-- closed in footer --%>

<jsp:include page="/jsp/messages.jsp"/>
