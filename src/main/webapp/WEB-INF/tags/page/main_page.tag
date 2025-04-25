<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="pageInfo" type="org.codedefenders.beans.page.PageInfoBean"--%>

<%@ attribute name="title" required="true" type="java.lang.String"
              description="Title of the page" %>
<%@ attribute name="additionalImports" required="false" fragment="true"
              description="Additional CSS or JavaScript tags to include in the head." %>

<%--
    Adds the header and footer along with other common elements to the base page.
--%>

<p:base_page title="${title}">
    <jsp:attribute name="additionalImports">
        <jsp:invoke fragment="additionalImports"/>
    </jsp:attribute>

    <jsp:body>
        <t:navbar/>
        <jsp:include page="/jsp/game_components/progress_bar_common.jsp"/>
        <jsp:include page="/jsp/messages.jsp"/>
        <div id="content">
            <jsp:doBody/>
        </div>
        <t:footer/>
    </jsp:body>
</p:base_page>
