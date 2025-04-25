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
<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--
This is intended as a workaround until we get scriptlets out of the last pages.

Takes the title (title) and JSP page (jspPage) from the request, and includes it in the main layout.
This allows us to include pages with scriptlets without having to create a wrapper for each one.
--%>

<p:main_page title="${requestScope.title}">
    <jsp:include page="${requestScope.jspPage}"/>
</p:main_page>
