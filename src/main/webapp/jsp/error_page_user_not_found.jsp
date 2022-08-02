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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html>

<head>
    <meta content="width=device-width, initial-scale=1, maximum-scale=1" name="viewport">
    <title>User not found (404)</title>
    <link rel="icon" href="favicon.ico" type="image/x-icon">
    <link href="${pageContext.request.contextPath}/css/specific/error_page.css" rel="stylesheet">
</head>

<body>
    <div class="content">
        <a href="${pageContext.request.contextPath}/" class="branding">
            <img src="${pageContext.request.contextPath}/images/logo.png"
                 alt="Code Defenders Logo"
                 width="58">
            <h1>Code Defenders</h1>
        </a>
        <h2>404</h2>
        <h3>The user with the name "${fn:escapeXml(pageContext.request.getAttribute("user"))}" does not exist.</h3>
        <hr/>
        <p>Make sure the address and the username are correct and that the page hasn't moved.</p>
        <p>Please contact your administrator if you think this is a mistake.</p>
        <div class="go-back" hidden>
            <a href="javascript:history.back()">Go back</a>
        </div>
        <script>
            if (history.length > 1) {
                document.querySelector('.go-back').removeAttribute('hidden');
            }
        </script>
    </div>
</body>

</html>
