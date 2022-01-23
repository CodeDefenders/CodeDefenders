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
<%@ page isErrorPage="true" import="java.io.*" contentType="text/html;" pageEncoding="UTF-8" %>

<%
    System.err.println(exception.getMessage());
    exception.printStackTrace();
%>

<!DOCTYPE html>
<html>

<head>
    <meta content="width=device-width, initial-scale=1, maximum-scale=1" name="viewport">
    <title>Internal Server Error (500)</title>
    <link rel="icon" href="favicon.ico" type="image/x-icon">
    <link href="${pageContext.request.contextPath}/css/specific/error_page.css" rel="stylesheet">
</head>

<body>
	<a href="<%=request.getContextPath()%>">
        <img src="<%=request.getContextPath()%>/images/logo.png">
	</a>
	<h1>500</h1>
    <div class="container">
        <h3>Internal Server Error</h3>
        <p>There has been a problem on our side.</p>
        <p>Sorry!</p>
        <p>Please try again and contact your administrator if this keeps happening.</p>
        <a href="javascript:history.back()" class="go-back">Go back</a>
    </div>
    <script>
        (function () {
            if (history.length > 1) {
                document.querySelector('.go-back').removeAttribute('hidden');
            }
        })();
    </script>
</body>

</html>

