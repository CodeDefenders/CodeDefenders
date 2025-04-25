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
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:error_page
        title="You don't have permission to perform this request (403)"
        statusCode="403"
        shortDescription="You don't have permission to perform this request.">
    <jsp:attribute name="message">
        <p>Your accounts permissions do not include the accessed page or performed action.</p>
        <%-- TODO: Info about appyling for teacher roles here? --%>
        <p>Please contact your administrator if you think this is a mistake.</p>
    </jsp:attribute>
</t:error_page>
