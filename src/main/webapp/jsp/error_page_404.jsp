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
<!DOCTYPE html>
<html>

<head>
    <meta content="width=device-width, initial-scale=1, maximum-scale=1" name="viewport">
    <title>The page you're looking for could not be found (404)</title>
    <link rel="icon" href="favicon.ico" type="image/x-icon">
    <link href="${pageContext.request.contextPath}/css/specific/error_page.css" rel="stylesheet">
</head>

<body>
	<a href="<%=request.getContextPath()%>">
        <img src="<%=request.getContextPath()%>/images/logo.png">
	</a>
    <h1>404</h1>
    <div class="container">
        <h3>The page could not be found or you don't have permission to view it.</h3>
        <hr/>
        <p>
            The resource that you are attempting to access does not exist or you don't have the necessary permissions to
            view it.
        </p>
        <p>Make sure the address is correct and that the page hasn't moved.</p>
        <p>Please contact your administrator if you think this is a mistake.</p>
        <a href="javascript:history.back()" class="go-back" hidden>Go back</a>
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

