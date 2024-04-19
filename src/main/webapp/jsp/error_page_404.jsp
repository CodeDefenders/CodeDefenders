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

<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:error_page
        title="The page you're looking for could not be found (404)"
        statusCode="404"
        shortDescription="The page could not be found, or you don't have permission to view it.">
    <jsp:attribute name="message">
        <p>
            The resource that you are attempting to access does not exist,
            or you don't have the necessary permissions to view it.
        </p>
        <p>Make sure the address is correct and that the page hasn't moved.</p>
        <p>Please contact your administrator if you think this is a mistake.</p>
    </jsp:attribute>
</t:error_page>
